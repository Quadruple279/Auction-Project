package client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField userName;
    @FXML
    private PasswordField password;
    @FXML
    private Label messageLabel;

    @FXML
    public void handleLogin() {
        String tenDangNhap = userName.getText();
        String matKhau = password.getText();
        if (tenDangNhap.isEmpty() || matKhau.isEmpty()) {
            messageLabel.setText("Không được để trống Tên đăng nhập và Mật khẩu.");
            return; // can co return neu khong chuong trinh loi van se chay tiep xuong duoi chu khong dung lai
        }
        if (tenDangNhap.equals("Sang") && matKhau.equals("1234a")) { // Kiem tra xem co dung tenDangNhap va matKhau cua tai khoan khong; tam thoi thi de v nao ket noi server sau
            switchScene("/fxml/AuctionView.fxml");
        } else {
            messageLabel.setText("Tên đăng nhập hoặc Mật khẩu không trùng khớp.");
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
