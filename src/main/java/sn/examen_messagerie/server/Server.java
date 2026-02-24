package sn.examen_messagerie.server;

import sn.examen_messagerie.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static final int PORT = 5000;
    private static final int THREAD_POOL_SIZE = 10;
    
    // Liste des clients connectés (encapsulée)
    private static final Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();

    // Pool de threads (nombre fixe pour éviter surcharge mémoire)
    private final ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final UserService userService = new UserService();
    private volatile boolean isRunning = false;
    private ServerSocket serverSocket;

    public void start() {
        if (isRunning) {
            logger.warning("Le serveur est déjà en cours d'exécution");
            return;
        }
        
        isRunning = true;
        logger.info("Démarrage du serveur sur le port " + PORT);

        try {
            serverSocket = new ServerSocket(PORT);
            logger.info("Serveur démarré avec succès sur le port " + PORT);

            while (isRunning && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Nouveau client connecté : " + clientSocket.getInetAddress());
                    
                    // Utilisation du pool de threads pour gérer les clients
                    threadPool.execute(new ClientHandler(clientSocket, userService));
                } catch (IOException e) {
                    if (isRunning) {
                        logger.log(Level.SEVERE, "Erreur lors de l'acceptation d'un client", e);
                    }
                }
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erreur lors du démarrage du serveur", e);
        } finally {
            shutdown();
        }
    }

    public void stop() {
        logger.info("Arrêt du serveur demandé...");
        isRunning = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Erreur lors de la fermeture du ServerSocket", e);
        }
        
        shutdown();
    }

    private void shutdown() {
        logger.info("Arrêt du serveur...");
        
        // Déconnexion de tous les clients
        synchronized (connectedClients) {
            connectedClients.values().forEach(client -> {
                try {
                    if (client.isConnected()) {
                        // Les clients se déconnecteront proprement via cleanup()
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Erreur lors de la déconnexion d'un client", e);
                }
            });
            connectedClients.clear();
        }
        
        // Arrêt du pool de threads
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning("Le pool de threads n'a pas pu être arrêté proprement");
                }
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("Serveur arrêté");
    }

    // Méthodes statiques pour gérer les clients connectés (encapsulation)
    public static void addClient(String username, ClientHandler handler) {
        if (username != null && handler != null) {
            connectedClients.put(username, handler);
            logger.info("Client ajouté: " + username + " (Total: " + connectedClients.size() + ")");
        }
    }

    public static void removeClient(String username) {
        if (username != null) {
            connectedClients.remove(username);
            logger.info("Client retiré: " + username + " (Total: " + connectedClients.size() + ")");
        }
    }

    public static ClientHandler getClient(String username) {
        return connectedClients.get(username);
    }

    public static boolean isClientConnected(String username) {
        return username != null && connectedClients.containsKey(username);
    }

    public static int getConnectedClientsCount() {
        return connectedClients.size();
    }

    public static void main(String[] args) {
        Server server = new Server();
        
        // Ajout d'un hook pour arrêter proprement le serveur
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Signal d'arrêt reçu, arrêt du serveur...");
            server.stop();
        }));
        
        server.start();
    }
}