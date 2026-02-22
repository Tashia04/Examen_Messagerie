package sn.examen_messagerie.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
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
}
