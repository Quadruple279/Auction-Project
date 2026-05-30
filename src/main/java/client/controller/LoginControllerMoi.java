package client.controller;

import client.ClientSocket;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;

import javafx.scene.control.*;
import javafx.stage.Stage;

import shared.dto.UserDTO;
import shared.protocol.MessageType;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginControllerMoi implements Initializable {

    @FXML private Button        buttonLogin;
    @FXML private Button        buttonRegister;
    @FXML private TextField     userName;
    @FXML private PasswordField password;
    @FXML private Label         messageLabel;



    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setActiveTab("LOGIN");

        // Bấm Enter ở bất kỳ field nào → tự động login
        userName.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) handleLogin();
        });
        password.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) handleLogin();
        });
    }

    private void setActiveTab(String tab) {
        String activeStyle  = "-fx-background-color: #3b82f6;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 10 0 0 10;" +
                "-fx-border-color: #374151;";
        String defaultStyle = "-fx-background-color: transparent;" +
                "-fx-background-radius: 0 10 10 0;" +
                "-fx-border-color: #374151;";

        if (tab.equals("LOGIN")) {
            buttonLogin.setStyle(activeStyle);
            buttonLogin.setTextFill(javafx.scene.paint.Color.WHITE);
            buttonRegister.setStyle(defaultStyle);
            buttonRegister.setTextFill(javafx.scene.paint.Color.web("#6b7280"));
        }
    }

    @FXML
    public void handleLogin() {
        String tenDangNhap = userName.getText().trim();
        String matKhau     = password.getText();

        if (tenDangNhap.isEmpty() || matKhau.isEmpty()) {
            showError("Không được để trống tên đăng nhập và mật khẩu.");
            return;
        }
        ClientSocket.getInstance().setResponseListener(msg -> {
            javafx.application.Platform.runLater(() -> {
            if (msg.getType() == MessageType.LOGIN_SUCCESS){
                int id                 = Integer.parseInt(msg.getOrDefault("id", "0"));
                String username        = msg.get("username");
                String displayName     = msg.getOrDefault("displayName", username);
                String role            = msg.get("role");
                UserDTO loggedInUser   = new UserDTO(id, username, displayName, role);
                showSuccess("Đăng nhập thành công!");
                openDashboard(loggedInUser);   // truyền userDTO vào
            }
            else {
                String reason = msg.getOrDefault("reason", "Sai tài khoản hoặc mật khẩu.");
                showError(reason);
            }
            });
        });
        ClientSocket.getInstance().sendLogin(tenDangNhap, matKhau);
        messageLabel.setText("Đang đăng nhập...");
    }

    private void openDashboard(UserDTO userDTO) {
        try {
            // Lấy role của user hiện tại
            String role = userDTO.getRole();

            String fxmlPath;
            String title;

            switch (role) {
                case "SELLER" -> {
                    fxmlPath = "/fxml/SellerView.fxml";
                    title    = "Abstract Auction — Seller Dashboard";
                }
                case "ADMIN" -> {
                    fxmlPath = "/fxml/AdminView.fxml";
                    title    = "Abstract Auction — Admin Dashboard";
                }
                default -> {
                    // BIDDER hoặc role khác → vào AuctionView
                    fxmlPath = "/fxml/AuctionView.fxml";
                    title    = "Abstract Auction — Danh sách đấu giá";
                }
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath)
            );
            Parent root = loader.load();

            // Truyền authController sang controller tương ứng
            switch (role) {
                case "SELLER" -> {
                    SellerController sellerController = loader.getController();
                    sellerController.setCurrentUser(userDTO);   // ← đổi từ setAuthController
                }
                case "ADMIN" -> {
                    AdminController adminController = loader.getController();
                    adminController.setCurrentAdmin(userDTO);
                }
                default -> {
                    AuctionListController dashboardController = loader.getController();
                    dashboardController.setCurrentUser(userDTO); // ← đổi từ setAuthenticationController
                }
            }

            Stage stage = (Stage) messageLabel.getScene().getWindow();
            stage.setTitle(title);
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            showError("Lỗi: Không thể mở dashboard.");
        }
    }

    @FXML
    private void handleSwitchToRegister() {
        switchScene("/fxml/RegisterViewMoi.fxml");
    }

    @FXML
    private void handleRegister() {
        switchScene("/fxml/RegisterViewMoi.fxml");
    }


    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: #fa5656;");
        messageLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setStyle("-fx-text-fill: #4caf50;");
        messageLabel.setText(msg);
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) userName.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Lỗi: Không thể tải màn hình");
        }
    }
}