package client.controller;

import client.ClientSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import shared.dto.AuctionDTO;
import shared.dto.UserDTO;
import shared.protocol.AuctionEvent;
import shared.protocol.AuctionObserver;
import shared.protocol.MessageType;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable, AuctionObserver {

    @FXML private TableView<AuctionDTO> tableView;
    @FXML private TableColumn<AuctionDTO, String> auction;
    @FXML private TableColumn<AuctionDTO, String> itemName;
    @FXML private TableColumn<AuctionDTO, String> description;
    @FXML private TableColumn<AuctionDTO, Double> price;
    @FXML private TableColumn<AuctionDTO, Double> highestBid;
    @FXML private TableColumn<AuctionDTO, String> owner;
    @FXML private TextArea console;
    @FXML private MenuItem disconnect;
    @FXML private Button back;
    @FXML private MenuItem openProfile;
    @FXML private MenuItem openSellerView;

    private UserDTO currentUser;

    private ObservableList<AuctionDTO> danhSach = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setUpCotBang();
        loadDuLieu();

        // Đăng ký nhận AuctionEvent để cập nhật bảng realtime
        ClientSocket.getInstance().addObserver(this);

        // Nhấn đôi vào hàng → vào phòng đấu giá
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                AuctionDTO selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openAuctionRoom(selected);
                }
            }
        });
    }

    // ─── Mở phòng đấu giá ────────────────────────────────────────────────────

    private void openAuctionRoom(AuctionDTO auction) {
        System.out.println("[DEBUG] openAuctionRoom() được gọi: " + auction.getAuctionId());
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AuctionRoomView.fxml")
            );
            Parent root = loader.load();

            AuctionRoomController roomController = loader.getController();
            roomController.setAuction(auction);
            roomController.setCurrentUsername(currentUser != null ? currentUser.getName() : "Khách");
            roomController.setCurrentUser(currentUser);


            Scene scene = tableView.getScene();
            scene.setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
            log("Lỗi: Không thể mở phòng đấu giá: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            log("Lỗi: " + e.getMessage());
        }
    }

    // ─── Setup bảng ──────────────────────────────────────────────────────────

    public void setUpCotBang() {
        auction.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        itemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        description.setCellValueFactory(new PropertyValueFactory<>("description"));
        owner.setCellValueFactory(new PropertyValueFactory<>("leadingBidder"));

        price.setCellValueFactory(new PropertyValueFactory<>("price"));
        price.setCellFactory(col -> new TableCell<AuctionDTO, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("%,.0f ₫", item));
            }
        });

        highestBid.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        highestBid.setCellFactory(col -> new TableCell<AuctionDTO, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("%,.0f ₫", item));
            }
        });

        tableView.setItems(danhSach);
    }

    public void loadDuLieu() {
        log("Đang tải dữ liệu phiên đấu giá...");
        ClientSocket.getInstance().setResponseListener(msg -> {
            if (msg.getType() == MessageType.AUCTION_LIST) {
                try {
                    String jsonData = msg.get("data");
                    // Deserialize chuỗi JSON → List<AuctionDTO>
                    // TypeReference cần thiết vì Java xóa generic type lúc runtime
                    ObjectMapper mapper = new ObjectMapper();
                    List<AuctionDTO> list = mapper.readValue(
                            jsonData,
                            new com.fasterxml.jackson.core.type.TypeReference<List<AuctionDTO>>() {}
                    );

                    // Cập nhật UI phải chạy trên JavaFX Thread
                    Platform.runLater(() -> {
                        danhSach.setAll(list);
                        log("Đã tải " + list.size() + " phiên đấu giá.");
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> log("Lỗi parse dữ liệu: " + e.getMessage()));
                }
            }
        });
        ClientSocket.getInstance().sendGetAuctions();
    }

    // ─── AuctionObserver ─────────────────────────────────────────────────────

    @Override
    public void onAuctionEvent(AuctionEvent event) {
        Platform.runLater(() -> {
            switch (event.getType()) {
                case BID_PLACED:
                    log(event.getBidderName() + " vừa đặt "
                            + String.format("%,.0f ₫", event.getBidAmount())
                            + " cho phiên " + event.getAuctionId());
                    // Cập nhật currentPrice trong danhSach để bảng hiển thị đúng
                    for (AuctionDTO dto : danhSach) {
                        if (dto.getAuctionId().equals(event.getAuctionId())) {
                            dto.setCurrentPrice(event.getCurrentPrice());
                            break;
                        }
                    }
                    tableView.refresh();
                    break;
                case BID_REJECTED:
                    log(event.getBidderName() + " đặt giá không hợp lệ (phiên "
                            + event.getAuctionId() + ")");
                    break;
                case AUCTION_ENDED:
                    log(event.getAuctionId() + " đã đóng! Người thắng: "
                            + event.getLeadingBidder());
                    break;
                case NEW_AUCTION:
                    log("Có phiên đấu giá mới! Đang cập nhật danh sách...");
                    loadDuLieu();
                    break;
            }
        });
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    @FXML
    public void disconnect(ActionEvent actionEvent) {
        ClientSocket.getInstance().removeObserver(this);
        ClientSocket.getInstance().disconnect();
        log("Đã ngắt kết nối.");
        switchScene("/fxml/LoginViewMoi.fxml");
    }

    @FXML
    public void back(ActionEvent actionEvent) {
        ClientSocket.getInstance().removeObserver(this);
        switchScene("/fxml/LoginViewMoi.fxml");
    }

    // ─── Tiện ích ─────────────────────────────────────────────────────────────

    public void log(String msg) {
        if (console != null) {
            console.appendText(msg + "\n");
        }
    }

    public void setCurrentUser(UserDTO user) {
        this.currentUser = user;
    }

    @FXML
    public void openProfile(ActionEvent actionEvent) {
        if (currentUser == null) {
            log("Lỗi: Không có thông tin user.");
            return;
        }


        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/ProfileView.fxml")
            );
            Parent root = loader.load();

            ProfileController profileController = loader.getController();
            profileController.setCurrentUser(currentUser);

            Stage stage = (Stage) tableView.getScene().getWindow();
            ClientSocket.getInstance().removeObserver(this);
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            log("Lỗi: Không thể mở Profile Dashboard.");
        }
    }


    @FXML
    public void openSellerView(ActionEvent actionEvent) {
        if (currentUser == null) {
            log("Lỗi: Không có thông tin user.");
            return;
        }
        String role = currentUser.getRole();


        if (!"SELLER".equals(role)) {
            log("Bạn không có quyền truy cập Seller Dashboard.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/SellerView.fxml")
            );
            Parent root = loader.load();

            SellerController sellerController = loader.getController();
            sellerController.setCurrentUser(currentUser);

            Stage stage = (Stage) tableView.getScene().getWindow();
            ClientSocket.getInstance().removeObserver(this);
            stage.getScene().setRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
            log("Lỗi: Không thể mở Seller Dashboard.");
        }
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            log("Lỗi: Không thể tải màn hình");
        }
    }
}
