package sn.examen_messagerie.server;

import sn.examen_messagerie.controller.ChatController;
import sn.examen_messagerie.entity.ChatMessage;
import sn.examen_messagerie.service.UserService;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());
    
    private final Socket socket;
    private final UserService userService;
    private final ChatController chatController;
    private String currentUser;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean isRunning = true;

    public ClientHandler(Socket socket, UserService userService) {
        this.socket = socket;
        this.userService = userService;
        this.chatController = new ChatController();
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (isRunning && !socket.isClosed()) {
                try {
                    String action = (String) in.readObject();
                    
                    if (action == null) {
                        break;
                    }

                    switch (action.toUpperCase()) {
                        case "REGISTER":
                            handleRegister();
                            break;
                        case "LOGIN":
                            handleLogin();
                            break;
                        case "LOGOUT":
                            handleLogout();
                            break;
                        case "SEND_MESSAGE":
                            handleSendMessage();
                            break;
                        case "MARK_AS_READ":
                            handleMarkAsRead();
                            break;
                        case "EXIT":
                            isRunning = false;
                            break;
                        default:
                            sendResponse(false, "Action inconnue: " + action);
                            break;
                    }
                } catch (EOFException e) {
                    logger.info("Client a fermé la connexion");
                    break;
                } catch (ClassNotFoundException e) {
                    logger.log(Level.WARNING, "Erreur de désérialisation", e);
                    sendResponse(false, "Erreur de format de données");
                } catch (IOException e) {
                    if (isRunning) {
                        logger.log(Level.WARNING, "Erreur de communication avec le client", e);
                    }
                    break;
                }
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erreur lors de l'initialisation des flux", e);
        } finally {
            cleanup();
        }
    }

    private void handleRegister() {
        try {
            String username = (String) in.readObject();
            String password = (String) in.readObject();
            
            if (username == null || password == null || username.trim().isEmpty()) {
                sendResponse(false, "Nom d'utilisateur ou mot de passe invalide");
                return;
            }
            
            boolean success = userService.register(username.trim(), password);
            sendResponse(success, success ? "Inscription réussie" : "Nom d'utilisateur déjà utilisé");
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.WARNING, "Erreur lors de l'inscription", e);
            sendResponse(false, "Erreur lors de l'inscription");
        }
    }

    private void handleLogin() {
        try {
            String username = (String) in.readObject();
            String password = (String) in.readObject();
            
            if (username == null || password == null || username.trim().isEmpty()) {
                sendResponse(false, "Nom d'utilisateur ou mot de passe invalide");
                return;
            }
            
            boolean success = userService.login(username.trim(), password);
            
            if (success) {
                currentUser = username.trim();
                Server.addClient(currentUser, this);
                logger.info("Utilisateur connecté: " + currentUser);
            }
            
            sendResponse(success, success ? "Connexion réussie" : "Identifiants incorrects");
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.WARNING, "Erreur lors de la connexion", e);
            sendResponse(false, "Erreur lors de la connexion");
        }
    }

    private void handleLogout() {
        if (currentUser != null) {
            userService.logout(currentUser);
            Server.removeClient(currentUser);
            logger.info("Utilisateur déconnecté: " + currentUser);
            currentUser = null;
        }
        sendResponse(true, "Déconnexion réussie");
        isRunning = false;
    }

    private void handleSendMessage() {
        try {
            ChatMessage message = (ChatMessage) in.readObject();
            
            if (message == null || currentUser == null) {
                sendResponse(false, "Non autorisé ou message invalide");
                return;
            }
            
            // Vérifier que l'expéditeur est bien l'utilisateur connecté
            if (!currentUser.equals(message.getSender())) {
                sendResponse(false, "Vous ne pouvez pas envoyer un message en tant qu'un autre utilisateur");
                return;
            }
            
            chatController.sendMessage(message);
            sendResponse(true, "Message envoyé");
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.WARNING, "Erreur lors de l'envoi du message", e);
            sendResponse(false, "Erreur lors de l'envoi du message");
        }
    }

    private void handleMarkAsRead() {
        try {
            ChatMessage message = (ChatMessage) in.readObject();
            
            if (message == null || currentUser == null) {
                sendResponse(false, "Non autorisé ou message invalide");
                return;
            }
            
            // Vérifier que le destinataire est bien l'utilisateur connecté
            if (!currentUser.equals(message.getReceiver())) {
                sendResponse(false, "Vous ne pouvez marquer comme lu que vos propres messages reçus");
                return;
            }
            
            chatController.markAsRead(message);
            sendResponse(true, "Message marqué comme lu");
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.WARNING, "Erreur lors du marquage du message", e);
            sendResponse(false, "Erreur lors du marquage du message");
        }
    }

    private void sendResponse(boolean success, String message) {
        try {
            if (out != null && !socket.isClosed()) {
                out.writeObject(success);
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Erreur lors de l'envoi de la réponse", e);
        }
    }

    public void sendMessage(ChatMessage message) {
        try {
            if (out != null && !socket.isClosed() && isRunning) {
                out.writeObject("MESSAGE_RECEIVED");
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Erreur lors de l'envoi du message à " + 
                (message != null ? message.getReceiver() : "destinataire inconnu"), e);
        }
    }

    private void cleanup() {
        isRunning = false;
        
        if (currentUser != null) {
            Server.removeClient(currentUser);
            try {
                userService.logout(currentUser);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Erreur lors de la déconnexion de l'utilisateur", e);
            }
        }
        
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Erreur lors de la fermeture des ressources", e);
        }
        
        logger.info("Client déconnecté: " + (currentUser != null ? currentUser : "inconnu"));
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public boolean isConnected() {
        return isRunning && socket != null && !socket.isClosed();
    }
}