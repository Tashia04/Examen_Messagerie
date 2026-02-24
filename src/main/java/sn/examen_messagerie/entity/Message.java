package sn.examen_messagerie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
// @Entity et @Table retirés : c'est ChatMessage qui gère la table "messages"
// Cette classe est conservée comme modèle alternatif (non utilisée en base)
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User sender;

    @ManyToOne
    private User receiver;

    @Column(length = 1000)
    private String contenu;

    private LocalDateTime dateEnvoi;

    @Enumerated(EnumType.STRING)
    private MessageStatus statut;

    public Message() {
    }

    public Message(Long id, User sender, User receiver, String contenu, LocalDateTime dateEnvoi, MessageStatus statut) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.statut = statut;
    }

    public Message(User sender, User receiver, String contenu, LocalDateTime dateEnvoi, MessageStatus statut) {
        this.sender = sender;
        this.receiver = receiver;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.statut = statut;
    }

    // Constructeur sans statut (statut par défaut : ENVOYE)
    public Message(User sender, User receiver, String content, LocalDateTime dateEnvoi) {
        this.sender = sender;
        this.receiver = receiver;
        this.contenu = content;
        this.dateEnvoi = dateEnvoi;
        this.statut = MessageStatus.ENVOYE;
    }

    // Permet de changer le statut du message (ENVOYE -> RECU -> LU)
    public void setStatus(MessageStatus messageStatus) {
        this.statut = messageStatus;
    }
}
