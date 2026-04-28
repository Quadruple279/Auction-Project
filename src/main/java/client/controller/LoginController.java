package client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import server.controller.AuthenticationController;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField userName;
    @FXML
    private PasswordField password;
    @FXML
    private Label messageLabel;

    private AuthenticationController authenticationController = new AuthenticationController();

    @FXML
    public void handleLogin() {
        String tenDangNhap = userName.getText().trim();
        String matKhau = password.getText().trim();
        if (tenDangNhap.isEmpty() || matKhau.isEmpty()) {
            messageLabel.setText("Không được để trống Tên đăng nhập và Mật khẩu.");
            return; // can co return neu khong chuong trinh loi van se chay tiep xuong duoi chu khong dung lai
        }
        try {
            // Đăng nhập qua AuthenticationController
            authenticationController.login(tenDangNhap, matKhau);
            messageLabel.setText("Đăng nhập thành công.");
            openDashboard();

        } catch (Exception e) {
            messageLabel.setText("Sai tài khoản hoặc mật khẩu.");
        }
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AuctionView.fxml")
            );
            Parent root = loader.load();

            // Lấy controller Dashboard và truyền auth vào
            AuctionController dashboardController = loader.getController();
            dashboardController.setAuthenticationController(authenticationController);

            Stage stage = (Stage) messageLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("BidNow — Danh sách đấu giá");
            stage.show();

        } catch (IOException e) {
            messageLabel.setText("Lỗi: Không thể mở dashboard");
        }
    }

    @FXML
    private void handleRegister() {
        switchScene("/fxml/RegisterView.fxml");
    }

    private void switchScene(String fxmlPath) { // Được sử dụng để chuyển đổi màn hình
        try {
            FXMLLoader loader = new FXMLLoader( getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage)  messageLabel.getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Loi: Khong the tai man hinh");
        }
    }
}
