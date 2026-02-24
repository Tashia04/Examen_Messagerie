package sn.examen_messagerie.controller;

import sn.examen_messagerie.server.Server;
import sn.examen_messagerie.server.ClientHandler;
import sn.examen_messagerie.entity.ChatMessage;
import sn.examen_messagerie.entity.MessageStatus;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatController {

    private static final Logger logger = Logger.getLogger(ChatController.class.getName());

    /**
     * Envoie un message d'un client vers un autre
     * @param msg Le message à envoyer
     * @return true si le message a été envoyé avec succès, false sinon
     */
    public boolean sendMessage(ChatMessage msg) {
        if (msg == null) {
            logger.warning("Tentative d'envoi d'un message null");
            return false;
        }

        if (msg.getReceiver() == null || msg.getReceiver().trim().isEmpty()) {
            logger.warning("Destinataire invalide pour le message");
            return false;
        }

        if (msg.getSender() == null || msg.getSender().trim().isEmpty()) {
            logger.warning("Expéditeur invalide pour le message");
            return false;
        }

        // Vérifie si le destinataire est en ligne
        ClientHandler receiver = Server.getClient(msg.getReceiver());

        if (receiver != null && receiver.isConnected()) {
            msg.setStatus(MessageStatus.RECU); // serveur marque comme reçu
            receiver.sendMessage(msg);
            logger.info("Message envoyé de " + msg.getSender() + " à " + msg.getReceiver());
            return true;
        } else {
            msg.setStatus(MessageStatus.ENVOYE); // destinataire offline, reste envoyé
            logger.info("Destinataire " + msg.getReceiver() + " hors ligne, message marqué comme ENVOYE");
            return false;
        }
    }

    /**
     * Marque un message comme lu et notifie l'expéditeur
     * @param msg Le message à marquer comme lu
     * @return true si le message a été marqué comme lu avec succès, false sinon
     */
    public boolean markAsRead(ChatMessage msg) {
        if (msg == null) {
            logger.warning("Tentative de marquage d'un message null");
            return false;
        }

        if (msg.getSender() == null || msg.getSender().trim().isEmpty()) {
            logger.warning("Expéditeur invalide pour le message");
            return false;
        }

        ClientHandler sender = Server.getClient(msg.getSender());
        if (sender != null && sender.isConnected()) {
            msg.setStatus(MessageStatus.LU);
            sender.sendMessage(msg); // Notifie l'expéditeur que le message a été lu
            logger.info("Message marqué comme lu par " + msg.getReceiver() + ", expéditeur notifié: " + msg.getSender());
            return true;
        } else {
            logger.info("Expéditeur " + msg.getSender() + " hors ligne, notification non envoyée");
            return false;
        }
    }
}