package sn.examen_messagerie.server;

import sn.examen_messagerie.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 5000;
    //  Liste globale des clients connectés
    public static Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    // Pool de threads (nombre fixe pour éviter surcharge mémoire)
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    private final UserService userService = new UserService();

    public void start() {
        System.out.println("Serveur démarré sur le port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (!threadPool.isShutdown()) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouveau client connecté : " + clientSocket.getInetAddress());

                // Au lieu de new Thread(), on utilise le pool
                threadPool.execute(new ClientHandler(clientSocket, userService));
            }

        } catch (IOException e) {
            System.err.println("Erreur serveur : " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        System.out.println("Arrêt du serveur...");
        threadPool.shutdown();
    }

    public static void main(String[] args) {
        new Server().start();
    }
}