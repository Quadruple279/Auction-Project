package client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController  implements Initializable{
    @FXML
    private TextField loginName;
    @FXML
    private PasswordField password1;
    @FXML
    private PasswordField password2;
    @FXML
    private Label messageLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    private String validate(String loginName, String password1, String password2) {
        if (loginName.isEmpty() ) {
            return "Tên đăng nhập bị trống." ;
        }
        if (password1.isEmpty() || password2.isEmpty()) {
            return "Mật khẩu và Xác nhận không được để trống.";
        }
        if (!password1.equals(password2)) {
            return "Mật khẩu và Xác nhận không trùng khớp.";
        }
        return null;
    }

    @FXML
    private void handleRegister(){
        String tenDangNhap = loginName.getText().trim();
        String matKhau = password1.getText();
        String xacNhan = password2.getText();

        String error = validate(tenDangNhap, matKhau, xacNhan);
        if (error != null) {
            messageLabel.setText(error);
            return;
        }

        messageLabel.setText("Đăng ký thành công.");
        switchScene("/fxml/LoginView.fxml");
    }

    @FXML
    private void handleLogin() {
        switchScene("/fxml/LoginView.fxml");
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
