module examen.messagerie {
    requires jakarta.persistence;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires static lombok;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires jbcrypt;

    // Ouvrir les packages au framework JavaFX (nécessaire pour le FXML)
    opens sn.examen_messagerie to javafx.fxml;
    opens sn.examen_messagerie.controller to javafx.fxml;
    opens sn.examen_messagerie.entity;

    // Exporter les packages pour qu'ils soient accessibles
    exports sn.examen_messagerie;
    exports sn.examen_messagerie.entity;
    exports sn.examen_messagerie.controller;
    exports sn.examen_messagerie.client;
    exports sn.examen_messagerie.service;
    exports sn.examen_messagerie.repository.impl;
}
