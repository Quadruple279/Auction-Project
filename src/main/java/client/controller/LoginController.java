package client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML
    TextField userName;
    @FXML
    PasswordField password;
    @FXML
    Label messageLabel;

    @FXML
    public void handleLogin() {
        String tenDangNhap = userName.getText();
        String matKhau = password.getText();
        if (tenDangNhap.isEmpty() || matKhau.isEmpty()) {
            messageLabel.setText("Không được để trống Tên đăng nhập và Mật khẩu.");
            return; // can co return neu khong chuong trinh loi van se chay tiep xuong duoi chu khong dung lai
        }
        if (tenDangNhap.equals("Sang") && matKhau.equals("1234a")) { // Kiem tra xem co dung tenDangNhap va matKhau cua tai khoan khong; tam thoi thi de v nao ket noi server sau
            messageLabel.setText("Đăng nhập thành công.");
        } else {
            messageLabel.setText("Tên đăng nhập hoặc Mật khẩu không trùng khớp.");
        }
    }

    @FXML
    private void handleRegister() {
        messageLabel.setText("Chuyển sang đăng kí.");
    }
}
