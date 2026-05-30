package client.controller;

import client.ClientSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import shared.dto.UserDTO;

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
    private UserDTO currentUser;
    public void setCurrentUser(UserDTO user) {
        this.currentUser = user;
        if (currentUser != null) { loadMyAuctions(); updateStats(); }
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
        myAuctionList.setCellFactory(list -> new ListCell<>() {
            private final Button btnEdit   = new Button("Sửa");
            private final Button btnDelete = new Button("Xóa");
            private final Button btnFinish = new Button("Kết thúc");
            private final Button btnCancel = new Button("Hủy");
            private final Button btnPaid   = new Button("Đã thanh toán");
            private final Label  label     = new Label();
            private final HBox   box       = new HBox(8, label, btnEdit, btnDelete, btnFinish, btnCancel, btnPaid);

            {
                // Style các nút
                btnEdit.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 4;");
                btnDelete.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 4;");
                btnFinish.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 4;");
                btnCancel.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-background-radius: 4;");
                btnPaid.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-background-radius: 4;");
                label.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

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
                                javafx.application.Platform.runLater(() -> loadMyAuctions());
                            } else if (msg.getType() == MessageType.ERROR) {
                                javafx.application.Platform.runLater(() ->
                                        showError("Lỗi xóa: " + msg.get("reason")));
                            }
                        });
                        ClientSocket.getInstance().sendDeleteAuction(a.getAuctionId());
                    }
                });

                btnFinish.setOnAction(e -> {
                    AuctionDTO a = getItem();
                    if (a != null) {
                        ClientSocket.getInstance().setResponseListener(msg -> {
                            if (msg.getType() == MessageType.FINISH_AUCTION_SUCCESS) {
                                javafx.application.Platform.runLater(() -> {
                                    showSuccess("Đã kết thúc phiên!");
                                    loadMyAuctions();
                                });
                            } else if (msg.getType() == MessageType.ERROR) {
                                javafx.application.Platform.runLater(() ->
                                        showError("Lỗi: " + msg.get("reason")));
                            }
                        });
                        ClientSocket.getInstance().sendFinishAuction(a.getAuctionId());
                    }
                });

                btnCancel.setOnAction(e -> {
                    AuctionDTO a = getItem();
                    if (a != null) {
                        ClientSocket.getInstance().setResponseListener(msg -> {
                            if (msg.getType() == MessageType.CANCEL_AUCTION_SUCCESS) {
                                javafx.application.Platform.runLater(() -> {
                                    showSuccess("Đã hủy phiên!");
                                    loadMyAuctions();
                                });
                            } else if (msg.getType() == MessageType.ERROR) {
                                javafx.application.Platform.runLater(() ->
                                        showError("Lỗi: " + msg.get("reason")));
                            }
                        });
                        ClientSocket.getInstance().sendCancelAuction(a.getAuctionId());
                    }
                });

                btnPaid.setOnAction(e -> {
                    AuctionDTO a = getItem();
                    if (a != null) {
                        ClientSocket.getInstance().setResponseListener(msg -> {
                            if (msg.getType() == MessageType.MARK_PAID_SUCCESS) {
                                javafx.application.Platform.runLater(() -> {
                                    showSuccess("Đã xác nhận thanh toán!");
                                    loadMyAuctions();
                                });
                            } else if (msg.getType() == MessageType.ERROR) {
                                javafx.application.Platform.runLater(() ->
                                        showError("Lỗi: " + msg.get("reason")));
                            }
                        });
                        ClientSocket.getInstance().sendMarkPaid(a.getAuctionId());
                    }
                });
            }

            @Override
            protected void updateItem(AuctionDTO a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) {
                    setGraphic(null);
                    setText(null); // Nên thêm dòng này cho an toàn
                    // THÊM DÒNG NÀY: Giữ nền xám đen cho các ô rỗng bên dưới
                    setStyle("-fx-background-color: #1d1d1d; -fx-border-color: transparent;");
                    return;
                }

                String text = String.format("[%s] %s — %,.0f ₫ — %s",
                        a.getAuctionId(),
                        a.getItemName(),
                        a.getCurrentPrice(),
                        a.getStatus()
                );
                label.setText(text);

                boolean finished = a.isFinished();
                btnEdit.setDisable(finished);
                btnDelete.setDisable(finished);
                btnFinish.setDisable(finished);

                btnPaid.setVisible("FINISHED".equals(a.getStatus()));
                btnPaid.setManaged("FINISHED".equals(a.getStatus()));

                btnCancel.setVisible(!finished);
                btnCancel.setManaged(!finished);

                setStyle("-fx-background-color: #1d1d1d; -fx-border-color: #333333 transparent transparent transparent; -fx-padding: 5 0;");

                setGraphic(box);
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
        String currentUsername = currentUser.getName();
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
                            if (currentUsername.equals(dto.getOwner())) {
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
            dashboardController.setCurrentUser(currentUser);

            Stage stage = (Stage) buttonBack.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Lỗi: Không thể quay về Dashboard.");
        }
    }

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