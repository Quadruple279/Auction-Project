package client.controller;

import client.AppContext;
import client.ClientSocket;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.*;

import shared.dto.AuctionDTO;
import shared.dto.UserDTO;
import shared.protocol.AuctionEvent;
import shared.protocol.AuctionObserver;
import shared.protocol.MessageType;

import java.io.IOException;
import java.net.URL;

import java.util.List;
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
    @FXML private Label currentUserLabel;

    private XYChart.Series<String, Number> priceSeries;
    private int bidCounter = 0;
    private boolean autoBidEnabled = false;
    private String currentUsername;
    private UserDTO currentUser;

    private javafx.animation.Timeline countdownTimer;

    // Named listeners để có thể remove đúng cách
    private ClientSocket.ResponseListener bidHistoryListener;
    private ClientSocket.ResponseListener bidErrorListener;

    // Phiên đấu giá hiện tại
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
        maxAutoBidField.setTextFormatter(new TextFormatter<>(change -> {
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
        incrementField.setTextFormatter(new TextFormatter<>(change -> {
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
     */
    public void setAuction(AuctionDTO auctionDTO) {
        this.currentAuction = auctionDTO;
        // Khôi phục trạng thái autobid nếu đang active
        if (AppContext.isAutoBidActive(auctionDTO.getAuctionId())) {
            autoBidEnabled = true;
            autoBidButton.setText("Tắt Auto-Bid");
            autoBidButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold;");
            maxAutoBidField.setDisable(true);
            incrementField.setDisable(true);
            autoBidStatusLabel.setText("⚡  Auto-Bid đang chạy");
            autoBidStatusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11px;");
        }

        hienThiThongTin();
        loadLichSuDauGia();
        startCountdown();
        log("Đã vào phiên: " + auctionDTO.getAuctionId());

        // Đăng ký nhận thông báo realtime
        ClientSocket.getInstance().addObserver(this);
        ClientSocket.getInstance().subscribe(auctionDTO.getAuctionId());
    }

    public void setCurrentUser(UserDTO user) {
        this.currentUser = user;
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
        priceSeries.getData().clear();
        bidCounter = 0;

        // Xóa listener cũ trước khi add — tránh duplicate
        if (bidHistoryListener != null) {
            ClientSocket.getInstance().removeResponseListener(
                    MessageType.GET_BID_HISTORY_SUCCESS, bidHistoryListener);
        }

        bidHistoryListener = msg -> {
            if (msg.getType() == MessageType.GET_BID_HISTORY_SUCCESS) {
                try {
                    List<String> history = new ObjectMapper().readValue(
                            msg.get("data"), new TypeReference<List<String>>() {}
                    );
                    Platform.runLater(() -> {
                        bidHistoryList.getItems().clear();
                        if (history.isEmpty()) {
                            bidHistoryList.getItems().add("Chưa có ai đặt giá.");
                        } else {
                            for (String entry : history) {
                                bidHistoryList.getItems().add(0, entry);
                                String[] parts = entry.split("\\|");
                                if (parts.length >= 2) {
                                    try {
                                        String amountStr = parts[1].trim()
                                                .replace("₫", "")
                                                .replace(",", "")
                                                .trim();
                                        double amount = Double.parseDouble(amountStr);
                                        updatePriceChart(amount);
                                    } catch (NumberFormatException ignored) {
                                    }
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> log("Lỗi tải lịch sử: " + e.getMessage()));
                }
            }
        };

        ClientSocket.getInstance().addResponseListener(
                MessageType.GET_BID_HISTORY_SUCCESS, bidHistoryListener);
        ClientSocket.getInstance().sendGetBidHistory(currentAuction.getAuctionId());
    }

    // ─── Countdown timer ─────────────────────────────────────────────────────

    private void startCountdown() {
        if (countdownTimer != null) countdownTimer.stop();

        countdownTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(1),
                        e -> {
                            java.time.LocalDateTime now = java.time.LocalDateTime.now();
                            java.time.LocalDateTime endTime;
                            try {
                                endTime = java.time.LocalDateTime.parse(currentAuction.getEndTime());
                            } catch (Exception ex) {
                                timeLabel.setText("--:--:--");
                                return;
                            }
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

    // ─── Đặt giá ────────────────────────────────────────────────────────────

    @FXML
    private void handlePlaceBid() {
        String input = bidAmountField.getText().trim();
        if (input.isEmpty()) {
            log("Vui lòng nhập số tiền muốn đặt.");
            return;
        }
        if (currentUsername != null && currentUsername.equals(leaderLabel.getText())) {
            log("Bạn đang là người đặt giá cao nhất!");
            return;
        }

        long soTien;
        try {
            soTien = Long.parseLong(input.replace(",", ""));
        } catch (NumberFormatException e) {
            log("Số tiền không hợp lệ. Vui lòng nhập lại.");
            return;
        }
        if (soTien <= 0) {
            log("Số tiền đặt giá phải lớn hơn 0.");
            return;
        }
        if (soTien <= parseCurrentPrice()) {
            log("Số tiền phải cao hơn giá hiện tại: "
                    + String.format("%,.0f ₫", (double) parseCurrentPrice()));
            return;
        }

        // Đăng ký listener lỗi nếu chưa có
        if (bidErrorListener == null) {
            bidErrorListener = msg -> {
                if (msg.getType() == MessageType.ERROR) {
                    Platform.runLater(() -> log("Lỗi: " + msg.getOrDefault("reason", "Không thể đặt giá")));
                }
            };
            ClientSocket.getInstance().addResponseListener(MessageType.ERROR, bidErrorListener);
        }

        ClientSocket.getInstance().sendBid(currentAuction.getAuctionId(), soTien);
    }

    // ─── AuctionObserver ─────────────────────────────────────────────────────

    @Override
    public void onAuctionEvent(AuctionEvent event) {
        if (!event.getAuctionId().equals(currentAuction.getAuctionId())) return;

        Platform.runLater(() -> {
            switch (event.getType()) {
                case BID_PLACED -> {
                    currentPriceLabel.setText(
                            String.format("%,.0f ₫", event.getCurrentPrice()));
                    leaderLabel.setText(event.getLeadingBidder());

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
                    resetAutoBidUI();
                    statusLabel.setText("● ĐÃ KẾT THÚC");
                    statusLabel.setTextFill(javafx.scene.paint.Color.web("#ff6b6b"));
                    log("Phiên đấu giá đã kết thúc! Người thắng: " + event.getLeadingBidder());
                }
                case AUTO_BID_DISABLED -> Platform.runLater(this::resetAutoBidUI);
                case TIME_EXTENDED -> {
                    // Cập nhật endTime thay vì tạo lại AuctionDTO mới
                    if (event.getNewEndTimeEpoch() > 0) {
                        java.time.LocalDateTime newEnd = java.time.LocalDateTime
                                .ofEpochSecond(event.getNewEndTimeEpoch(), 0,
                                        java.time.ZoneOffset.UTC);
                        currentAuction.setEndTime(newEnd.toString());
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

        // Dọn sạch tất cả listener trước khi rời màn hình
        if (bidHistoryListener != null) {
            ClientSocket.getInstance().removeResponseListener(
                    MessageType.GET_BID_HISTORY_SUCCESS, bidHistoryListener);
        }
        if (bidErrorListener != null) {
            ClientSocket.getInstance().removeResponseListener(
                    MessageType.ERROR, bidErrorListener);
        }

        ClientSocket.getInstance().removeObserver(this);
        ClientSocket.getInstance().unsubscribe(currentAuction.getAuctionId());

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/AuctionView.fxml")
            );
            Parent root = loader.load();

            AuctionListController listController = loader.getController();
            listController.setCurrentUser(currentUser);

            Stage stage = (Stage) buttonBack.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            log("Lỗi: Không thể quay về Dashboard");
        }
    }

    // ─── Auto-Bid ────────────────────────────────────────────────────────────

    @FXML
    private void handleEnableAutoBid() {
        if (autoBidEnabled) {
            ClientSocket.getInstance().sendDisableAutoBid(currentAuction.getAuctionId());
            resetAutoBidUI();
            return;
        }
        String maxText = maxAutoBidField.getText().replace(",", "").trim();
        String incText = incrementField.getText().replace(",", "").trim();

        if (maxText.isEmpty() || incText.isEmpty()) {
            autoBidStatusLabel.setText("⚠ Nhập đầy đủ giá tối đa và bước tăng.");
            autoBidStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
            return;
        }

        long maxBid, increment;
        try {
            maxBid    = Long.parseLong(maxText);
            increment = Long.parseLong(incText);
        } catch (NumberFormatException e) {
            autoBidStatusLabel.setText("⚠ Giá trị không hợp lệ.");
            autoBidStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
            return;
        }

        long currentPrice = parseCurrentPrice();
        if (maxBid <= currentPrice) {
            autoBidStatusLabel.setText("⚠ Giá tối đa phải lớn hơn giá hiện tại.");
            autoBidStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
            return;
        }
        if (increment <= 0) {
            autoBidStatusLabel.setText("⚠ Bước tăng phải lớn hơn 0.");
            autoBidStatusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
            return;
        }

        // Gửi lên server để xử lý — server tự bid kể cả khi client thoát phòng
        ClientSocket.getInstance().sendEnableAutoBid(
                currentAuction.getAuctionId(), maxBid, increment);

        // Cập nhật UI
        autoBidEnabled = true;
        AppContext.setAutoBidActive(currentAuction.getAuctionId(), true); // thêm dòng này
        autoBidButton.setText("Tắt Auto-Bid");
        autoBidButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold;");
        maxAutoBidField.setDisable(true);
        incrementField.setDisable(true);
        autoBidStatusLabel.setText("⚡ Auto-Bid đang chạy | Tối đa: " + formatPrice(maxBid));
        autoBidStatusLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-size: 11px;");
    }

    /**
     * Chỉ reset giao diện Auto-Bid — không dừng logic phía server.
     */
    private void resetAutoBidUI() {
        autoBidEnabled = false;
        AppContext.setAutoBidActive(currentAuction.getAuctionId(), false); // thêm dòng này
        autoBidButton.setText("Bật Auto-Bid");
        autoBidButton.setStyle("-fx-background-color: #f0a500; -fx-text-fill: black; -fx-font-weight: bold;");
        maxAutoBidField.setDisable(false);
        incrementField.setDisable(false);
        autoBidStatusLabel.setText("Auto-Bid đã tắt.");
        autoBidStatusLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");
    }

    // ─── Tiện ích ─────────────────────────────────────────────────────────────

    private void setupPriceChart() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá");
        priceChart.getData().add(priceSeries);
        priceChart.setLegendVisible(false);
        priceChart.setAnimated(false);
        priceChart.setCreateSymbols(true);
        priceChart.setStyle("-fx-background-color: transparent;");
    }

    private void updatePriceChart(double price) {
        bidCounter++;
        String label = String.valueOf(bidCounter);
        priceSeries.getData().add(new XYChart.Data<>(label, price));
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
        if (currentUserLabel != null) {
            currentUserLabel.setText("Người dùng: " + (username != null ? username : "Khách"));
        }
    }
}