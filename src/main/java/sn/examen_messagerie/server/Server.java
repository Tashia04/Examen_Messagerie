package sn.examen_messagerie.server;

import sn.examen_messagerie.entity.ChatMessage;
import sn.examen_messagerie.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Serveur principal qui accepte les connexions des clients
public class Server {

    private static final int PORT = 9000;

    // Liste globale des clients connectés (clé = username, valeur = handler)
    public static Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    // Pool de threads pour gérer plusieurs clients en parallèle (RG11)
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    private final UserService userService = new UserService();

    // Démarre le serveur et attend les connexions
    public void start() {
        System.out.println("[SERVEUR] Démarré sur le port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (!threadPool.isShutdown()) {
                // Attendre une nouvelle connexion client
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVEUR] Nouveau client : " + clientSocket.getInetAddress());

                // Lancer un thread pour ce client
                threadPool.execute(new ClientHandler(clientSocket, userService));
            }

        } catch (IOException e) {
            System.err.println("[SERVEUR] Erreur : " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    // Envoyer la liste des utilisateurs connectés à tous les clients
    public static void broadcastUserList() {
        // Construire la liste des usernames séparés par des virgules
        String users = String.join(",", connectedClients.keySet());

        ChatMessage userListMsg = new ChatMessage();
        userListMsg.setAction("user_list");
        userListMsg.setContenu(users);

        // Envoyer à chaque client connecté
        for (ClientHandler handler : connectedClients.values()) {
            handler.sendMessage(userListMsg);
        }
    }

    // Arrêter proprement le serveur
    private void shutdown() {
        System.out.println("[SERVEUR] Arrêt...");
        threadPool.shutdown();
    }

    // Point d'entrée du serveur
    public static void main(String[] args) {
        new Server().start();
    }
}
