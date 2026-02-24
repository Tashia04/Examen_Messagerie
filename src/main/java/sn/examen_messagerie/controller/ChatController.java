package sn.examen_messagerie.controller;

import sn.examen_messagerie.server.Server;
import sn.examen_messagerie.server.ClientHandler;
import sn.examen_messagerie.entity.ChatMessage;
import sn.examen_messagerie.entity.MessageStatus;

public class ChatController {

    // Envoyer un message d'un client vers un autre
    public void sendMessage(ChatMessage msg) {
        // Vérifie si le destinataire est en ligne
        ClientHandler receiver = Server.connectedClients.get(msg.getReceiver());

        if (receiver != null) {
            msg.setStatut(MessageStatus.RECU); // serveur marque comme reçu
            receiver.sendMessage(msg);
        } else {
            msg.setStatut(MessageStatus.ENVOYE); // destinataire offline, reste envoyé
        }
    }

    // Marquer un message comme lu
    public void markAsRead(ChatMessage msg) {
        ClientHandler sender = Server.connectedClients.get(msg.getSender());
        if (sender != null) {
            msg.setStatut(MessageStatus.LU);
            sender.sendMessage(msg); // Notifie l'expéditeur que le message a été lu
        }
    }
}