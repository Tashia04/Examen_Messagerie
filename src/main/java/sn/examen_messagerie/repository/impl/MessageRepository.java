package sn.examen_messagerie.repository.impl;

import jakarta.persistence.EntityManager;
import sn.examen_messagerie.utils.JPAUtils;

public class MessageRepository {
    private EntityManager entityManager;

    public MessageRepository(){
        this.entityManager = JPAUtils.getEntityManagerFactory().createEntityManager();
    }
}
