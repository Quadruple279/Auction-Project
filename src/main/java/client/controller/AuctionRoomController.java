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

import shared.dto.AuctionDTO;
import shared.protocol.AuctionEvent;
import shared.protocol.AuctionObserver;
import shared.protocol.MessageType;

import java.io.IOException;
import java.net.URL;

import java.util.Locale;
import java.util.ResourceBundle;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.NumberAxis;

/**
 * Phiên đấu giá — mọi thao tác với server đều qua ClientSocket,
 * KHÔNG khởi tạo bất kỳ server.controller.* nào trực tiếp.
 */
public class AuctionRoomController implements Initializable, AuctionObserver {

    // ─── FXML fields ─────────────────────────────────────────────────────────
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
    @FXML private LineChart<String, Number> priceChart;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;
    @FXML private TextField maxAutoBidField;
    @FXML private TextField incrementField;
    @FXML private Button autoBidButton;
    @FXML private Label autoBidStatusLabel;

    private XYChart.Series<String, Number> priceSeries;
    private int bidCounter = 0;
    private boolean autoBidEnabled = false;
    private long maxAutoBidPrice = 0;
    private long autoBidIncrement = 0;
    private String currentUsername;
    private javafx.animation.Timeline autoBidTimeline;

    private javafx.animation.Timeline countdownTimer;

    // Phiên đấu giá hiện tại (dữ liệu ban đầu lấy từ AuctionManager)
    private AuctionDTO currentAuction;

    // ─── Khởi tạo ────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupBidHistoryList();
        setupPriceChart();

        // Chỉ cho phép nhập số, tự động format 1,000,000
        bidAmountField.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (!newText.matches("[\\d,]*")) return null;

            String digits = newText.replace(",", "");
            if (digits.isEmpty()) {
                change.setText("");
                change.setRange(0, change.getControlText().length());
                change.setCaretPosition(0);
                change.setAnchor(0);
                return change;
            }
            if (digits.length() > 15) return null;
//hhhh
            try {
                long value = Long.parseLong(digits);
                String formatted = String.format(Locale.US, "%,d", value);

                int oldCaretPos = change.getControlNewText().length();
                int digitsBeforeCaret = change.getControlNewText()
                        .substring(0, Math.min(oldCaretPos, newText.length()))
                        .replace(",", "").length();

                int newCaretPos = 0, digitCount = 0;
                for (int i = 0; i < formatted.length(); i++) {
                    if (Character.isDigit(formatted.charAt(i))) digitCount++;
                    if (digitCount == digitsBeforeCaret) { newCaretPos = i + 1; break; }
                }
                if (digitsBeforeCaret == 0) newCaretPos = 0;

                change.setText(formatted);
                change.setRange(0, change.getControlText().length());
                change.setCaretPosition(newCaretPos);
                change.setAnchor(newCaretPos);
            } catch (NumberFormatException e) {
                return null;
            }
            return change;
        }));
    }

    /**
     * Được gọi từ AuctionListController sau khi load FXML.
     * Không nhận AuthenticationController — mọi thứ qua ClientSocket.
     */
    public void setAuction(AuctionDTO auctionDTO) {
        this.currentAuction = auctionDTO;

        hienThiThongTin();
        loadLichSuDauGia();
        startCountdown();
        log("Đã vào phiên: " + auctionDTO.getAuctionId());

        // Đăng ký nhận thông báo realtime
        ClientSocket.getInstance().addObserver(this);
        ClientSocket.getInstance().subscribe(auctionDTO.getAuctionId());
    }

    // ─── Hiển thị thông tin ───────────────────────────────────────────────────

    private void hienThiThongTin() {
        auctionIdLabel.setText("Phiên: " + currentAuction.getAuctionId());
        itemNameLabel.setText(currentAuction.getItemName());
        descriptionLabel.setText(currentAuction.getDescription());
        basePriceLabel.setText(String.format("%,.0f đ", currentAuction.getPrice()));
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
        bidHistoryList.getItems().add("Chưa có ai đặt giá.");
        // Lịch sử sẽ được cập nhật realtime khi nhận BID_PLACED event
    }


    // ─── Countdown timer ─────────────────────────────────────────────────────

    private void startCountdown() {
        if (countdownTimer != null) countdownTimer.stop();

        countdownTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(1),
                        e -> {
                            java.time.LocalDateTime now = java.time.LocalDateTime.now();
                            java.time.LocalDateTime endTime = java.time.LocalDateTime.parse(currentAuction.getEndTime());
                            java.time.Duration timeLeft = java.time.Duration.between(now, endTime);

                            if (timeLeft.isNegative() || timeLeft.isZero()) {
                                timeLabel.setText("00:00:00");
                                countdownTimer.stop();
                                placeBidButton.setDisable(true);
                                statusLabel.setText("● ĐÃ KẾT THÚC");
                                statusLabel.setTextFill(javafx.scene.paint.Color.web("#ff6b6b"));
                                log("Phiên đấu giá đã kết thúc!");
                            } else {
                                long h = timeLeft.toHours();
                                long m = timeLeft.toMinutesPart();
                                long s = timeLeft.toSecondsPart();
                                timeLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
                            }
                        }
                )
        );
        countdownTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        countdownTimer.play();
    }

    // ─── Đặt giá — gửi qua socket, KHÔNG gọi server controller trực tiếp ────

    @FXML
    private void handlePlaceBid() {
        String input = bidAmountField.getText().trim();
        if (input.isEmpty()) {
            log("Vui lòng nhập số tiền muốn đặt.");
            return;
        }

        long soTien;
        try {
            soTien = Long.parseLong(input.replace(",", ""));
        } catch (NumberFormatException e) {
            log("Số tiền không hợp lệ. Vui lòng nhập lại.");
            return;
        }

        // Đặt response listener để xử lý phản hồi từ server
        ClientSocket.getInstance().setResponseListener(msg -> {
            Platform.runLater(() -> {
                log("Lỗi: " + msg.getOrDefault("reason", "Không thể đặt giá"));
            });
        });

        // Gửi lệnh BID qua socket — ClientHandler trên server xử lý và broadcast
        ClientSocket.getInstance().sendBid(currentAuction.getAuctionId(), soTien);
    }

    // ─── AuctionObserver — nhận update realtime từ server ────────────────────

    @Override
    public void onAuctionEvent(AuctionEvent event) {
        if (!event.getAuctionId().equals(currentAuction.getAuctionId())) return;

        Platform.runLater(() -> {
            switch (event.getType()) {
                case BID_PLACED -> {
                    currentPriceLabel.setText(
                            String.format("%,.0f ₫", event.getCurrentPrice()));
                    leaderLabel.setText(event.getLeadingBidder());

                    // Thêm dòng này để cập nhật lịch sử bid realtime
                    String dong = event.getBidderName()
                            + " | " + String.format("%,.0f ₫", event.getBidAmount())
                            + " | " + java.time.LocalTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                    bidHistoryList.getItems().add(0, dong);

                    log("Bid mới! " + event.getBidderName()
                            + " đặt " + String.format("%,.0f ₫", event.getBidAmount()));
                    updatePriceChart(event.getBidAmount());

                }
                case BID_REJECTED -> log("Bid bị từ chối của " + event.getBidderName());
                case AUCTION_ENDED -> {
                    placeBidButton.setDisable(true);
                    disableAutoBid();
                    statusLabel.setText("● ĐÃ KẾT THÚC");
                    statusLabel.setTextFill(javafx.scene.paint.Color.web("#ff6b6b"));
                    log("Phiên đấu giá đã kết thúc! Người thắng: " + event.getLeadingBidder());
                }
                case TIME_EXTENDED -> {
                    // Cập nhật endTime nội bộ để countdown timer tự khớp
                    if (event.getNewEndTimeEpoch() > 0) {
                        java.time.LocalDateTime newEnd = java.time.LocalDateTime
                                .ofEpochSecond(event.getNewEndTimeEpoch(), 0,
                                        java.time.ZoneOffset.of("+07:00"));
                        currentAuction = new shared.dto.AuctionDTO(
                                currentAuction.getAuctionId(),
                                currentAuction.getItemName(),
                                currentAuction.getDescription(),
                                currentAuction.getPrice(),
                                currentAuction.getCurrentPrice(),
                                currentAuction.getLeadingBidder(),
                                currentAuction.getOwner(),
                                currentAuction.isFinished(),
                                currentAuction.getStatus(),
                                newEnd.toString()
                        );
                    }
                    log("⚠ Anti-snipe! Phiên được gia hạn thêm 10 giây.");
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("Gia hạn phiên");
                    alert.setHeaderText(null);
                    alert.setContentText("⚠ Có người đặt giá vào phút cuối!\nPhiên được gia hạn thêm 10 giây.");
                    alert.show();
                }
            }
        });
    }

    // ─── Nút Quay lại ────────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        if (countdownTimer != null) countdownTimer.stop();
        disableAutoBid();

        ClientSocket.getInstance().removeObserver(this);
        ClientSocket.getInstance().unsubscribe(currentAuction.getAuctionId());

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AuctionView.fxml")
            );
            Parent root = loader.load();
            Stage stage = (Stage) buttonBack.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            log("Lỗi: Không thể quay về Dashboard");
        }
    }

    // ─── Tiện ích ─────────────────────────────────────────────────────────────

    private void setupPriceChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá");
        priceChart.getData().add(priceSeries);
        priceChart.setLegendVisible(false);
        priceChart.setAnimated(false);
        priceChart.setCreateSymbols(true);
        priceChart.lookup(".chart-plot-background")  ;
        // Style đường kẻ xanh lá
        priceChart.setStyle("-fx-background-color: transparent;");
    }

    private void updatePriceChart(double price) {
        bidCounter++;
        String label = String.valueOf(bidCounter);
        priceSeries.getData().add(new XYChart.Data<>(label, price));
        // Giữ tối đa 20 điểm để chart không quá dày
        if (priceSeries.getData().size() > 20) {
            priceSeries.getData().remove(0);
        }
    }

    private void log(String msg) {
        if (console != null) console.appendText(msg + "\n");
    }

    private void setupBidHistoryList() {
        bidHistoryList.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                    setStyle("-fx-background-color: #1d1d1d;");
                    return;
                }

                HBox hbox = new HBox();
                VBox vbox = new VBox();
                vbox.setSpacing(3);

                Label tenLabel      = new Label();
                Label thoiGianLabel = new Label();
                Label tienLabel     = new Label();

                String[] parts = item.split("\\|");
                if (parts.length >= 2) {
                    tenLabel.setText(parts[0]);
                    tienLabel.setText(parts[1]);
                    if (parts.length == 3) thoiGianLabel.setText(parts[2]);
                }

                tenLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
                thoiGianLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

                if (getIndex() == 0) {
                    tienLabel.setStyle(
                            "-fx-text-fill: #4caf50; -fx-font-size: 13px; -fx-font-weight: bold;");
                    hbox.setStyle("-fx-background-color: #1d1d1d;" +
                            "-fx-padding: 8 10 8 12;" +
                            "-fx-border-color: #4caf50 transparent transparent transparent;" +
                            "-fx-border-width: 0 0 0 2;");
                } else {
                    tienLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 13px;");
                    hbox.setStyle("-fx-background-color: #1d1d1d; -fx-padding: 8 10 8 12;");
                }

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
    @FXML
    private void handleEnableAutoBid() {
        if (autoBidEnabled) {
            disableAutoBid();
            return;
        }

        String maxText = maxAutoBidField.getText().replace(",", "").trim();
        String incText = incrementField.getText().replace(",", "").trim();

        if (maxText.isEmpty() || incText.isEmpty()) {
            autoBidStatusLabel.setText("⚠ Nhập đầy đủ giá tối đa và bước tăng.");
            autoBidStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
            return;
        }

        try {
            maxAutoBidPrice = Long.parseLong(maxText);
            autoBidIncrement = Long.parseLong(incText);
        } catch (NumberFormatException e) {
            autoBidStatusLabel.setText("⚠ Giá trị không hợp lệ.");
            autoBidStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
            return;
        }

        long currentPrice = parseCurrentPrice();

        if (maxAutoBidPrice <= currentPrice) {
            autoBidStatusLabel.setText("⚠ Giá tối đa phải lớn hơn giá hiện tại.");
            autoBidStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
            return;
        }
        if (autoBidIncrement <= 0) {
            autoBidStatusLabel.setText("⚠ Bước tăng phải lớn hơn 0.");
            autoBidStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
            return;
        }

        autoBidEnabled = true;
        autoBidButton.setText("Tắt Auto-Bid");
        autoBidButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold;");
        maxAutoBidField.setDisable(true);
        incrementField.setDisable(true);
        autoBidStatusLabel.setText("⚡ Auto-Bid đang chạy | Tối đa: " + formatPrice(maxAutoBidPrice));
        autoBidStatusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11px;");

        autoBidTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> runAutoBidLogic())
        );
        autoBidTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        autoBidTimeline.play();
    }

    private void runAutoBidLogic() {
        if (!autoBidEnabled) return;

        // Nếu mình đang dẫn đầu thì không đặt thêm
        if (currentUsername != null && currentUsername.equals(leaderLabel.getText())) return;

        long currentPrice = parseCurrentPrice();
        long nextBid = currentPrice + autoBidIncrement;

        if (nextBid <= maxAutoBidPrice) {
            bidAmountField.setText(String.valueOf(nextBid));
            handlePlaceBid();
            autoBidStatusLabel.setText("⚡ Vừa đặt: " + formatPrice(nextBid)
                    + " | Tối đa: " + formatPrice(maxAutoBidPrice));
        } else {
            autoBidStatusLabel.setText("🔴 Đã đạt giới hạn tối đa: " + formatPrice(maxAutoBidPrice));
            autoBidStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
            disableAutoBid();
        }
    }

    private void disableAutoBid() {
        autoBidEnabled = false;
        if (autoBidTimeline != null) autoBidTimeline.stop();
        autoBidButton.setText("Bật Auto-Bid");
        autoBidButton.setStyle("-fx-background-color: #f0a500; -fx-text-fill: black; -fx-font-weight: bold;");
        maxAutoBidField.setDisable(false);
        incrementField.setDisable(false);
        if (!autoBidStatusLabel.getText().contains("Đã đạt")) {
            autoBidStatusLabel.setText("Auto-Bid đã tắt.");
            autoBidStatusLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
        }
    }

// ─── Helpers ──────────────────────────────────────────────────────────────

    private long parseCurrentPrice() {
        try {
            return Long.parseLong(currentPriceLabel.getText().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String formatPrice(long price) {
        return String.format(Locale.US, "%,d ₫", price);
    }
    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }
}