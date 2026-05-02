package client.controller;

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
import server.controller.AuthenticationController;
import server.model.Auction;
import server.model.AuctionEvent;
import server.model.AuctionManager;
import server.model.item.ItemFactory;
import server.model.observer.AuctionObserver;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class AuctionController implements Initializable, AuctionObserver {
    @FXML
    private TableView<Auction> tableView;
    @FXML
    private TableColumn<Auction, String> auction;
    @FXML
    private TableColumn<Auction, String> itemName;
    @FXML
    private TableColumn<Auction, String> description;
    @FXML
    private TableColumn<Auction, Double> price;
    @FXML
    private TableColumn<Auction, Double> highestBid;
    @FXML
    private TableColumn<Auction, String> owner;
    @FXML
    private TextArea console;
    @FXML
    private MenuItem disconnect;
    @FXML
    private Button back;

    private ObservableList<Auction> danhSach = FXCollections.observableArrayList();

    private AuthenticationController authenticationController;
    public void setAuthenticationController(AuthenticationController authenticationController ) {
        this.authenticationController = authenticationController;
        loadDuLieu();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setUpCotBang();

        // Xử lý việc nhấn vào hàng thì sẽ chuyển màn hình sang phòng đấu giá
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Auction selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openAuctionRoom(selected);
                }
            }
        });
    }

    private void openAuctionRoom(Auction auction) {
        System.out.println("[DEBUG] openAuctionRoom() được gọi: " + auction.getAuctionId());
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AuctionRoomView.fxml")
            );
            Parent root = loader.load();

            AuctionRoomController roomController = loader.getController();
            roomController.setAuction(auction, authenticationController);

            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            log("Lỗi: Không thể mở phòng đấu giá: " + e.getMessage());
        } catch (Exception e) {
            // Bắt thêm exception khác
            e.printStackTrace();
            System.out.println("[DEBUG] Lỗi khác: " + e.getMessage());
            log("Lỗi: " + e.getMessage());
        }
    }

    public void setUpCotBang() {
        auction.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        itemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        description.setCellValueFactory(new PropertyValueFactory<>("description"));
        owner.setCellValueFactory(new PropertyValueFactory<>("leadingBidder"));
        // Thêm format cho cột price và highestBid
        price.setCellValueFactory(new PropertyValueFactory<>("price")); // ← lấy dữ liệu
        price.setCellFactory(col -> new TableCell<Auction, Double>() {  // ← định dạng
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("%,.0f ₫", item));
            }
        });
        highestBid.setCellValueFactory(new PropertyValueFactory<>("currentPrice")); // ← lấy dữ liệu
        highestBid.setCellFactory(col -> new TableCell<Auction, Double>() {         // ← định dạng
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
        System.out.println("[DEBUG] loadDuLieu() được gọi");
        System.out.println("[DEBUG] Số phiên trong AuctionManager: "
                + AuctionManager.getInstance().getAuctionList().size());

        danhSach.setAll(AuctionManager.getInstance().getAuctionList());

        System.out.println("[DEBUG] Số phiên trong danhSach: " + danhSach.size());
        log("Đã tải " + danhSach.size() + " phiên đấu giá.");


    }

    public void log(String msg) {
        if (console != null) {
            console.appendText(msg + "\n");
        }
    }

    @FXML
    public void disconnect(ActionEvent actionEvent) {
        log("Đã ngắt kết nối.");
        switchScene("/fxml/LoginView.fxml");
    }

    @FXML
    public void back(ActionEvent actionEvent) {
        switchScene("/fxml/LoginView.fxml");
    }

    private void switchScene(String fxmlPath) { // Được sử dụng để chuyển đổi màn hình
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) tableView.getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            log("Loi: Khong the tai man hinh");
        }
    }

    @Override
    public void onAuctionEvent(AuctionEvent event) {
        Platform.runLater(() -> {
            switch (event.getType()) {
                case BID_PLACED:
                    log(event.getBidderName() + " vừa đặt " + event.getBidAmount() + " VNĐ cho phiên " + event.getAuctionId());
                    tableView.refresh(); // Ép bảng load lại số tiền mới nhất
                    break;
                case BID_REJECTED:
                    log(event.getBidderName() + " đặt giá không hợp lệ (mã " + event.getAuctionId() + ")");
                    break;
                case AUCTION_ENDED:
                    log(event.getAuctionId() + " đã đóng! Người thắng: " + event.getLeadingBidder());
                    break;
            }
        });
    }
}
