package sn.examen_messagerie;

import jakarta.persistence.EntityManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import sn.examen_messagerie.repository.impl.MessageRepository;

import java.net.URL;
import java.util.ResourceBundle;

public class HelloController implements Initializable {
    private MessageRepository messageRepository;

    public HelloController() {
       messageRepository = new MessageRepository();
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}
