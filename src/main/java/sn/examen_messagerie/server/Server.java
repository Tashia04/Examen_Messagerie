package sn.examen_messagerie.server;

import sn.examen_messagerie.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final int Port =5000;
    private UserService userService = new UserService();

    public void start(){
        try(ServerSocket serverSocket = new ServerSocket(Port)) {
            System.out.println("Server demarré sur le prt " + Port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                //chaque client a son thread
                new Thread(new ClientHandler(clientSocket, userService)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        new Server().start();
    }
}
