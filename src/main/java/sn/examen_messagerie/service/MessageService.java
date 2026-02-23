package sn.examen_messagerie.service;

import sn.examen_messagerie.entity.Message;
import sn.examen_messagerie.entity.MessageStatus;
import sn.examen_messagerie.entity.User;
import sn.examen_messagerie.entity.Status;


import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import sn.examen_messagerie.utils.JPAUtils;

import java.time.LocalDateTime;
import java.util.List;

public class MessageService {

    // Envoyer un message
    public void sendMessage(User sender, User receiver, String content) {
        EntityManager em = JPAUtils.getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = em.getTransaction();

        try {
            transaction.begin();

            MessageStatus status = (receiver.getStatus() == Status.ONLINE) ? MessageStatus.RECU : MessageStatus.ENVOYE;

            Message message = new Message(sender, receiver, content, LocalDateTime.now(), status);
            em.persist(message);

            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    // Récupérer les messages entre deux utilisateurs
    public List<Message> getMessages(User u1, User u2) {
        EntityManager em =JPAUtils.getEntityManagerFactory().createEntityManager();
        try {
            return em.createQuery(
                            "SELECT m FROM Message m WHERE " +
                                    "(m.sender = :u1 AND m.receiver = :u2) OR " +
                                    "(m.sender = :u2 AND m.receiver = :u1) " +
                                    "ORDER BY m.dateEnvoi", Message.class)
                    .setParameter("u1", u1)
                    .setParameter("u2", u2)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Marquer un message comme LU
    public void markAsRead(Message message) {
        EntityManager em = JPAUtils.getEntityManagerFactory().createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        try {
            transaction.begin();
            message.setStatus(MessageStatus.LU);
            em.merge(message);
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) transaction.rollback();
        } finally {
            em.close();
        }
    }
}