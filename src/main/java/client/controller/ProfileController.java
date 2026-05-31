package client.controller;

import client.ClientSocket;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import shared.dto.BidHistoryDTO;
import shared.dto.UserDTO;
import shared.protocol.MessageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;


import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Button       backButton;
    @FXML private Label        avatarLabel;
    @FXML private Label        displayNameLabel;
    @FXML private Label        displayRoleLabel;
    @FXML private TextField    fullNameField;
    @FXML private TextField    usernameField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label        messageLabel;
    @FXML private TableView<BidHistoryDTO>           historyTable;
    @FXML private TableColumn<BidHistoryDTO, String> colAuctionId;
    @FXML private TableColumn<BidHistoryDTO, String> colBidAmount;
    @FXML private TableColumn<BidHistoryDTO, String> colBidTime;

    private UserDTO currentUser;

    // Named listeners để remove đúng cách
    private ClientSocket.ResponseListener historyListener;
    private ClientSocket.ResponseListener saveListener;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Sẽ load data sau khi setCurrentUser() được gọi
    }

    public void setCurrentUser(UserDTO user) {
        this.currentUser = user;
        loadUserInfo();
        loadHistory();
    }

    private void loadHistory() {
        if (currentUser == null) return;

        colAuctionId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getAuctionId()
                ));
        colBidAmount.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%,.0f ₫", data.getValue().getBidAmount())
                ));
        colBidTime.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getBidTime()
                ));

        // Xóa listener cũ nếu có
        if (historyListener != null) {
            ClientSocket.getInstance().removeResponseListener(
                    MessageType.GET_BID_HISTORY_BY_USER_SUCCESS, historyListener);
        }

        historyListener = msg -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<BidHistoryDTO> list = mapper.readValue(
                        msg.get("data"),
                        new TypeReference<List<BidHistoryDTO>>() {});
                javafx.application.Platform.runLater(() -> {
                    historyTable.setItems(
                            javafx.collections.FXCollections.observableArrayList(list));
                    if (list.isEmpty())
                        historyTable.setPlaceholder(new Label("Chưa có lịch sử đấu giá nào."));
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        showError("Lỗi tải lịch sử: " + e.getMessage()));
            }
        };

        ClientSocket.getInstance().addResponseListener(
                MessageType.GET_BID_HISTORY_BY_USER_SUCCESS, historyListener);
        ClientSocket.getInstance().sendGetBidHistoryByUser(currentUser.getName());
    }

    private void loadUserInfo() {
        if (currentUser == null) return;

        avatarLabel.setText(
                String.valueOf(currentUser.getDisplayName().charAt(0)).toUpperCase()
        );
        displayNameLabel.setText(currentUser.getDisplayName());
        displayRoleLabel.setText("● " + currentUser.getRole());
        fullNameField.setText(currentUser.getDisplayName());
        usernameField.setText(currentUser.getName());
    }

    @FXML
    private void handleSave() {
        String newDisplayName = fullNameField.getText().trim();
        String newPassword    = newPasswordField.getText();
        String confirmPw      = confirmPasswordField.getText();

        if (newDisplayName.isEmpty()) {
            showError("Tên hiển thị không được để trống.");
            return;
        }
        if (!newPassword.isEmpty()) {
            if (newPassword.length() < 6) {
                showError("Mật khẩu phải có ít nhất 6 ký tự.");
                return;
            }
            if (!newPassword.equals(confirmPw)) {
                showError("Mật khẩu xác nhận không khớp.");
                return;
            }
        }

        // Xóa listener cũ nếu có (tránh duplicate khi bấm Save nhiều lần)
        if (saveListener != null) {
            ClientSocket.getInstance().removeResponseListener(
                    MessageType.UPDATE_USER_SUCCESS, saveListener);
        }

        ClientSocket.ResponseListener[] saveRef = new ClientSocket.ResponseListener[1];
        ClientSocket.ResponseListener[] errorRef = new ClientSocket.ResponseListener[1];

        saveRef[0] = msg -> {
            javafx.application.Platform.runLater(() -> {
                ClientSocket.getInstance().removeResponseListener(
                        MessageType.UPDATE_USER_SUCCESS, saveRef[0]);
                ClientSocket.getInstance().removeResponseListener(
                        MessageType.ERROR, errorRef[0]);

                currentUser.setDisplayName(newDisplayName);
                displayNameLabel.setText(newDisplayName);
                avatarLabel.setText(
                        String.valueOf(newDisplayName.charAt(0)).toUpperCase()
                );
                showSuccess("Cập nhật thành công!");
                newPasswordField.clear();
                confirmPasswordField.clear();
            });
        };

        errorRef[0] = msg -> {
            javafx.application.Platform.runLater(() -> {
                ClientSocket.getInstance().removeResponseListener(
                        MessageType.UPDATE_USER_SUCCESS, saveRef[0]);
                ClientSocket.getInstance().removeResponseListener(
                        MessageType.ERROR, errorRef[0]);

                showError("Lỗi: " + msg.getOrDefault("reason", "Không thể cập nhật"));
            });
        };

        ClientSocket.getInstance().addResponseListener(MessageType.UPDATE_USER_SUCCESS, saveRef[0]);
        ClientSocket.getInstance().addResponseListener(MessageType.ERROR, errorRef[0]);

        ClientSocket.getInstance().sendUpdateUser(newDisplayName,
                newPassword.isEmpty() ? null : newPassword);
    }

    @FXML
    private void handleBack() {
        // Dọn listener trước khi rời màn hình
        if (historyListener != null) {
            ClientSocket.getInstance().removeResponseListener(
                    MessageType.GET_BID_HISTORY_BY_USER_SUCCESS, historyListener);
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AuctionView.fxml")
            );
            Parent root = loader.load();

            AuctionListController listController = loader.getController();
            listController.setCurrentUser(currentUser);

            Stage stage = (Stage) avatarLabel.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            showError("Lỗi: Không thể quay lại.");
        }
    }

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: #ff4444;");
        messageLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setStyle("-fx-text-fill: #4caf50;");
        messageLabel.setText(msg);
    }
}