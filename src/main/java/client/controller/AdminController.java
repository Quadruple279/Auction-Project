package client.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import server.controller.AuthenticationController;
import server.model.*;
import server.model.user.User;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminController implements Initializable {

    // ===== FXML – Sidebar & Topbar =====
    @FXML private Label adminNameLabel, pageTitle;
    @FXML private Button navDashboard, navUsers, navAuctions, navStats;

    // ===== FXML – Các trang chính =====
    @FXML private VBox pageDashboard, pageUsers, pageAuctions, pageStats;

    // ===== FXML – Bảng User (trang Users) =====
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> colUserId, colUserName, colUserRole;
    @FXML private TableColumn<User, Void> colUserAction;
    @FXML private TextField userSearchField;
    @FXML private ComboBox<String> userRoleFilter;
    @FXML private Label userCountLabel;   // Bổ sung

    // ===== FXML – Bảng Auction (trang Auctions) =====
    @FXML private TableView<Auction> auctionTable;
    @FXML private TableColumn<Auction, String> colAuctionId, colAuctionItem, colAuctionSeller, colAuctionPrice, colAuctionStatus;
    @FXML private TableColumn<Auction, Void> colAuctionAction;
    @FXML private TextField auctionSearchField;
    @FXML private ComboBox<String> auctionStatusFilter;
    @FXML private Label auctionCountLabel; // Bổ sung

    // ===== FXML – Dashboard preview =====
    @FXML private TableView<User> dashUserTable;
    @FXML private TableColumn<User, String> dashColUserName, dashColUserRole, dashColUserId;
    @FXML private TableView<Auction> dashAuctionTable;
    @FXML private TableColumn<Auction, String> dashColAuctionId, dashColAuctionItem, dashColAuctionStatus;

    // ===== FXML – Stats =====
    @FXML private Label statTotalUsers, statRunning, statFinished, statCanceled;
    @FXML private Label statBidder, statSeller, statTotalAuctions;
    @FXML private Label statSuccessRate;   // Tỉ lệ thành công
    @FXML private TextArea systemLog;      // Log hệ thống

    // ===== DATA =====
    private AuthenticationController authController;
    private final AuctionManager auctionManager = AuctionManager.getInstance();

    private ObservableList<User> userList = FXCollections.observableArrayList();
    private ObservableList<Auction> auctionList = FXCollections.observableArrayList();

    private FilteredList<User> filteredUsers;
    private FilteredList<Auction> filteredAuctions;

    // ===== INIT =====
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTables();
        setupFilters();
        // Mặc định hiển thị Dashboard
        showDashboard();
    }

    public void setAuthController(AuthenticationController auth) {
        this.authController = auth;
        adminNameLabel.setText(auth.getCurrentUser().getName());
        loadData();
    }

    // ===== LOAD DATA =====
    private void loadData() {
        userList.setAll(authController.getAllUsers());
        auctionList.setAll(auctionManager.getAuctionList());

        filteredUsers = new FilteredList<>(userList, p -> true);
        filteredAuctions = new FilteredList<>(auctionList, p -> true);

        userTable.setItems(filteredUsers);
        auctionTable.setItems(filteredAuctions);

        // Gán dữ liệu preview cho Dashboard
        dashUserTable.setItems(FXCollections.observableArrayList(userList.subList(0, Math.min(5, userList.size()))));
        dashAuctionTable.setItems(FXCollections.observableArrayList(
                auctionList.stream().filter(a -> !a.isFinished()).limit(5).toList()
        ));

        updateStats();
        updateCountLabels();
    }

    // ===== TABLE SETUP =====
    private void setupTables() {
        // ----- USER TABLE -----
        colUserId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));  // Đã sửa: getId() thay vì getName()
        colUserName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colUserRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole()));
        setupUserAction();

        // ----- AUCTION TABLE -----
        colAuctionId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAuctionId()));
        colAuctionItem.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItem().getName()));
        colAuctionSeller.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOwner()));
        colAuctionPrice.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCurrentPrice())));
        colAuctionStatus.setCellValueFactory(c -> {
            Auction a = c.getValue();
            if (a.isCancelled()) return new SimpleStringProperty("CANCELED"); // Ưu tiên trạng thái huỷ
            if (a.isFinished()) return new SimpleStringProperty("FINISHED");
            return new SimpleStringProperty("RUNNING");
        });
        setupAuctionAction();

        // ----- DASHBOARD PREVIEW -----
        dashColUserName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        dashColUserRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole()));
        dashColUserId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));

        dashColAuctionId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAuctionId()));
        dashColAuctionItem.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItem().getName()));
        dashColAuctionStatus.setCellValueFactory(c -> {
            Auction a = c.getValue();
            if (a.isCancelled()) return new SimpleStringProperty("CANCELED");
            if (a.isFinished()) return new SimpleStringProperty("FINISHED");
            return new SimpleStringProperty("RUNNING");
        });
    }

    // ===== FILTER =====
    private void setupFilters() {
        userRoleFilter.setItems(FXCollections.observableArrayList("ALL", "BIDDER", "SELLER"));
        userRoleFilter.setValue("ALL");

        // Thêm trạng thái "CANCELED" cho phù hợp với logic huỷ phiên
        auctionStatusFilter.setItems(FXCollections.observableArrayList("ALL", "RUNNING", "FINISHED", "CANCELED"));
        auctionStatusFilter.setValue("ALL");

        userSearchField.textProperty().addListener((obs, o, n) -> applyUserFilter());
        userRoleFilter.valueProperty().addListener((obs, o, n) -> applyUserFilter());

        auctionSearchField.textProperty().addListener((obs, o, n) -> applyAuctionFilter());
        auctionStatusFilter.valueProperty().addListener((obs, o, n) -> applyAuctionFilter());
    }

    private void applyUserFilter() {
        String keyword = userSearchField.getText().toLowerCase();
        String role = userRoleFilter.getValue();
        filteredUsers.setPredicate(user ->
                (role.equals("ALL") || user.getRole().equals(role)) &&
                        user.getName().toLowerCase().contains(keyword)
        );
        updateCountLabels();
    }

    private void applyAuctionFilter() {
        String keyword = auctionSearchField.getText().toLowerCase();
        String status = auctionStatusFilter.getValue();
        filteredAuctions.setPredicate(a -> {
            boolean statusMatch = status.equals("ALL") ||
                    (status.equals("RUNNING") && !a.isFinished() && !a.isCancelled()) ||
                    (status.equals("FINISHED") && a.isFinished() && !a.isCancelled()) ||
                    (status.equals("CANCELED") && a.isCancelled());
            return statusMatch && a.getItem().getName().toLowerCase().contains(keyword);
        });
        updateCountLabels();
    }

    private void updateCountLabels() {
        userCountLabel.setText("(" + filteredUsers.size() + " người)");
        auctionCountLabel.setText("(" + filteredAuctions.size() + " phiên)");
    }

    // ===== ACTION BUTTON =====
    private void setupUserAction() {
        colUserAction.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Xóa");
            {
                deleteBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.getRole().equals("ADMIN")) return;
                    authController.removeUser(u);
                    userList.remove(u);
                    updateStats();
                    updateCountLabels();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private void setupAuctionAction() {
        colAuctionAction.setCellFactory(param -> new TableCell<>() {
            private final Button cancelBtn = new Button("Hủy");
            {
                cancelBtn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    // Gọi logic huỷ phiên (giả định AuctionManager có phương thức cancelAuction)
                    if (auctionManager.cancelAuction(a)) {
                        // Làm mới dữ liệu từ AuctionManager để cập nhật trạng thái
                        auctionList.setAll(auctionManager.getAuctionList());
                        applyAuctionFilter(); // cập nhật lại filter hiển thị
                        updateStats();
                        updateCountLabels();
                    } else {
                        // Thông báo lỗi nếu cần
                        System.out.println("Không thể huỷ phiên này.");
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : cancelBtn);
            }
        });
    }

    // ===== STATS =====
    private void updateStats() {
        long total = auctionList.size();
        long running = auctionList.stream().filter(a -> !a.isFinished() && !a.isCancelled()).count();
        long finished = auctionList.stream().filter(a -> a.isFinished() && !a.isCancelled()).count();
        long canceled = auctionList.stream().filter(Auction::isCancelled).count();

        statRunning.setText(String.valueOf(running));
        statFinished.setText(String.valueOf(finished));
        statCanceled.setText(String.valueOf(canceled));
        statTotalAuctions.setText(String.valueOf(total));

        statTotalUsers.setText(String.valueOf(userList.size()));
        long bidder = userList.stream().filter(u -> u.getRole().equals("BIDDER")).count();
        long seller = userList.stream().filter(u -> u.getRole().equals("SELLER")).count();
        statBidder.setText(String.valueOf(bidder));
        statSeller.setText(String.valueOf(seller));

        // Tỉ lệ thành công = finished / (finished + canceled) nếu có
        long closed = finished + canceled;
        if (closed > 0) {
            double rate = (double) finished / closed * 100;
            statSuccessRate.setText(String.format("%.1f%%", rate));
        } else {
            statSuccessRate.setText("0%");
        }
    }

    // ===== NAVIGATION =====
    @FXML
    private void showDashboard() {
        setActivePage(pageDashboard);
        setActiveNav(navDashboard);
        pageTitle.setText("Dashboard");
    }

    @FXML
    private void showUsers() {
        setActivePage(pageUsers);
        setActiveNav(navUsers);
        pageTitle.setText("Quản lý User");
    }

    @FXML
    private void showAuctions() {
        setActivePage(pageAuctions);
        setActiveNav(navAuctions);
        pageTitle.setText("Quản lý Phiên đấu giá");
    }

    @FXML
    private void showStats() {
        setActivePage(pageStats);
        setActiveNav(navStats);
        pageTitle.setText("Thống kê hệ thống");
    }

    @FXML
    private void handleLogout() {
        // Gọi logic đăng xuất từ authController (nếu có)
        if (authController != null) {
            authController.logout();
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginViewMoi.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) adminNameLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Hiển thị một VBox và ẩn các trang còn lại */
    private void setActivePage(VBox activePage) {
        pageDashboard.setVisible(false);
        pageDashboard.setManaged(false);
        pageUsers.setVisible(false);
        pageUsers.setManaged(false);
        pageAuctions.setVisible(false);
        pageAuctions.setManaged(false);
        pageStats.setVisible(false);
        pageStats.setManaged(false);

        activePage.setVisible(true);
        activePage.setManaged(true);
    }

    /** Đổi màu nút đang active (xanh) và reset các nút khác */
    private void setActiveNav(Button activeBtn) {
        Button[] navs = {navDashboard, navUsers, navAuctions, navStats};
        for (Button btn : navs) {
            if (btn == activeBtn) {
                btn.setStyle("-fx-background-color: rgba(37,99,235,0.15); -fx-text-fill: #60a5fa; -fx-border-color: #2563eb; -fx-border-width: 0 0 0 3; -fx-alignment: center-left; -fx-padding: 8 10; -fx-background-radius: 7; -fx-font-size: 12; -fx-cursor: hand;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #5a6a8a; -fx-alignment: center-left; -fx-padding: 8 10; -fx-background-radius: 7; -fx-font-size: 12; -fx-cursor: hand;");
            }
        }
    }
}
