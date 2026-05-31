package client.controller;

import client.ClientSocket;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import shared.protocol.MessageType;

import java.io.IOException;

public class LoginController {

    @FXML private TextField userName;
    @FXML private PasswordField password;
    @FXML private Label messageLabel;

    // ─── Xử lý nút Đăng nhập ─────────────────────────────────────────────────

    @FXML
    public void handleLogin() {
        String tenDangNhap = userName.getText().trim();
        String matKhau     = password.getText().trim();

        if (tenDangNhap.isEmpty() || matKhau.isEmpty()) {
            messageLabel.setText("Không được để trống Tên đăng nhập và Mật khẩu.");
            return;
        }

        // 1. Kết nối tới server (nếu chưa kết nối)
        if (!ClientSocket.getInstance().connect()) {
            messageLabel.setText("Không thể kết nối tới server!");
            return;
        }

        // 2. Đăng ký callback nhận phản hồi LOGIN_SUCCESS / LOGIN_FAILED
        ClientSocket.ResponseListener[] loginRef = new ClientSocket.ResponseListener[1];
        loginRef[0] = msg -> {
            Platform.runLater(() -> {
                ClientSocket.getInstance().removeResponseListener(MessageType.LOGIN_SUCCESS, loginRef[0]);
                ClientSocket.getInstance().removeResponseListener(MessageType.LOGIN_FAILED, loginRef[0]);
                if (msg.getType() == MessageType.LOGIN_SUCCESS) {
                    messageLabel.setText("Đăng nhập thành công.");
                    openDashboard();
                } else {
                    // LOGIN_FAILED hoặc ERROR
                    String reason = msg.getOrDefault("reason", "Sai tài khoản hoặc mật khẩu.");
                    messageLabel.setText(reason);
                    // Ngắt kết nối để cho phép thử lại
                    ClientSocket.getInstance().disconnect();
                }
            });
        };
        ClientSocket.getInstance().addResponseListener(MessageType.LOGIN_SUCCESS, loginRef[0]);
        ClientSocket.getInstance().addResponseListener(MessageType.LOGIN_FAILED, loginRef[0]);

        // 3. Gửi lệnh LOGIN qua socket — ClientHandler trên server sẽ xử lý
        ClientSocket.getInstance().sendLogin(tenDangNhap, matKhau);
        messageLabel.setText("Đang kết nối...");
    }

    // ─── Mở Dashboard ────────────────────────────────────────────────────────

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AuctionView.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) messageLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("BidNow — Danh sách đấu giá");
            stage.show();

        } catch (IOException e) {
            messageLabel.setText("Lỗi: Không thể mở dashboard");
        }
    }

    // ─── Chuyển sang màn hình Đăng ký ────────────────────────────────────────

    @FXML
    private void handleRegister() {
        switchScene("/fxml/RegisterView.fxml");
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) messageLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Loi: Khong the tai man hinh");
        }
    }
}
