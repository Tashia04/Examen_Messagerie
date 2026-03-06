package sn.examen_messagerie.server;

import sn.examen_messagerie.entity.ChatMessage;
import sn.examen_messagerie.entity.Message;
import sn.examen_messagerie.entity.MessageStatus;
import sn.examen_messagerie.entity.User;
import sn.examen_messagerie.repository.impl.MessageRepository;
import sn.examen_messagerie.service.UserService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

// Handles communication with a connected client (one thread per client, RG11)
public class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final UserService userService;
    private final MessageRepository messageRepository;
    private String currentUser;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ClientHandler(Socket socket, UserService userService) {
        this.socket = socket;
        this.userService = userService;
        this.messageRepository = new MessageRepository();
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                ChatMessage request = (ChatMessage) in.readObject();
                String action = request.getAction();

                switch (action) {
                    case "register":
                        handleRegister(request);
                        break;
                    case "login":
                        handleLogin(request);
                        break;
                    case "logout":
                        handleLogout();
                        return;
                    case "send_message":
                        handleSendMessage(request);
                        break;
                    case "get_users":
                        handleGetUsers();
                        break;
                    case "get_history":
                        handleGetHistory(request);
                        break;
                    default:
                        sendResponse("error", "Unknown action: " + action);
                        break;
                }
            }

        } catch (Exception e) {
            LOGGER.warning("Connection lost with " +
                    (currentUser != null ? currentUser : "unknown client"));
        } finally {
            disconnect();
        }
    }

    // ===================== REGISTRATION (RG1, RG9) =====================
    private void handleRegister(ChatMessage request) {
        String username = request.getSender();
        String password = request.getContenu();

        boolean success = userService.register(username, password);

        if (success) {
            LOGGER.info("Registration successful: " + username);
            sendResponse("register_response", "OK");
        } else {
            LOGGER.info("Registration failed: " + username + " (already exists)");
            sendResponse("register_response", "Ce nom d'utilisateur existe déjà");
        }
    }

    // ===================== LOGIN (RG3, RG4, RG6) =====================
    private void handleLogin(ChatMessage request) {
        String username = request.getSender();
        String password = request.getContenu();

        // RG3: check if user is already connected
        if (Server.connectedClients.containsKey(username)) {
            sendResponse("login_response", "Cet utilisateur est déjà connecté");
            return;
        }

        boolean success = userService.login(username, password);

        if (success) {
            currentUser = username;
            Server.connectedClients.put(username, this);
            LOGGER.info("Login: " + username);

            sendResponse("login_response", "OK");

            // RG6: deliver pending offline messages
            deliverPendingMessages();

            Server.broadcastUserList();
        } else {
            LOGGER.info("Login failed: " + username);
            sendResponse("login_response", "Identifiants incorrects");
        }
    }

    // ===================== LOGOUT (RG4) =====================
    private void handleLogout() {
        if (currentUser != null) {
            LOGGER.info("Logout: " + currentUser);
            userService.logout(currentUser);
        }
    }

    // ===================== SEND MESSAGE (RG2, RG5, RG7) =====================
    private void handleSendMessage(ChatMessage request) {
        // RG2: sender must be authenticated
        if (currentUser == null) {
            sendResponse("error", "Vous devez être connecté pour envoyer un message");
            return;
        }

        String receiver = request.getReceiver();
        String content = request.getContenu();

        // RG5: receiver must exist
        User receiverUser = userService.findByUsername(receiver);
        if (receiverUser == null) {
            sendResponse("error", "L'utilisateur " + receiver + " n'existe pas");
            return;
        }

        // RG7: content validation
        if (content == null || content.trim().isEmpty()) {
            sendResponse("error", "Le message ne peut pas être vide");
            return;
        }
        if (content.length() > 1000) {
            sendResponse("error", "Le message ne doit pas dépasser 1000 caractères");
            return;
        }

        User senderUser = userService.findByUsername(currentUser);
        LocalDateTime now = LocalDateTime.now();

        // Check if receiver is online
        ClientHandler receiverHandler = Server.connectedClients.get(receiver);
        MessageStatus status = (receiverHandler != null) ? MessageStatus.RECU : MessageStatus.ENVOYE;

        // Save to database with proper User relations
        Message message = new Message(senderUser, receiverUser, content, now, status);
        messageRepository.save(message);

        // Forward to receiver if online
        if (receiverHandler != null) {
            ChatMessage delivery = new ChatMessage();
            delivery.setAction("receive_message");
            delivery.setSender(currentUser);
            delivery.setReceiver(receiver);
            delivery.setContenu(content);
            delivery.setDateEnvoi(now);
            delivery.setStatut(MessageStatus.RECU);
            receiverHandler.sendMessage(delivery);
        }

        LOGGER.info("Message from " + currentUser + " to " + receiver);
        sendResponse("message_sent", "OK");
    }

    // ===================== ONLINE USERS LIST =====================
    private void handleGetUsers() {
        StringBuilder users = new StringBuilder();
        for (String username : Server.connectedClients.keySet()) {
            if (!username.equals(currentUser)) {
                if (users.length() > 0) {
                    users.append(",");
                }
                users.append(username);
            }
        }
        sendResponse("user_list", users.toString());
    }

    // ===================== MESSAGE HISTORY (RG2, RG8) =====================
    private void handleGetHistory(ChatMessage request) {
        // RG2: must be authenticated
        if (currentUser == null) {
            sendResponse("error", "Vous devez être connecté pour consulter les messages");
            return;
        }

        String otherUser = request.getReceiver();
        List<Message> history = messageRepository.findBetweenUsers(currentUser, otherUser);

        for (Message msg : history) {
            ChatMessage historyMsg = new ChatMessage();
            historyMsg.setAction("history");
            historyMsg.setSender(msg.getSender().getUsername());
            historyMsg.setReceiver(msg.getReceiver().getUsername());
            historyMsg.setContenu(msg.getContenu());
            historyMsg.setDateEnvoi(msg.getDateEnvoi());
            historyMsg.setStatut(msg.getStatut());
            sendMessage(historyMsg);
        }

        sendResponse("history_end", "OK");
    }

    // ===================== DELIVER PENDING MESSAGES (RG6) =====================
    private void deliverPendingMessages() {
        List<Message> pendingMessages = messageRepository.findPendingForUser(currentUser);

        for (Message msg : pendingMessages) {
            ChatMessage delivery = new ChatMessage();
            delivery.setAction("receive_message");
            delivery.setSender(msg.getSender().getUsername());
            delivery.setReceiver(msg.getReceiver().getUsername());
            delivery.setContenu(msg.getContenu());
            delivery.setDateEnvoi(msg.getDateEnvoi());
            delivery.setStatut(MessageStatus.RECU);
            sendMessage(delivery);

            messageRepository.updateStatus(msg.getId(), MessageStatus.RECU);
        }
    }

    // ===================== UTILITY METHODS =====================

    public void sendMessage(ChatMessage message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (Exception e) {
            LOGGER.warning("Send error to " +
                    (currentUser != null ? currentUser : "unknown"));
        }
    }

    private void sendResponse(String action, String content) {
        ChatMessage response = new ChatMessage();
        response.setAction(action);
        response.setContenu(content);
        sendMessage(response);
    }

    private void disconnect() {
        if (currentUser != null) {
            Server.connectedClients.remove(currentUser);
            userService.logout(currentUser);
            LOGGER.info(currentUser + " disconnected");
            Server.broadcastUserList();
        }

        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }
}
