package client.controller;

import client.ClientSocket;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.*;

import server.controller.AuthenticationController;
import server.model.Auction;
import server.model.AuctionEvent;
import server.model.BidTransaction;
import server.model.observer.AuctionObserver;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;


public class AuctionRoomController implements Initializable, AuctionObserver {
    // Khai bao bien FXML
    @FXML private Button buttonBack;
    @FXML private Label auctionIdLabel;
    @FXML private Label statusLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label basePriceLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label leaderLabel;
    @FXML private Label timeLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button placeBidButton;
    @FXML private ListView<String> bidHistoryList;
    @FXML private TextArea console;

    private javafx.animation.Timeline countdownTimer; // Để đếm ngược thời gian cho phiên đấu giá

    private void startCountdown() {
        // Dừng timer cũ nếu đang chạy
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        countdownTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(1), // chạy mỗi 1 giây
                        e -> {
                            java.time.LocalDateTime now = java.time.LocalDateTime.now();
                            java.time.Duration timeLeft = java.time.Duration.between(
                                    now, currentAuction.getEndTime()
                            );

                            if (timeLeft.isNegative() || timeLeft.isZero()) {
                                // Hết giờ
                                timeLabel.setText("00:00:00");
                                countdownTimer.stop();

                                // Khóa nút đặt giá
                                placeBidButton.setDisable(true);
                                statusLabel.setText("● ĐÃ KẾT THÚC");
                                statusLabel.setTextFill(
                                        javafx.scene.paint.Color.web("#ff6b6b")
                                );
                                log("Phiên đấu giá đã kết thúc!");

                            } else {
                                // Tính giờ phút giây còn lại
                                long hours   = timeLeft.toHours();
                                long minutes = timeLeft.toMinutesPart();
                                long seconds = timeLeft.toSecondsPart();

                                timeLabel.setText(
                                        String.format("%02d:%02d:%02d", hours, minutes, seconds)
                                );
                            }
                        }
                )
        );

        // Chạy mãi cho đến khi dừng thủ công
        countdownTimer.setCycleCount(
                javafx.animation.Animation.INDEFINITE
        );
        countdownTimer.play();
    }

    // Phien dau gia hien tai
    private Auction currentAuction;

    // Controller server
    private server.controller.AuctionController auctionController;
    private AuthenticationController authenticationController;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupBidHistoryList(); // setup giao dien ListView
    }
    private void setupBidHistoryList() {
        bidHistoryList.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: #1d1d1d;");
                    return;
                }

                // Tạo layout
                HBox hbox = new HBox();
                VBox vbox = new VBox();
                vbox.setSpacing(3);

                Label tenLabel      = new Label();
                Label thoiGianLabel = new Label();
                Label tienLabel     = new Label();

                // Parse chuỗi "tên|tiền|giờ"
                String[] parts = item.split("\\|");
                if (parts.length >= 2) {
                    tenLabel.setText(parts[0]);
                    tienLabel.setText(parts[1]);
                    if (parts.length == 3) {
                        thoiGianLabel.setText(parts[2]);
                    }
                }

                tenLabel.setStyle(
                        "-fx-text-fill: white; -fx-font-size: 13px;");
                thoiGianLabel.setStyle(
                        "-fx-text-fill: #666666; -fx-font-size: 11px;");

                // Ô đầu tiên = người dẫn đầu → màu xanh lá
                if (getIndex() == 0) {
                    tienLabel.setStyle(
                            "-fx-text-fill: #4caf50; -fx-font-size: 13px; -fx-font-weight: bold;");
                    hbox.setStyle(
                            "-fx-background-color: #1d1d1d;" +
                                    "-fx-padding: 8 10 8 12;" +
                                    "-fx-border-color: #4caf50 transparent transparent transparent;" +
                                    "-fx-border-width: 0 0 0 2;");
                } else {
                    tienLabel.setStyle(
                            "-fx-text-fill: #aaaaaa; -fx-font-size: 13px;");
                    hbox.setStyle(
                            "-fx-background-color: #1d1d1d;" +
                                    "-fx-padding: 8 10 8 12;");
                }

                // Đẩy tiền sang phải
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                vbox.getChildren().addAll(tenLabel, thoiGianLabel);
                hbox.getChildren().addAll(vbox, spacer, tienLabel);

                setText(null);
                setGraphic(hbox);
                setStyle("-fx-background-color: #1d1d1d; -fx-padding: 2 0;");
            }
        });
    }

    public void setAuction(Auction auction, AuthenticationController auth) {
        this.currentAuction = auction;
        this.authenticationController = auth;
        this.auctionController = new server.controller.AuctionController(auth);

        hienThiThongTin();
        loadLichSuDauGia();
        startCountdown();
        log("Đã vào phiên: "+ auction.getAuctionId());
        // Đăng ký nhận thông báo realtime
        ClientSocket.getInstance().addObserver(this);
        ClientSocket.getInstance().subscribe(auction.getAuctionId());
    }

    private void hienThiThongTin() {
        auctionIdLabel.setText("Phiên: " + currentAuction.getAuctionId());
        itemNameLabel.setText(currentAuction.getItem().getName());
        descriptionLabel.setText(currentAuction.getItem().getDescription());

        basePriceLabel.setText(String.format("%,.0f đ", currentAuction.getItem().getBasePrice()));
        currentPriceLabel.setText(String.format("%,.0f đ", currentAuction.getCurrentPrice()));
        leaderLabel.setText(currentAuction.getLeadingBidder());

        if (currentAuction.isFinished()) {
            statusLabel.setText("● ĐÃ KẾT THÚC");
            statusLabel.setTextFill(javafx.scene.paint.Color.web("#ff6b6b"));
            placeBidButton.setDisable(true);
        } else {
            statusLabel.setText("● ĐANG MỞ");
            statusLabel.setTextFill(javafx.scene.paint.Color.web("#4caf50"));
        }
    }

    private void loadLichSuDauGia() {
        bidHistoryList.getItems().clear();

        for (BidTransaction bidTransaction : currentAuction.getTransactionHistory()) {
            // Dinh dang "ten | tien | gio"
            String dong = bidTransaction.getBidderName() + " | " + String.format("%,.0f ₫", bidTransaction.getBidAmount()) + " | " + bidTransaction.getBidTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            bidHistoryList.getItems().add(0, dong);
        }
        if (bidHistoryList.getItems().isEmpty()) {
            bidHistoryList.getItems().add("Chua co ai dat gia.||");
        }
    }

    @FXML
    private void handlePlaceBid() {
        String input = bidAmountField.getText().trim();

        if (input.isEmpty()) {
            log("Vui lòng nhập số tiền muốn đặt.");
            return;
        }

        double soTien;
        try {
            // Xóa dấu phẩy nếu người dùng gõ vào
            soTien = Double.parseDouble(input.replace(",", ""));
        } catch (NumberFormatException e) {
            log("Số tiền không hợp lệ. Vui lòng nhập lại.");
            return;
        }

        try {
            // Gọi AuctionController của server để đặt giá
            // AuctionController tự kiểm tra quyền và gọi auction.placeBid()
            auctionController.placeBid(currentAuction, soTien);

            // Cập nhật UI sau khi đặt giá thành công
            currentPriceLabel.setText(
                    String.format("%,.0f ₫", currentAuction.getCurrentPrice())
            );
            leaderLabel.setText(currentAuction.getLeadingBidder());
            AuctionRoomController.this.loadLichSuDauGia();
            bidAmountField.clear();
            log("Đặt giá thành công: " + String.format("%,.0f ₫", soTien));

        } catch (RuntimeException e) {
            // Bắt lỗi từ AuctionController — ví dụ chưa login, sai quyền
            log("Lỗi: " + e.getMessage());
        }
    }

    @Override
    public void onAuctionEvent(AuctionEvent event) {
        Platform.runLater(() -> {
            switch (event.getType()) {
                case BID_PLACED -> {
                    // Cập nhật giá và người dẫn đầu
                    currentPriceLabel.setText(
                            String.format("%,.0f ₫", event.getCurrentPrice())
                    );
                    leaderLabel.setText(event.getLeadingBidder());
                    AuctionRoomController.this.loadLichSuDauGia();
                    log("Bid mới! " + event.getBidderName()
                            + " đặt " + String.format("%,.0f ₫", event.getBidAmount()));
                }
                case BID_REJECTED -> {
                    log("Bid bị từ chối của " + event.getBidderName());
                }
                case AUCTION_ENDED -> {
                    // Khóa nút đặt giá khi phiên kết thúc
                    placeBidButton.setDisable(true);
                    statusLabel.setText("● ĐÃ KẾT THÚC");
                    statusLabel.setTextFill(javafx.scene.paint.Color.web("#ff6b6b"));
                    log("Phiên đấu giá đã kết thúc! Người thắng: "
                            + event.getLeadingBidder());
                }
            }
        });
    }

    @FXML
    private void handleBack() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        // Hủy đăng ký khi thoát
        ClientSocket.getInstance().removeObserver(this);
        ClientSocket.getInstance().unsubscribe(currentAuction.getAuctionId());
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AuctionView.fxml")
            );
            Parent root = loader.load();

            // Lấy controller Dashboard và truyền auth lại
            AuctionController dashboardController = loader.getController();
            dashboardController.setAuthenticationController(authenticationController);

            Stage stage = (Stage) buttonBack.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            log("Lỗi: Không thể quay về Dashboard");
        }
    }

    private void log(String msg) {
        if (console != null) {
            console.appendText(msg + "\n");
        }
    }

    private void switchScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) buttonBack.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            log("Lỗi: Không thể tải màn hình");
        }
    }
}

