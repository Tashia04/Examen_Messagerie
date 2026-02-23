module examen.messagerie {
    requires jakarta.persistence;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires static lombok;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires jbcrypt;


    opens sn.examen_messagerie to javafx.fxml;
    opens sn.examen_messagerie.entity;

    exports sn.examen_messagerie;
}