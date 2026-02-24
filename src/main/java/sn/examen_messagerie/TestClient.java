package sn.examen_messagerie;

import sn.examen_messagerie.entity.ChatMessage;
import sn.examen_messagerie.entity.MessageStatus;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TestClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 5000);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            // Crée un message de test
            ChatMessage msg = new ChatMessage(
                    "MESSAGE",
                    "fatim",
                    "mama",
                    "Salut, ça marche ?",
                    MessageStatus.ENVOYE
            );

            // Envoie le message
            out.writeObject(msg);
            out.flush();

            // Essaie de lire la réponse
            ChatMessage response = (ChatMessage) in.readObject();
            System.out.println("Réponse reçue : " + response.getStatut());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}