package sn.examen_messagerie;

import sn.examen_messagerie.entity.ChatMessage;
import sn.examen_messagerie.entity.MessageStatus;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe client pour communiquer avec le serveur de messagerie
 */
public class ChatClient {
    
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());
    private static final int DEFAULT_TIMEOUT = 5000; // 5 secondes
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;
    
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private Thread messageListenerThread;

    public ChatClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Connecte le client au serveur
     * @return true si la connexion est réussie, false sinon
     */
    public boolean connect() {
        if (isConnected.get()) {
            logger.warning("Le client est déjà connecté");
            return true;
        }

        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(DEFAULT_TIMEOUT);
            
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            isConnected.set(true);
            logger.info("Connecté au serveur " + host + ":" + port);
            
            // Démarrer le thread d'écoute des messages
            startMessageListener();
            
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erreur lors de la connexion au serveur", e);
            cleanup();
            return false;
        }
    }

    /**
     * Déconnecte le client du serveur
     */
    public void disconnect() {
        if (!isConnected.get()) {
            return;
        }

        try {
            if (out != null) {
                out.writeObject("EXIT");
                out.flush();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Erreur lors de l'envoi de la commande EXIT", e);
        }

        cleanup();
        logger.info("Déconnecté du serveur");
    }

    /**
     * Inscrit un nouvel utilisateur
     * @param username Le nom d'utilisateur
     * @param password Le mot de passe
     * @return true si l'inscription est réussie, false sinon
     */
    public boolean register(String username, String password) {
        if (!isConnected.get()) {
            logger.warning("Tentative d'inscription sans être connecté");
            return false;
        }

        if (username == null || password == null || username.trim().isEmpty()) {
            logger.warning("Nom d'utilisateur ou mot de passe invalide");
            return false;
        }

        try {
            out.writeObject("REGISTER");
            out.writeObject(username.trim());
            out.writeObject(password);
            out.flush();

            boolean success = (Boolean) in.readObject();
            String message = (String) in.readObject();
            
            if (success) {
                logger.info("Inscription réussie: " + username);
            } else {
                logger.warning("Échec de l'inscription: " + message);
            }
            
            return success;
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Erreur lors de l'inscription", e);
            return false;
        }
    }

    /**
     * Connecte un utilisateur
     * @param username Le nom d'utilisateur
     * @param password Le mot de passe
     * @return true si la connexion est réussie, false sinon
     */
    public boolean login(String username, String password) {
        if (!isConnected.get()) {
            logger.warning("Tentative de connexion sans être connecté au serveur");
            return false;
        }

        if (username == null || password == null || username.trim().isEmpty()) {
            logger.warning("Nom d'utilisateur ou mot de passe invalide");
            return false;
        }

        try {
            out.writeObject("LOGIN");
            out.writeObject(username.trim());
            out.writeObject(password);
            out.flush();

            boolean success = (Boolean) in.readObject();
            String message = (String) in.readObject();
            
            if (success) {
                logger.info("Connexion réussie: " + username);
            } else {
                logger.warning("Échec de la connexion: " + message);
            }
            
            return success;
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Erreur lors de la connexion", e);
            return false;
        }
    }

    /**
     * Déconnecte l'utilisateur
     * @return true si la déconnexion est réussie, false sinon
     */
    public boolean logout() {
        if (!isConnected.get()) {
            return false;
        }

        try {
            out.writeObject("LOGOUT");
            out.flush();

            boolean success = (Boolean) in.readObject();
            String message = (String) in.readObject();
            
            if (success) {
                logger.info("Déconnexion réussie: " + message);
            }
            
            return success;
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Erreur lors de la déconnexion", e);
            return false;
        }
    }

    /**
     * Envoie un message
     * @param message Le message à envoyer
     * @return true si l'envoi est réussi, false sinon
     */
    public boolean sendMessage(ChatMessage message) {
        if (!isConnected.get()) {
            logger.warning("Tentative d'envoi de message sans être connecté");
            return false;
        }

        if (message == null) {
            logger.warning("Tentative d'envoi d'un message null");
            return false;
        }

        try {
            out.writeObject("SEND_MESSAGE");
            out.writeObject(message);
            out.flush();

            boolean success = (Boolean) in.readObject();
            String responseMessage = (String) in.readObject();
            
            if (success) {
                logger.info("Message envoyé avec succès: " + responseMessage);
            } else {
                logger.warning("Échec de l'envoi du message: " + responseMessage);
            }
            
            return success;
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Erreur lors de l'envoi du message", e);
            return false;
        }
    }

    /**
     * Marque un message comme lu
     * @param message Le message à marquer comme lu
     * @return true si le marquage est réussi, false sinon
     */
    public boolean markAsRead(ChatMessage message) {
        if (!isConnected.get()) {
            logger.warning("Tentative de marquage sans être connecté");
            return false;
        }

        if (message == null) {
            logger.warning("Tentative de marquage d'un message null");
            return false;
        }

        try {
            out.writeObject("MARK_AS_READ");
            out.writeObject(message);
            out.flush();

            boolean success = (Boolean) in.readObject();
            String responseMessage = (String) in.readObject();
            
            if (success) {
                logger.info("Message marqué comme lu: " + responseMessage);
            } else {
                logger.warning("Échec du marquage: " + responseMessage);
            }
            
            return success;
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Erreur lors du marquage du message", e);
            return false;
        }
    }

    /**
     * Démarre le thread d'écoute des messages entrants
     */
    private void startMessageListener() {
        messageListenerThread = new Thread(() -> {
            while (isConnected.get() && !socket.isClosed()) {
                try {
                    String action = (String) in.readObject();
                    
                    if (action == null) {
                        break;
                    }

                    switch (action) {
                        case "MESSAGE_RECEIVED":
                            ChatMessage receivedMessage = (ChatMessage) in.readObject();
                            onMessageReceived(receivedMessage);
                            break;
                        default:
                            logger.warning("Action inconnue reçue: " + action);
                            break;
                    }
                } catch (SocketTimeoutException e) {
                    // Timeout normal, continue la boucle
                    continue;
                } catch (EOFException e) {
                    logger.info("Connexion fermée par le serveur");
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    if (isConnected.get()) {
                        logger.log(Level.WARNING, "Erreur lors de la réception d'un message", e);
                    }
                    break;
                }
            }
        });
        
        messageListenerThread.setDaemon(true);
        messageListenerThread.start();
    }

    /**
     * Callback appelé lorsqu'un message est reçu
     * @param message Le message reçu
     */
    protected void onMessageReceived(ChatMessage message) {
        System.out.println("Message reçu de " + message.getSender() + ": " + message.getContent());
        System.out.println("Statut: " + message.getStatus());
    }

    /**
     * Nettoie les ressources
     */
    private void cleanup() {
        isConnected.set(false);
        
        try {
            if (messageListenerThread != null && messageListenerThread.isAlive()) {
                messageListenerThread.interrupt();
            }
            
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Erreur lors du nettoyage des ressources", e);
        }
    }

    public boolean isConnected() {
        return isConnected.get() && socket != null && !socket.isClosed();
    }

    // Classe de test
    public static class TestClient {
        public static void main(String[] args) {
            ChatClient client = new ChatClient();
            
            try {
                // Connexion
                if (!client.connect()) {
                    System.err.println("Échec de la connexion");
                    return;
                }

                // Test d'inscription
                if (client.register("testuser", "password123")) {
                    System.out.println("Inscription réussie");
                }

                // Test de connexion
                if (client.login("testuser", "password123")) {
                    System.out.println("Connexion réussie");
                }

                // Crée et envoie un message de test
                ChatMessage msg = new ChatMessage(
                        "MESSAGE",
                        "testuser",
                        "autreuser",
                        "Salut, ça marche ?",
                        MessageStatus.ENVOYE
                );

                if (client.sendMessage(msg)) {
                    System.out.println("Message envoyé avec succès");
                }

                // Attendre un peu pour recevoir des messages
                Thread.sleep(2000);

                // Déconnexion
                client.logout();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                client.disconnect();
            }
        }
    }
}