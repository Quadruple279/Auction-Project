package client.controller;

import client.ClientSocket;
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
import server.model.Auction;
import server.model.AuctionManager;
import shared.protocol.AuctionEvent;
import shared.protocol.AuctionObserver;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AuctionController implements Initializable, AuctionObserver {

    @FXML private TableView<Auction> tableView;
    @FXML private TableColumn<Auction, String> auction;
    @FXML private TableColumn<Auction, String> itemName;
    @FXML private TableColumn<Auction, String> description;
    @FXML private TableColumn<Auction, Double> price;
    @FXML private TableColumn<Auction, Double> highestBid;
    @FXML private TableColumn<Auction, String> owner;
    @FXML private TextArea console;
    @FXML private MenuItem disconnect;
    @FXML private Button back;

    private ObservableList<Auction> danhSach = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setUpCotBang();
        loadDuLieu();

        // Đăng ký nhận AuctionEvent để cập nhật bảng realtime
        ClientSocket.getInstance().addObserver(this);

        // Nhấn đôi vào hàng → vào phòng đấu giá
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Auction selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openAuctionRoom(selected);
                }
            }
        });
    }

    // ─── Mở phòng đấu giá ────────────────────────────────────────────────────

    private void openAuctionRoom(Auction auction) {
        System.out.println("[DEBUG] openAuctionRoom() được gọi: " + auction.getAuctionId());
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AuctionRoomView.fxml")
            );
            Parent root = loader.load();

            AuctionRoomController roomController = loader.getController();
            roomController.setAuction(auction);

            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

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
        price.setCellFactory(col -> new TableCell<Auction, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(String.format("%,.0f ₫", item));
            }
        });

        highestBid.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        highestBid.setCellFactory(col -> new TableCell<Auction, Double>() {
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
        danhSach.setAll(AuctionManager.getInstance().getAuctionList());
        log("Đã tải " + danhSach.size() + " phiên đấu giá.");
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
            }
        });
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    @FXML
    public void disconnect(ActionEvent actionEvent) {
        ClientSocket.getInstance().removeObserver(this);
        ClientSocket.getInstance().disconnect();
        log("Đã ngắt kết nối.");
        switchScene("/fxml/LoginView.fxml");
    }

    @FXML
    public void back(ActionEvent actionEvent) {
        ClientSocket.getInstance().removeObserver(this);
        switchScene("/fxml/LoginView.fxml");
    }

    // ─── Tiện ích ─────────────────────────────────────────────────────────────

    public void log(String msg) {
        if (console != null) {
            console.appendText(msg + "\n");
        }
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) tableView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            log("Lỗi: Không thể tải màn hình");
        }
    }
}
