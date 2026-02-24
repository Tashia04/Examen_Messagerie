package sn.examen_messagerie.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;

// Entité Message : correspond au modèle de données du sujet
// Sert aussi de message réseau (Serializable) pour le protocole client-serveur
@Entity
@Table(name = "messages")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;      // champ technique pour le protocole (login, send_message, etc.)
    private String sender;      // expéditeur
    private String receiver;    // destinataire
    private String contenu;     // contenu du message

    @Enumerated(EnumType.STRING)
    private MessageStatus statut;   // statut du message (ENVOYE, RECU, LU)

    private LocalDateTime dateEnvoi; // date d'envoi

    // Constructeur vide (obligatoire pour JPA)
    public ChatMessage() {
    }

    // Constructeur complet
    public ChatMessage(String action, String sender, String receiver, String contenu, MessageStatus statut) {
        this.action = action;
        this.sender = sender;
        this.receiver = receiver;
        this.contenu = contenu;
        this.statut = statut;
        this.dateEnvoi = LocalDateTime.now();
    }

    // ============ Getters et Setters ============

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public MessageStatus getStatut() {
        return statut;
    }

    public void setStatut(MessageStatus statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }
}
