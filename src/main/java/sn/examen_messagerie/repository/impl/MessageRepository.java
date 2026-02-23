package sn.examen_messagerie.repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import sn.examen_messagerie.entity.ChatMessage;
import sn.examen_messagerie.entity.MessageStatus;
import sn.examen_messagerie.utils.JPAUtils;

import java.util.List;

public class MessageRepository {

    // Sauvegarder un message
    public void save(ChatMessage message) {
        EntityManager em = JPAUtils.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(message);
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    // Récupérer tous les messages reçus par un utilisateur
    public List<ChatMessage> findByReceiver(String receiver) {
        EntityManager em = JPAUtils.getEntityManagerFactory().createEntityManager();
        try {
            return em.createQuery(
                            "SELECT m FROM ChatMessage m WHERE m.receiver = :receiver", ChatMessage.class)
                    .setParameter("receiver", receiver)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Récupérer tous les messages envoyés par un utilisateur
    public List<ChatMessage> findBySender(String sender) {
        EntityManager em = JPAUtils.getEntityManagerFactory().createEntityManager();
        try {
            return em.createQuery(
                            "SELECT m FROM ChatMessage m WHERE m.sender = :sender", ChatMessage.class)
                    .setParameter("sender", sender)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Récupérer un message par son ID
    public ChatMessage findById(Long id) {
        EntityManager em = JPAUtils.getEntityManagerFactory().createEntityManager();
        try {
            return em.find(ChatMessage.class, id);
        } finally {
            em.close();
        }
    }

    // Mettre à jour le statut d'un message
    public void updateStatus(Long messageId, MessageStatus status) {
        EntityManager em = JPAUtils.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            ChatMessage msg = em.find(ChatMessage.class, messageId);
            if (msg != null) {
                msg.setStatus(status);
                em.merge(msg);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    // Supprimer un message (optionnel)
    public void delete(Long messageId) {
        EntityManager em = JPAUtils.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            ChatMessage msg = em.find(ChatMessage.class, messageId);
            if (msg != null) {
                em.remove(msg);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}