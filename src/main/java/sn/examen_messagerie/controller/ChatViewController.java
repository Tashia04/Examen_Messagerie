package sn.examen_messagerie.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    private Label statusLabel;

    @FXML
    private ListView<String> userListView;

    @FXML
    private ListView<DisplayMessage> messageListView;

    @FXML
    private TextField messageField;

    // Liste des messages affichés dans la zone de chat
    private ObservableList<DisplayMessage> messages = FXCollections.observableArrayList();

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

        // Custom cell factory pour design WhatsApp
        messageListView.setCellFactory(param -> new MessageCell());

        // Quand on clique sur un utilisateur dans la liste
        userListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        onUserSelected(newValue);
                    }
                });

        users.addListener((ListChangeListener.Change<? extends String> c) -> {
            Platform.runLater(this::updateStatusLabel);
        });

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
        Platform.runLater(() -> {
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
                        String time = (msg.getDateEnvoi() != null) ? msg.getDateEnvoi().format(dateFormatter) : "";
                        messages.add(new DisplayMessage(msg.getContenu(), time, false, false, null, msg.getSender()));
                        messageListView.scrollTo(messages.size() - 1);
                    }
                    break;

                case "history":
                    // Message de l'historique
                    String hTime = (msg.getDateEnvoi() != null) ? msg.getDateEnvoi().format(dateFormatter) : "";
                    boolean isMe = msg.getSender().equals(client.getCurrentUser());
                    // Messages of history downloaded are already read at this point
                    MessageStatus st = isMe ? MessageStatus.LU : null;
                    messages.add(new DisplayMessage(msg.getContenu(), hTime, isMe, false, st, msg.getSender()));
                    break;

                case "history_end":
                    // Fin de l'historique -> scroller vers le bas
                    if (!messages.isEmpty()) {
                        messageListView.scrollTo(messages.size() - 1);
                    }
                    break;

                case "message_sent":
                    // Confirmation d'envoi -> on pourrait mettre à jour le statut du message à
                    // RECU/LU
                    break;

                case "error":
                    // Erreur du serveur
                    messages.add(new DisplayMessage("[ERREUR] " + msg.getContenu(), "", false, true, null, "Système"));
                    break;

                case "connection_lost":
                    // Perte de connexion (RG10)
                    messages.add(new DisplayMessage("[ERREUR] Connexion au serveur perdue !", "", false, true, null,
                            "Système"));
                    contactLabel.setText("DECONNECTE");
                    statusLabel.setText("");
                    break;

                default:
                    break;
            }
        });
    }

    private void onUserSelected(String username) {
        selectedUser = username;
        contactLabel.setText(username);
        updateStatusLabel();

        // Vider la zone de messages
        messages.clear();

        // Demander l'historique des messages avec cet utilisateur
        ChatMessage historyRequest = new ChatMessage();
        historyRequest.setAction("get_history");
        historyRequest.setReceiver(username);
        client.send(historyRequest);
    }

    @FXML
    private void onSendMessage() {
        String content = messageField.getText().trim();

        // Vérifier qu'un destinataire est sélectionné
        if (selectedUser == null) {
            messages.add(new DisplayMessage("Sélectionnez d'abord un utilisateur", "", false, true, null, "Système"));
            return;
        }

        // Vérifier que le message n'est pas vide
        if (content.isEmpty()) {
            return;
        }

        // RG7 : le message ne doit pas dépasser 1000 caractères
        if (content.length() > 1000) {
            messages.add(new DisplayMessage("Le message ne doit pas dépasser 1000 caractères", "", false, true, null,
                    "Système"));
            return;
        }

        // Envoyer le message au serveur
        ChatMessage msg = new ChatMessage("send_message", client.getCurrentUser(), selectedUser, content, null);
        client.send(msg);

        // Si l'utilisateur est en ligne, le message est directement marqué comme Lu
        // (bleu), sinon juste Envoyé
        MessageStatus initialStatus = users.contains(selectedUser) ? MessageStatus.LU : MessageStatus.ENVOYE;

        // Afficher le message localement (pas besoin d'attendre la confirmation)
        String time = java.time.LocalDateTime.now().format(dateFormatter);
        messages.add(new DisplayMessage(content, time, true, false, initialStatus, client.getCurrentUser()));

        // Scroller vers le bas
        messageListView.scrollTo(messages.size() - 1);

        // Vider le champ de saisie
        messageField.clear();
    }

    @FXML
    private void onLogout() {
        client.disconnect();

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

    private void updateStatusLabel() {
        if (selectedUser != null) {
            if (users.contains(selectedUser)) {
                statusLabel.setText("En ligne");
                statusLabel.setStyle("-fx-text-fill: #128C7E; -fx-font-size: 12;");
            } else {
                statusLabel.setText("Hors ligne");
                statusLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12;");
            }
        } else {
            statusLabel.setText("");
        }
    }

    public enum MessageStatus {
        ENVOYE, RECU, LU
    }

    public static class DisplayMessage {
        private String text;
        private String time;
        private boolean isMe;
        private boolean isSystem;
        private MessageStatus status;
        private String sender;

        public DisplayMessage(String text, String time, boolean isMe, boolean isSystem, MessageStatus status,
                String sender) {
            this.text = text;
            this.time = time;
            this.isMe = isMe;
            this.isSystem = isSystem;
            this.status = status;
            this.sender = sender;
        }

        public String getText() {
            return text;
        }

        public String getTime() {
            return time;
        }

        public boolean isMe() {
            return isMe;
        }

        public boolean isSystem() {
            return isSystem;
        }

        public MessageStatus getStatus() {
            return status;
        }

        public String getSender() {
            return sender;
        }

        public void setStatus(MessageStatus status) {
            this.status = status;
        }
    }

    private class MessageCell extends ListCell<DisplayMessage> {
        private HBox root = new HBox();
        private VBox bubble = new VBox();
        private Label textLabel = new Label();
        private Label timeLabel = new Label();
        private Label statusIcon = new Label();
        private HBox timeStatusBox = new HBox();

        public MessageCell() {
            super();
            textLabel.setWrapText(true);
            textLabel.setMaxWidth(400);
            timeLabel.getStyleClass().add("chat-time");
            statusIcon.setStyle("-fx-font-size: 10px;");

            timeStatusBox.getChildren().addAll(timeLabel, statusIcon);
            timeStatusBox.setSpacing(5);
            timeStatusBox.setAlignment(Pos.CENTER_RIGHT);

            bubble.getChildren().addAll(textLabel, timeStatusBox);
            bubble.setPadding(new Insets(8));
            bubble.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            root.getChildren().add(bubble);

            setGraphicTextGap(0);
        }

        @Override
        protected void updateItem(DisplayMessage message, boolean empty) {
            super.updateItem(message, empty);
            if (empty || message == null) {
                setGraphic(null);
            } else {
                textLabel.setText(message.getText());
                timeLabel.setText(message.getTime());

                bubble.getStyleClass().removeAll("message-bubble-sent", "message-bubble-received",
                        "message-bubble-system");
                statusIcon.setText("");

                if (message.isSystem()) {
                    bubble.getStyleClass().add("message-bubble-system");
                    root.setAlignment(Pos.CENTER);
                    timeStatusBox.setVisible(false);
                    timeStatusBox.setManaged(false);
                } else if (message.isMe()) {
                    bubble.getStyleClass().add("message-bubble-sent");
                    root.setAlignment(Pos.CENTER_RIGHT);
                    timeStatusBox.setVisible(true);
                    timeStatusBox.setManaged(true);

                    if (message.getStatus() == MessageStatus.ENVOYE) {
                        statusIcon.setText("✔");
                        statusIcon.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");
                    } else if (message.getStatus() == MessageStatus.RECU) {
                        statusIcon.setText("✔✔");
                        statusIcon.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");
                    } else if (message.getStatus() == MessageStatus.LU) {
                        statusIcon.setText("✔✔");
                        statusIcon.setStyle("-fx-text-fill: #34B7F1; -fx-font-size: 10px;"); // WhatsApp Blue
                    }
                } else {
                    bubble.getStyleClass().add("message-bubble-received");
                    root.setAlignment(Pos.CENTER_LEFT);
                    timeStatusBox.setVisible(true);
                    timeStatusBox.setManaged(true);
                }

                setGraphic(root);
            }
        }
    }
}
