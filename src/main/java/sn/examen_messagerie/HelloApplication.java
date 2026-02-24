package sn.examen_messagerie;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

// Point d'entrée de l'application JavaFX (côté client)
public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Charger la page de connexion au démarrage
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 400, 500);
        stage.setTitle("Messagerie - Connexion");
        stage.setScene(scene);
        stage.show();
    }
}
