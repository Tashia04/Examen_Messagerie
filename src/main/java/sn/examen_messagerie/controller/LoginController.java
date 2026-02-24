package sn.examen_messagerie.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import sn.examen_messagerie.client.Client;
import sn.examen_messagerie.entity.ChatMessage;

// Contrôleur de la page de connexion / inscription
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    // Appelé quand l'utilisateur clique sur "Connexion"
    @FXML
    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Vérification des champs
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Veuillez remplir tous les champs");
            return;
        }

        // Connexion au serveur dans un thread séparé (pour ne pas bloquer l'interface)
        new Thread(() -> {
            try {
                Client client = Client.getInstance();

                // Se connecter au serveur si pas encore connecté
                if (!client.isConnected()) {
                    client.connect("localhost", 9000);
                }

                // Envoyer la demande de login
                ChatMessage loginRequest = new ChatMessage("login", username, null, password, null);
                ChatMessage response = client.sendAndReceive(loginRequest);

                // Traiter la réponse sur le thread JavaFX
                javafx.application.Platform.runLater(() -> {
                    if (response != null && "OK".equals(response.getContenu())) {
                        // Connexion réussie
                        client.setCurrentUser(username);
                        switchToChat();
                    } else {
                        // Echec de connexion
                        statusLabel.setStyle("-fx-text-fill: red;");
                        String erreur = (response != null) ? response.getContenu() : "Impossible de contacter le serveur";
                        statusLabel.setText(erreur);
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Impossible de se connecter au serveur");
                });
            }
        }).start();
    }

    // Appelé quand l'utilisateur clique sur "Inscription"
    @FXML
    private void onRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        // Vérification des champs
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Veuillez remplir tous les champs");
            return;
        }

        // Inscription dans un thread séparé
        new Thread(() -> {
            try {
                Client client = Client.getInstance();

                // Se connecter au serveur si pas encore connecté
                if (!client.isConnected()) {
                    client.connect("localhost", 9000);
                }

                // Envoyer la demande d'inscription
                ChatMessage registerRequest = new ChatMessage("register", username, null, password, null);
                ChatMessage response = client.sendAndReceive(registerRequest);

                // Traiter la réponse sur le thread JavaFX
                javafx.application.Platform.runLater(() -> {
                    if (response != null && "OK".equals(response.getContenu())) {
                        statusLabel.setStyle("-fx-text-fill: green;");
                        statusLabel.setText("Inscription réussie ! Vous pouvez vous connecter.");
                    } else {
                        statusLabel.setStyle("-fx-text-fill: red;");
                        String erreur = (response != null) ? response.getContenu() : "Impossible de contacter le serveur";
                        statusLabel.setText(erreur);
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setStyle("-fx-text-fill: red;");
                    statusLabel.setText("Impossible de se connecter au serveur");
                });
            }
        }).start();
    }

    // Basculer vers la page de chat après une connexion réussie
    private void switchToChat() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sn/examen_messagerie/chat-view.fxml"));
            Scene chatScene = new Scene(loader.load(), 800, 600);

            // Récupérer le stage actuel et changer la scène
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setTitle("Messagerie - " + Client.getInstance().getCurrentUser());
            stage.setScene(chatScene);
        } catch (Exception e) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Erreur lors du chargement du chat");
            e.printStackTrace();
        }
    }
}
