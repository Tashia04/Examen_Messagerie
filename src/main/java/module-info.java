module examen.messagerie {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // Ouvrir les packages au framework JavaFX (nécessaire pour le FXML)
    opens sn.examen_messagerie to javafx.fxml;
    opens sn.examen_messagerie.controller to javafx.fxml;

    // Exporter les packages pour qu'ils soient accessibles
    exports sn.examen_messagerie;
    exports sn.examen_messagerie.entity;
    exports sn.examen_messagerie.controller;
    exports sn.examen_messagerie.client;
}
