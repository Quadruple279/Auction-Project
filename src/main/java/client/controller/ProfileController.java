package client.controller;

import client.ClientSocket;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import server.controller.AuthenticationController;
import server.dao.BidTransactionDAO;
import server.model.BidTransaction;
import server.model.user.User;
import shared.protocol.MessageType;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
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
    @FXML private TableView<BidTransaction>           historyTable;
    @FXML private TableColumn<BidTransaction, String> colAuctionId;
    @FXML private TableColumn<BidTransaction, String> colBidAmount;
    @FXML private TableColumn<BidTransaction, String> colBidTime;

    private final BidTransactionDAO bidDAO = new BidTransactionDAO();
    private AuthenticationController authController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Sẽ load data sau khi setAuthController() được gọi
    }

    public void setAuthController(AuthenticationController auth) {
        this.authController = auth;
        loadUserInfo();
        loadHistory();
    }

    private void loadHistory() {
        User user = authController.getCurrentUser();
        if (user == null) return;

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
                        data.getValue().getBidTime().toString()
                ));

        try {
            List<BidTransaction> history =
                    bidDAO.findByBidderName(user.getName());

            historyTable.setItems(
                    javafx.collections.FXCollections.observableArrayList(history)
            );

            if (history.isEmpty()) {
                historyTable.setPlaceholder(
                        new Label("Chưa có lịch sử đấu giá nào.")
                );
            }

        } catch (SQLException e) {
            showError("Lỗi tải lịch sử: " + e.getMessage());
        }
    }

    private void loadUserInfo() {
        User user = authController.getCurrentUser();
        if (user == null) return;

        // Avatar — lấy chữ cái đầu của tên
        avatarLabel.setText(
                String.valueOf(user.getTenHienThi().charAt(0)).toUpperCase()
        );

        // Tên và role hiển thị
        displayNameLabel.setText(user.getTenHienThi());
        displayRoleLabel.setText("● " + user.getRole());

        // Điền sẵn vào form
        fullNameField.setText(user.getTenHienThi());
        usernameField.setText(user.getName());
    }

    @FXML
    private void handleSave() {
        String newTenHienThi = fullNameField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPw   = confirmPasswordField.getText();

        if (newTenHienThi.isEmpty()) {
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

        ClientSocket.getInstance().setResponseListener(msg -> {
            if (msg.getType() == MessageType.UPDATE_USER_SUCCESS) {
                javafx.application.Platform.runLater(() -> {
                    displayNameLabel.setText(newTenHienThi);
                    avatarLabel.setText(
                            String.valueOf(newTenHienThi.charAt(0)).toUpperCase()
                    );
                    showSuccess("Cập nhật thành công!");
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                });
            } else if (msg.getType() == MessageType.ERROR) {
                javafx.application.Platform.runLater(() ->
                        showError("Lỗi: " + msg.getOrDefault("reason", "Không thể cập nhật"))
                );
            }
        });

        ClientSocket.getInstance().sendUpdateUser(newTenHienThi,
                newPassword.isEmpty() ? null : newPassword);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AuctionView.fxml")
            );
            Parent root = loader.load();

            AuctionListController listController = loader.getController();
            listController.setAuthenticationController(authController);

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