package client.controller;

import client.ClientSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import server.controller.AuthenticationController;
import shared.dto.AuctionDTO;
import shared.protocol.MessageType;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SellerController implements Initializable {

    // ── FXML bindings ─────────────────────────────────────────
    @FXML private Button           buttonBack;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField        itemNameField;
    @FXML private TextArea         descriptionField;
    @FXML private TextField        basePriceField;
    @FXML private TextField        durationField;
    @FXML private TextField        extraInfo1Field;
    @FXML private TextField        extraInfo2Field;
    @FXML private Label            messageLabel;
    @FXML private Label            activeCountLabel;
    @FXML private Label            soldCountLabel;
    @FXML private Label            pendingCountLabel;
    @FXML private ListView<AuctionDTO> myAuctionList;

    private AuctionDTO selectedAuction;
    private List<AuctionDTO> myAuctions = new ArrayList<>();
    private boolean isEditing = false;

    // ── Dependencies ──────────────────────────────────────────
    private AuthenticationController authController;
    // ── Nhận authController từ LoginController ────────────────
    public void setAuthController(AuthenticationController auth) {
        this.authController = auth;
        if (authController != null) {
            loadMyAuctions();
            updateStats();
        }
    }

    // ════════════════════════════════════════════════════════════
    //  KHỞI TẠO
    // ════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Điền loại sản phẩm vào ComboBox
        typeComboBox.setItems(FXCollections.observableArrayList(
                "electronics", "art", "vehicle"
        ));
        typeComboBox.getSelectionModel().selectFirst();

        // Ẩn extraInfo2 mặc định — chỉ hiện khi chọn vehicle
        extraInfo2Field.setVisible(false);
        extraInfo2Field.setManaged(false);

        // Hiện/ẩn extraInfo2 khi đổi loại sản phẩm
        typeComboBox.setOnAction(e -> {
            boolean isVehicle = "vehicle".equals(typeComboBox.getValue());
            extraInfo2Field.setVisible(isVehicle);
            extraInfo2Field.setManaged(isVehicle);
        });
        setUpListView();
    }
    private void setUpListView() {
        myAuctionList.setOnMouseClicked(e -> {
            int index = myAuctionList.getSelectionModel().getSelectedIndex();

            if (index != -1) {
                selectedAuction = myAuctions.get(index);
                itemNameField.setText(selectedAuction.getItemName());        // thay getItem().getName()
                descriptionField.setText(selectedAuction.getDescription());  // thay getItem().getDescription()
                basePriceField.setText(String.valueOf(selectedAuction.getCurrentPrice()));
            }
        });
        myAuctionList.setCellFactory(list -> new ListCell<>() {
            private Button btnEdit = new Button("Sửa");
            private Button btnDelete = new Button("Xóa");
            private Label label = new Label();
            private HBox box = new HBox(10, label, btnEdit, btnDelete);

            {
                btnEdit.setOnAction(e -> {
                    AuctionDTO a = getItem();
                    if (a != null) {
                        selectedAuction = a;
                        isEditing = true;
                        itemNameField.setText(a.getItemName());
                        descriptionField.setText(a.getDescription());
                        basePriceField.setText(String.valueOf(a.getCurrentPrice()));
                    }
                });

                btnDelete.setOnAction(e -> {
                    AuctionDTO a = getItem();
                    if (a != null) {
                        ClientSocket.getInstance().setResponseListener(msg -> {
                            if (msg.getType() == MessageType.DELETE_AUCTION_SUCCESS) {
                                Platform.runLater(() -> loadMyAuctions());
                            } else if (msg.getType() == MessageType.ERROR) {
                                Platform.runLater(() -> showError("Lỗi xóa: " + msg.get("reason")));
                            }
                        });
                        ClientSocket.getInstance().sendDeleteAuction(a.getAuctionId());

                    }

                });
            }


            @Override
            protected void updateItem(AuctionDTO a, boolean empty) {
                super.updateItem(a, empty);

                if (empty || a == null) {
                    setGraphic(null);
                } else {
                    String text = String.format("[%s] %s - %,.0f ₫",
                            a.getAuctionId(),
                            a.getItemName(),          // thay getItem().getName()
                            a.getCurrentPrice());
                    label.setText(text);
                    setGraphic(box);
                }
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    //  XỬ LÝ ĐĂNG SẢN PHẨM
    // ════════════════════════════════════════════════════════════
    @FXML
    private void handleAddItem() {
        if (isEditing) {
            handleUpdate();
            return;
        }
        String loai     = typeComboBox.getValue();
        String ten      = itemNameField.getText().trim();
        String moTa     = descriptionField.getText().trim();
        String gia      = basePriceField.getText().trim();
        String thoiGian = durationField.getText().trim();
        String info1    = extraInfo1Field.getText().trim();
        String info2    = extraInfo2Field.getText().trim();

        // Validate form
        String error = validate(ten, gia, thoiGian, info1);
        if (error != null) {
            showError(error);
            return;
        }

        try {
            double giaKD = Double.parseDouble(gia.replace(",", ""));
            int    phut  = Integer.parseInt(thoiGian);

            // Gửi qua socket — server tự tạo item và auction
            String finalTen = ten;
            ClientSocket.getInstance().setResponseListener(msg -> {
                if (msg.getType() == MessageType.CREATE_AUCTION_SUCCESS) {
                    Platform.runLater(() -> {
                        showSuccess("Đăng sản phẩm \"" + finalTen + "\" thành công!");
                        clearForm();
                        loadMyAuctions();
                    });
                } else if (msg.getType() == MessageType.ERROR) {
                    Platform.runLater(() -> showError("Lỗi: " + msg.get("reason")));
                }
            });
            ClientSocket.getInstance().sendCreateAuction(loai, ten, moTa, giaKD, phut, info1, info2);

        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════
    //  VALIDATE
    // ════════════════════════════════════════════════════════════
    private String validate(String ten, String gia, String thoiGian, String info1) {
        if (ten.isEmpty())
            return "Vui lòng nhập tên sản phẩm.";

        if (gia.isEmpty())
            return "Vui lòng nhập giá khởi điểm.";

        try {
            double giaKD = Double.parseDouble(gia.replace(",", ""));
            if (giaKD <= 0) return "Giá khởi điểm phải lớn hơn 0.";
        } catch (NumberFormatException e) {
            return "Giá khởi điểm không hợp lệ.";
        }

        if (thoiGian.isEmpty())
            return "Vui lòng nhập thời gian đấu giá.";

        try {
            int phut = Integer.parseInt(thoiGian);
            if (phut <= 0) return "Thời gian phải lớn hơn 0 phút.";
        } catch (NumberFormatException e) {
            return "Thời gian không hợp lệ.";
        }

        if (info1.isEmpty())
            return "Vui lòng nhập thông tin thêm.";

        return null;
    }

    // ════════════════════════════════════════════════════════════
    //  LOAD DỮ LIỆU
    // ════════════════════════════════════════════════════════════
    private void loadMyAuctions() {
        String currentUser = authController.getCurrentUser().getName();
        ClientSocket.getInstance().setResponseListener(msg -> {
            if (msg.getType() == MessageType.AUCTION_LIST) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    List<AuctionDTO> list = mapper.readValue(
                            msg.get("data"),
                            new com.fasterxml.jackson.core.type.TypeReference<List<AuctionDTO>>() {});
                    Platform.runLater(() -> {
                        myAuctions.clear();
                        myAuctionList.getItems().clear();
                        for (AuctionDTO dto : list) {
                            if (currentUser.equals(dto.getOwner())) {
                                myAuctions.add(dto);
                                myAuctionList.getItems().add(dto);
                            }
                        }
                        updateStats();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showError("Lỗi tải dữ liệu: " + e.getMessage()));
                }
            }
        });
        ClientSocket.getInstance().sendGetAuctions();
    }


    private void updateStats() {
        long dangMo  = myAuctions.stream().filter(a -> !a.isFinished()).count();
        long ketThuc = myAuctions.stream().filter(AuctionDTO::isFinished).count();
        activeCountLabel.setText(String.valueOf(dangMo));
        soldCountLabel.setText(String.valueOf(ketThuc));
        pendingCountLabel.setText("0");
    }


    // ════════════════════════════════════════════════════════════
    //  ĐIỀU HƯỚNG
    // ════════════════════════════════════════════════════════════
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AuctionView.fxml")
            );
            Parent root = loader.load();

            AuctionListController dashboardController = loader.getController();
            dashboardController.setAuthenticationController(authController);

            Stage stage = (Stage) buttonBack.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            showError("Lỗi: Không thể quay về Dashboard.");
        }
    }

    // ════════════════════════════════════════════════════════════
    //  HELPER
    // ════════════════════════════════════════════════════════════
    private void clearForm() {
        itemNameField.clear();
        descriptionField.clear();
        basePriceField.clear();
        durationField.clear();
        extraInfo1Field.clear();
        extraInfo2Field.clear();
        typeComboBox.getSelectionModel().selectFirst();
        messageLabel.setText("");
    }

    private void showError(String msg) {
        messageLabel.setStyle("-fx-text-fill: #ff4444;");
        messageLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        messageLabel.setStyle("-fx-text-fill: #00cc66;");
        messageLabel.setText(msg);
    }

    @FXML
    private void handleUpdate() {
        if (selectedAuction == null) { showError("Chọn sản phẩm để sửa"); return; }
        try {
            String newName = itemNameField.getText();
            String newDesc = descriptionField.getText();
            double newPrice = Double.parseDouble(basePriceField.getText().replace(",", ""));
            ClientSocket.getInstance().setResponseListener(msg -> {
                if (msg.getType() == MessageType.UPDATE_AUCTION_SUCCESS) {
                    Platform.runLater(() -> {
                        showSuccess("Cập nhật thành công");
                        isEditing = false;
                        selectedAuction = null;
                        clearForm();
                        loadMyAuctions();
                    });
                } else if (msg.getType() == MessageType.ERROR) {
                    Platform.runLater(() -> showError("Lỗi: " + msg.get("reason")));
                }
            });
            ClientSocket.getInstance().sendUpdateAuction(
                    selectedAuction.getAuctionId(), newName, newDesc, newPrice);
        } catch (NumberFormatException e) {
            showError("Giá không hợp lệ");
        }
    }
}