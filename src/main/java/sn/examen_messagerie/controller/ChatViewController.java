package sn.examen_messagerie.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import sn.examen_messagerie.client.Client;
import sn.examen_messagerie.entity.ChatMessage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

// Contrôleur de la page de chat
public class ChatViewController implements Initializable {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label contactLabel;

    @FXML
    private ListView<String> userListView;

    @FXML
    private ListView<String> messageListView;

    @FXML
    private TextField messageField;

    // Liste des messages affichés dans la zone de chat
    private ObservableList<String> messages = FXCollections.observableArrayList();

    // Liste des utilisateurs connectés
    private ObservableList<String> users = FXCollections.observableArrayList();

    // L'utilisateur avec qui on discute actuellement
    private String selectedUser;

    // Format pour afficher les dates
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private Client client;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        client = Client.getInstance();

        // Afficher le nom de l'utilisateur connecté
        welcomeLabel.setText("Bienvenue, " + client.getCurrentUser());

        // Lier les listes aux composants visuels
        userListView.setItems(users);
        messageListView.setItems(messages);

        // Quand on clique sur un utilisateur dans la liste
        userListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        onUserSelected(newValue);
                    }
                }
        );

        // Définir le callback pour les messages reçus du serveur
        client.setOnMessageReceived(this::handleServerMessage);

        // Lancer le thread d'écoute du serveur
        client.startListening();

        // Demander la liste des utilisateurs connectés
        ChatMessage getUsersRequest = new ChatMessage();
        getUsersRequest.setAction("get_users");
        client.send(getUsersRequest);
    }

    // Traiter les messages reçus du serveur
    private void handleServerMessage(ChatMessage msg) {
        switch (msg.getAction()) {

            case "user_list":
                // Mise à jour de la liste des utilisateurs connectés
                users.clear();
                String content = msg.getContenu();
                if (content != null && !content.isEmpty()) {
                    String[] userArray = content.split(",");
                    for (String user : userArray) {
                        // Ne pas afficher soi-même dans la liste
                        if (!user.equals(client.getCurrentUser())) {
                            users.add(user);
                        }
                    }
                }
                break;

            case "receive_message":
                // Message reçu d'un autre utilisateur
                if (msg.getSender().equals(selectedUser)) {
                    // Le message vient de l'utilisateur sélectionné -> l'afficher
                    String time = (msg.getDateEnvoi() != null) ? msg.getDateEnvoi().format(dateFormatter) : "";
                    messages.add(msg.getSender() + " [" + time + "] : " + msg.getContenu());

                    // Scroller vers le bas
                    messageListView.scrollTo(messages.size() - 1);
                }
                break;

            case "history":
                // Message de l'historique
                String time = (msg.getDateEnvoi() != null) ? msg.getDateEnvoi().format(dateFormatter) : "";
                String prefix = msg.getSender().equals(client.getCurrentUser()) ? "Moi" : msg.getSender();
                messages.add(prefix + " [" + time + "] : " + msg.getContenu());
                break;

            case "history_end":
                // Fin de l'historique -> scroller vers le bas
                if (!messages.isEmpty()) {
                    messageListView.scrollTo(messages.size() - 1);
                }
                break;

            case "message_sent":
                // Confirmation d'envoi -> afficher le message dans la zone de chat
                // (le message a déjà été ajouté localement dans onSendMessage)
                break;

            case "error":
                // Erreur du serveur
                messages.add("[ERREUR] " + msg.getContenu());
                break;

            case "connection_lost":
                // Perte de connexion (RG10)
                messages.add("[ERREUR] Connexion au serveur perdue !");
                contactLabel.setText("DECONNECTE");
                break;

            default:
                break;
        }
    }

    // Appelé quand on sélectionne un utilisateur dans la liste
    private void onUserSelected(String username) {
        selectedUser = username;
        contactLabel.setText("Discussion avec " + username);

        // Vider la zone de messages
        messages.clear();

        // Demander l'historique des messages avec cet utilisateur
        ChatMessage historyRequest = new ChatMessage();
        historyRequest.setAction("get_history");
        historyRequest.setReceiver(username);
        client.send(historyRequest);
    }

    // Appelé quand on clique sur "Envoyer" ou quand on appuie sur Entrée
    @FXML
    private void onSendMessage() {
        String content = messageField.getText().trim();

        // Vérifier qu'un destinataire est sélectionné
        if (selectedUser == null) {
            messages.add("[INFO] Sélectionnez d'abord un utilisateur");
            return;
        }

        // Vérifier que le message n'est pas vide
        if (content.isEmpty()) {
            return;
        }

        // RG7 : le message ne doit pas dépasser 1000 caractères
        if (content.length() > 1000) {
            messages.add("[INFO] Le message ne doit pas dépasser 1000 caractères");
            return;
        }

        // Envoyer le message au serveur
        ChatMessage msg = new ChatMessage("send_message", client.getCurrentUser(), selectedUser, content, null);
        client.send(msg);

        // Afficher le message localement (pas besoin d'attendre la confirmation)
        String time = java.time.LocalDateTime.now().format(dateFormatter);
        messages.add("Moi [" + time + "] : " + content);

        // Scroller vers le bas
        messageListView.scrollTo(messages.size() - 1);

        // Vider le champ de saisie
        messageField.clear();
    }

    // Appelé quand on clique sur "Déconnexion"
    @FXML
    private void onLogout() {
        // Se déconnecter du serveur
        client.disconnect();

        // Revenir à la page de connexion
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sn/examen_messagerie/login-view.fxml"));
            Scene loginScene = new Scene(loader.load(), 400, 500);

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setTitle("Messagerie - Connexion");
            stage.setScene(loginScene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
