package client.controller;

import client.AppContext;
import client.ClientSocket;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import server.controller.AuthenticationController;
import server.exception.AuthenticationException;
import shared.dto.UserDTO;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class RegisterControllerMoi implements Initializable {

    @FXML
    private Button buttonLogin;
    @FXML
    private Button buttonRegister;
    @FXML
    private Button buttonBidder;
    @FXML
    private Button buttonSeller;
    @FXML
    private Button buttonAdmin;
    @FXML
    private TextField loginName;
    @FXML
    private PasswordField password1;
    @FXML
    private PasswordField password2;
    @FXML
    private VBox adminKeyBox;
    @FXML
    private PasswordField adminKeyField;
    @FXML
    private Label messageLabel;

    // Vai trò đang được chọn — mặc định là BIDDER
    private String selectedRole = "BIDDER";

    // Mã bí mật để đăng ký Admin
    private static final String ADMIN_SECRET = "ADMIN2024";

    private AuthenticationController authController = AppContext.getAuthController();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Mặc định highlight Bidder khi mở màn hình
        setActiveRole("BIDDER");
    }

    @FXML
    private void handleSelectBidder() {
        setActiveRole("BIDDER");
    }

    @FXML
    private void handleSelectSeller() {
        setActiveRole("SELLER");
    }

    @FXML
    private void handleSelectAdmin() {
        setActiveRole("ADMIN");
    }

    /**
     * Cập nhật giao diện khi chọn vai trò
     * - Đổi màu nút được chọn
     * - Hiện/ẩn ô nhập mã Admin
     */
    private void setActiveRole(String role) {
        selectedRole = role;

        // Reset tất cả nút về trạng thái chưa chọn
        String defaultStyle = "-fx-background-color: transparent;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: #3b82f6;" +
                "-fx-border-radius: 10;";
        String activeStyle = "-fx-background-color: #1f2937;" +
                "-fx-background-radius: 10;" +
                "-fx-border-color: #3b82f6;" +
                "-fx-border-radius: 10;";

        buttonBidder.setStyle(defaultStyle);
        buttonSeller.setStyle(defaultStyle);
        buttonAdmin.setStyle(defaultStyle);

        buttonBidder.setTextFill(javafx.scene.paint.Color.web("#6b7280"));
        buttonSeller.setTextFill(javafx.scene.paint.Color.web("#6b7280"));
        buttonAdmin.setTextFill(javafx.scene.paint.Color.web("#6b7280"));

        // Highlight nút đang chọn
        switch (role) {
            case "BIDDER" -> {
                buttonBidder.setStyle(activeStyle);
                buttonBidder.setTextFill(javafx.scene.paint.Color.web("#3b82f6"));
            }
            case "SELLER" -> {
                buttonSeller.setStyle(activeStyle);
                buttonSeller.setTextFill(javafx.scene.paint.Color.web("#3b82f6"));
            }
            case "ADMIN" -> {
                buttonAdmin.setStyle(activeStyle);
                buttonAdmin.setTextFill(javafx.scene.paint.Color.web("#3b82f6"));
            }
        }

        // Hiện ô mã Admin nếu chọn Admin, ẩn nếu không
        boolean isAdmin = role.equals("ADMIN");
        adminKeyBox.setVisible(isAdmin);
        adminKeyBox.setManaged(isAdmin);
    }

    private String validate(String tenDangNhap, String matKhau, String xacNhan) {
        if (tenDangNhap.isEmpty())
            return "Vui lòng nhập tên đăng nhập.";

        if (tenDangNhap.length() < 4)
            return "Tên đăng nhập phải có ít nhất 4 ký tự.";

        if (tenDangNhap.contains(" "))
            return "Tên đăng nhập không được chứa dấu cách.";

        if (matKhau.isEmpty())
            return "Vui lòng nhập mật khẩu.";

        if (matKhau.length() < 6)
            return "Mật khẩu phải có ít nhất 6 ký tự.";

        if (!matKhau.equals(xacNhan))
            return "Mật khẩu xác nhận không khớp.";

        // Kiểm tra mã Admin nếu chọn vai trò Admin
        if (selectedRole.equals("ADMIN")) {
            String adminKey = adminKeyField.getText().trim();
            if (adminKey.isEmpty())
                return "Vui lòng nhập mã xác nhận Admin.";
            if (!adminKey.equals(ADMIN_SECRET))
                return "Mã xác nhận Admin không đúng.";
        }

        return null;
    }
    @FXML
    private void handleRegister() {
        String tenDangNhap = loginName.getText().trim();   // ← loginName
        String matKhau = password1.getText();              // ← password1
        String xacNhan = password2.getText();

        String error = validate(tenDangNhap, matKhau, xacNhan);
        if (error != null) {
            showError(error);
            return;
        }

        buttonRegister.setDisable(true);                   // ← buttonRegister
        messageLabel.setText("Đang đăng ký...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                authController.register(tenDangNhap, matKhau, selectedRole);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            showSuccess("Đăng ký thành công! Đang chuyển về đăng nhập...");
            buttonRegister.setDisable(false);              // ← buttonRegister

            // Chuyển màn hình trên FX thread
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/LoginViewMoi.fxml"));
                Parent root = loader.load();
                LoginControllerMoi loginController = loader.getController();
                loginController.setAuthenticationController(authController);

                Stage stage = (Stage) messageLabel.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException ex) {
                showError("Lỗi: Không thể tải màn hình đăng nhập.");
            }
        });

        task.setOnFailed(e -> {
            showError("Lỗi: " + task.getException().getMessage());
            buttonRegister.setDisable(false);              // ← buttonRegister
        });

        new Thread(task).start();
    }

    @FXML
    private void handleLogin() {
        switchScene("/fxml/LoginViewMoi.fxml");
    }

    // Click tab Moi ở trên cùng
    @FXML
    private void handleSwitchToLogin() {
        switchScene("/fxml/LoginViewMoi.fxml");
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
            Stage stage = (Stage) messageLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Lỗi: Không thể tải màn hình");
        }
    }
}