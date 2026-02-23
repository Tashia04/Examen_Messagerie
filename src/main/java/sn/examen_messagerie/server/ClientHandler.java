package sn.examen_messagerie.server;

import sn.examen_messagerie.service.UserService;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private UserService userService;

    public ClientHandler(Socket socket, UserService userService) {
        this.socket = socket;
        this.userService = userService;
    }
    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            // Lecture de l'action du client
            String action = (String) in.readObject();
            String username = (String) in.readObject();
            String password = (String) in.readObject();

            if ("register".equalsIgnoreCase(action)) {
                boolean success = userService.register(username, password);
                out.writeObject(success);

            } else if ("login".equalsIgnoreCase(action)) {
                boolean success = userService.login(username, password);
                out.writeObject(success);

            } else if ("logout".equalsIgnoreCase(action)) {
                userService.logout(username);
                out.writeObject(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
