package sn.examen_messagerie.client;

import sn.examen_messagerie.entity.ChatMessage;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

// Client socket qui communique avec le serveur
// Utilise le pattern Singleton (une seule instance par application)
public class Client {

    private static Client instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String currentUser;       // nom de l'utilisateur connecté
    private boolean connected = false;

    // Callback appelé quand un message est reçu du serveur
    private Consumer<ChatMessage> onMessageReceived;

    // Constructeur privé (Singleton)
    private Client() {
    }

    // Récupérer l'instance unique du client
    public static Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    // Se connecter au serveur
    public void connect(String host, int port) throws Exception {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        connected = true;
        System.out.println("[CLIENT] Connecté au serveur " + host + ":" + port);
    }

    // Envoyer un message au serveur (sans attendre de réponse)
    public void send(ChatMessage message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (Exception e) {
            System.err.println("[CLIENT] Erreur d'envoi : " + e.getMessage());
            connected = false;
        }
    }

    // Envoyer un message et attendre la réponse (mode synchrone)
    // Utilisé pour login et register (avant de lancer le thread d'écoute)
    public ChatMessage sendAndReceive(ChatMessage message) {
        try {
            out.writeObject(message);
            out.flush();
            return (ChatMessage) in.readObject();
        } catch (Exception e) {
            System.err.println("[CLIENT] Erreur envoi/réception : " + e.getMessage());
            connected = false;
            return null;
        }
    }

    // Démarrer le thread d'écoute (après le login)
    // Ce thread reçoit en continu les messages du serveur
    public void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (connected) {
                    ChatMessage msg = (ChatMessage) in.readObject();

                    // Appeler le callback sur le thread JavaFX
                    if (onMessageReceived != null) {
                        javafx.application.Platform.runLater(() -> onMessageReceived.accept(msg));
                    }
                }
            } catch (Exception e) {
                if (connected) {
                    System.err.println("[CLIENT] Connexion perdue avec le serveur");
                    connected = false;

                    // Notifier l'UI de la perte de connexion (RG10)
                    if (onMessageReceived != null) {
                        ChatMessage errorMsg = new ChatMessage();
                        errorMsg.setAction("connection_lost");
                        errorMsg.setContenu("Connexion au serveur perdue");
                        javafx.application.Platform.runLater(() -> onMessageReceived.accept(errorMsg));
                    }
                }
            }
        });
        listenerThread.setDaemon(true); // le thread s'arrête quand l'application se ferme
        listenerThread.start();
    }

    // Se déconnecter du serveur
    public void disconnect() {
        try {
            if (connected) {
                // Envoyer la demande de déconnexion
                ChatMessage logout = new ChatMessage();
                logout.setAction("logout");
                send(logout);

                connected = false;
                socket.close();
                currentUser = null;
                instance = null;
                System.out.println("[CLIENT] Déconnecté");
            }
        } catch (Exception e) {
            System.err.println("[CLIENT] Erreur déconnexion : " + e.getMessage());
        }
    }

    // Définir le callback pour les messages reçus
    public void setOnMessageReceived(Consumer<ChatMessage> callback) {
        this.onMessageReceived = callback;
    }

    // Getters et setters
    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isConnected() {
        return connected;
    }
}
