package sn.examen_messagerie.server;

import sn.examen_messagerie.entity.ChatMessage;
import sn.examen_messagerie.service.UserService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final UserService userService;
    private String currentUser;
    private ObjectOutputStream out;  // flux pour envoyer au client

    public ClientHandler(Socket socket, UserService userService) {
        this.socket = socket;
        this.userService = userService;
    }

    @Override
    public void run() {

        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            while (true) {

                String action = (String) in.readObject();

                if ("register".equalsIgnoreCase(action)) {

                    String username = (String) in.readObject();
                    String password = (String) in.readObject();

                    boolean success = userService.register(username, password);
                    out.writeObject(success);
                    out.flush();

                } else if ("login".equalsIgnoreCase(action)) {

                    String username = (String) in.readObject();
                    String password = (String) in.readObject();

                    boolean success = userService.login(username, password);

                    if (success) {
                        currentUser = username;
                    }

                    out.writeObject(success);
                    out.flush();

                } else if ("logout".equalsIgnoreCase(action)) {

                    if (currentUser != null) {
                        userService.logout(currentUser);
                        currentUser = null;
                    }

                    out.writeObject(true);
                    out.flush();
                    break; // on sort de la boucle

                } else if ("exit".equalsIgnoreCase(action)) {

                    break; // fermeture complète

                } else {
                    out.writeObject(false);
                    out.flush();
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur client : " + e.getMessage());
        } finally {
            try {
                socket.close();
                System.out.println("Client déconnecté.");
            } catch (Exception ignored) {}
        }
    }

    public void sendMessage(ChatMessage message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du message à " + message.getReceiver());
        }
    }
}