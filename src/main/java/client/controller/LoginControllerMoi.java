package client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import server.controller.AuthenticationController;
import server.exception.AuthenticationException;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginControllerMoi implements Initializable {

    @FXML private Button        buttonLogin;
    @FXML private Button        buttonRegister;
    @FXML private TextField     userName;
    @FXML private PasswordField password;
    @FXML private Label         messageLabel;

    private AuthenticationController authenticationController
            = new AuthenticationController();
    

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setActiveTab("LOGIN");
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

        try {
            authenticationController.login(tenDangNhap, matKhau);
            showSuccess("Đăng nhập thành công!");
            openDashboard();

        } catch (AuthenticationException e) {
            showError("Sai tài khoản hoặc mật khẩu.");
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    private void openDashboard() {
        try {
            // Lấy role của user hiện tại
            String role = authenticationController.getCurrentUser().getRole();

            String fxmlPath;
            String title;

            switch (role) {
                case "SELLER" -> {
                    fxmlPath = "/fxml/SellerView.fxml";
                    title    = "BidNow — Seller Dashboard";
                }
                case "ADMIN" -> {
                    fxmlPath = "/fxml/AdminView.fxml";
                    title    = "BidNow — Admin Dashboard";
                }
                default -> {
                    // BIDDER hoặc role khác → vào AuctionView
                    fxmlPath = "/fxml/AuctionView.fxml";
                    title    = "BidNow — Danh sách đấu giá";
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
                    sellerController.setAuthController(authenticationController);
                }
                case "ADMIN" -> {
                    // TODO: AdminController sau
                    // AdminController adminController = loader.getController();
                    // adminController.setAuthController(authenticationController);
                }
                default -> {
                    AuctionController dashboardController = loader.getController();
                    dashboardController.setAuthenticationController(authenticationController);
                }
            }

            Stage stage = (Stage) messageLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();

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

    public void setAuthenticationController(AuthenticationController auth) {
        this.authenticationController = auth;
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
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath)
            );
            Parent root = loader.load();
            Stage stage  = (Stage) messageLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Lỗi: Không thể tải màn hình.");
        }
    }
}