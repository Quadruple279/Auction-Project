package client.controller;

import client.ClientSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;

import java.util.List;
import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import server.controller.AuthenticationController;

import server.model.user.User;
import shared.dto.AuctionDTO;
import shared.protocol.AuctionEvent;
import shared.protocol.AuctionObserver;
import shared.protocol.MessageType;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminController implements Initializable, AuctionObserver {

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
    @FXML private TableView<AuctionDTO> auctionTable;
    @FXML private TableColumn<AuctionDTO, String> colAuctionId, colAuctionItem,
            colAuctionSeller, colAuctionPrice, colAuctionStatus;
    @FXML private TableColumn<AuctionDTO, Void> colAuctionAction;
    @FXML private TextField auctionSearchField;
    @FXML private ComboBox<String> auctionStatusFilter;
    @FXML private Label auctionCountLabel; // Bổ sung

    // ===== FXML – Dashboard preview =====
    @FXML private TableView<User> dashUserTable;
    @FXML private TableColumn<User, String> dashColUserName, dashColUserRole, dashColUserId;
    @FXML private TableView<AuctionDTO> dashAuctionTable;
    @FXML private TableColumn<AuctionDTO, String> dashColAuctionId, dashColAuctionItem, dashColAuctionStatus;

    // ===== FXML – Stats =====
    @FXML private Label statTotalUsers, statRunning, statFinished, statCanceled;
    @FXML private Label statBidder, statSeller, statTotalAuctions;
    @FXML private Label statSuccessRate;   // Tỉ lệ thành công
    @FXML private TextArea systemLog;      // Log hệ thống
    @FXML private Label statUserSub;

    // ===== DATA =====
    private AuthenticationController authController;
    private AdminService adminService;

    private ObservableList<User>    userList    = FXCollections.observableArrayList();
    private ObservableList<AuctionDTO> auctionList = FXCollections.observableArrayList();
    private FilteredList<User>    filteredUsers;
    private FilteredList<AuctionDTO> filteredAuctions;

    // ===== INIT =====
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTables();
        setupFilters();
        // Mặc định hiển thị Dashboard
        showDashboard();
    }

    /** Gọi từ LoginController sau khi xác thực ADMIN thành công */
    public void setAuthController(AuthenticationController auth) {
        this.authController = auth;
        this.adminService   = new AdminService(auth);
        adminNameLabel.setText(auth.getCurrentUser().getName());
        loadData();
        registerEventBusListener();
    }

    // ===== LOAD DATA =====
    private void loadData() {
        userList.setAll(authController.getAllUsers());
        filteredUsers     = new FilteredList<>(userList,    p -> true);
        userTable.setItems(filteredUsers);
        ClientSocket.getInstance().setResponseListener(msg -> {
            if (msg.getType() == MessageType.AUCTION_LIST){
                try{
                    String jsonData = msg.get("data");
                    ObjectMapper mapper = new ObjectMapper();
                    List<AuctionDTO> list = mapper.readValue(
                            jsonData,
                            new com.fasterxml.jackson.core.type.TypeReference<List<AuctionDTO>>() {}
                    );
                    Platform.runLater(() -> {
                        auctionList.setAll(list);
                        filteredAuctions = new FilteredList<>(auctionList, p -> true);
                        auctionTable.setItems(filteredAuctions);
                        refreshDashboardTables();
                        updateStats();
                        updateCountLabels();
                    });
                }
                catch (Exception e){
                    Platform.runLater(() -> {
                        if (systemLog != null)
                            systemLog.appendText("Lỗi tải phiên: " + e.getMessage() + "\n");
                    });
                }
            }
        });
        ClientSocket.getInstance().sendGetAuctions();
    }
    private void refreshDashboardTables() {
        dashUserTable.setItems(FXCollections.observableArrayList(userList.subList(0, Math.min(5, userList.size()))));
        dashAuctionTable.setItems(FXCollections.observableArrayList(
                auctionList.stream().filter(a -> !a.isFinished()).limit(5).toList()
        ));
    }



    /** Đăng ký lắng nghe AdminEventBus – cập nhật systemLog và refresh data */
    private void registerEventBusListener() {
        ClientSocket.getInstance().addObserver(this);
    }
    @Override
    public void onAuctionEvent(AuctionEvent event) {
        Platform.runLater(() -> {
            switch (event.getType()) {
                case NEW_AUCTION -> {
                    if (systemLog != null)
                        systemLog.appendText("[Phiên mới] " + event.getAuctionId() + "\n");
                    loadData();
                }
                case AUCTION_ENDED -> {
                    if (systemLog != null)
                        systemLog.appendText("[Kết thúc] " + event.getAuctionId()
                                + " — Người thắng: " + event.getLeadingBidder() + "\n");
                    loadData();
                }
                case USER_DELETED -> {
                    String deletedName = event.getAuctionId(); // dùng auctionId field để truyền username
                    userList.removeIf(u -> u.getName().equals(deletedName));
                    refreshDashboardTables();
                    updateStats();
                    updateCountLabels();
                }
                default -> {}
            }
        });
    }

    // ───────────────────────────── TABLE SETUP ─────────────────────────────

    private void setupTables() {
        // ----- USER TABLE -----
        colUserId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));  // Đã sửa: getId() thay vì getName()
        colUserName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colUserRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole()));
        setupUserActionColumn();

        // ----- AUCTION TABLE -----
        colAuctionId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAuctionId()));
        colAuctionItem.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItemName()));
        colAuctionSeller.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOwner()));
        colAuctionPrice.setCellValueFactory(c  -> new SimpleStringProperty(
                String.format("%,.0f", c.getValue().getCurrentPrice())));
        colAuctionStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        setupAuctionActionColumn();

        // ----- DASHBOARD PREVIEW -----
        dashColUserName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        dashColUserRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRole()));
        dashColUserId.setCellValueFactory(c   -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        dashColAuctionId.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getAuctionId()));
        dashColAuctionItem.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getItemName()));
        dashColAuctionStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
    }

    // ===== FILTER =====
    private void setupFilters() {
        userRoleFilter.setItems(FXCollections.observableArrayList("ALL", "BIDDER", "SELLER"));
        userRoleFilter.setValue("ALL");

        // Thêm trạng thái "CANCELED" cho phù hợp với logic huỷ phiên
        auctionStatusFilter.setItems(FXCollections.observableArrayList("ALL", "RUNNING", "FINISHED", "CANCELED"));
        auctionStatusFilter.setValue("ALL");

        userSearchField.textProperty().addListener((obs, o, n)   -> applyUserFilter());
        userRoleFilter.valueProperty().addListener((obs, o, n)   -> applyUserFilter());
        auctionSearchField.textProperty().addListener((obs, o, n) -> applyAuctionFilter());
        auctionStatusFilter.valueProperty().addListener((obs, o, n) -> applyAuctionFilter());
    }

    private void applyUserFilter() {
        if (filteredUsers == null){
            return;
        }
        String keyword = userSearchField.getText().toLowerCase();
        String role    = userRoleFilter.getValue();
        filteredUsers.setPredicate(u ->
                (role.equals("ALL") || u.getRole().equals(role)) &&
                        u.getName().toLowerCase().contains(keyword));
        updateCountLabels();
    }

    private void applyAuctionFilter() {
        if (filteredAuctions == null){
            return;
        }
        String keyword = auctionSearchField.getText().toLowerCase();
        String status = auctionStatusFilter.getValue();
        filteredAuctions.setPredicate(a -> {
            String statusDTO = a.getStatus();
            boolean statusMatch = status.equals("ALL") ||
                    (status.equals("RUNNING") && !statusDTO.equals("FINISHED") && !statusDTO.equals("CANCELED")) ||
                    (status.equals("FINISHED") && statusDTO.equals("FINISHED") && !statusDTO.equals("CANCELED")) ||
                    (status.equals("CANCELED") && statusDTO.equals("CANCELED"));
            return statusMatch && a.getItemName().toLowerCase().contains(keyword);
        });
        updateCountLabels();
    }

    private void updateCountLabels() {
        if (filteredUsers != null){
            userCountLabel.setText("(" + filteredUsers.size() + " người)");
        }
        if (filteredAuctions != null){
            auctionCountLabel.setText("(" + filteredAuctions.size() + " phiên)");
        }}

    // ───────────────────────────── ACTION COLUMNS ─────────────────────────────

    /**
     * Cột Hành động của bảng User: nút [Sửa] + [Xóa].
     */
    private void setupUserActionColumn() {
        colUserAction.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn   = new Button("Sửa");
            private final Button deleteBtn = new Button("Xóa");
            private final HBox   box       = new HBox(4, editBtn, deleteBtn);
            {
                editBtn.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white;" +
                        " -fx-background-radius: 4; -fx-font-size: 10; -fx-padding: 3 8;");
                deleteBtn.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fca5a5;" +
                        " -fx-background-radius: 4; -fx-font-size: 10; -fx-padding: 3 8;");

                editBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    showEditUserDialog(u);
                });
                deleteBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.getRole().equals("ADMIN")) return;
                    if (confirmAction("Xác nhận xóa", "Xóa user \"" + u.getName() + "\"?")) {
                        ClientSocket.getInstance().sendDeleteUser(u.getName());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                // Ẩn nút Xóa với Admin
                deleteBtn.setDisable(u.getRole().equals("ADMIN"));
                setGraphic(box);
            }
        });
    }

    /**
     * Cột Hành động của bảng Auction: nút [Hủy] + [Xóa].
     */
    private void setupAuctionActionColumn() {
        colAuctionAction.setCellFactory(param -> new TableCell<>() {
            private final Button cancelBtn = new Button("Hủy");
            private final Button deleteBtn = new Button("Xóa");
            private final HBox   box       = new HBox(4, cancelBtn, deleteBtn);
            {
                cancelBtn.setStyle("-fx-background-color: #78350f; -fx-text-fill: #fcd34d;" +
                        " -fx-background-radius: 4; -fx-font-size: 10; -fx-padding: 3 8;");
                deleteBtn.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #fca5a5;" +
                        " -fx-background-radius: 4; -fx-font-size: 10; -fx-padding: 3 8;");

                cancelBtn.setOnAction(e -> {
                    AuctionDTO a = getTableView().getItems().get(getIndex());
                    String status = a.getStatus();
                    if (status.equals("FINISHED") || status.equals("CANCELED")) {
                        showError("Phiên đã kết thúc hoặc đã bị hủy."); return;
                    }
                    if (confirmAction("Xác nhận hủy", "Hủy phiên \"" + a.getAuctionId() + "\"?")) {
                        ClientSocket.getInstance().setResponseListener(msg -> {
                            if (msg.getType() == MessageType.CANCEL_AUCTION_SUCCESS) {
                                Platform.runLater(() -> loadData());
                            } else if (msg.getType() == MessageType.ERROR) {
                                Platform.runLater(() -> showError(msg.get("reason")));
                            }
                        });
                        ClientSocket.getInstance().sendCancelAuction(a.getAuctionId());
                    }
                });
                deleteBtn.setOnAction(e -> {
                    AuctionDTO a = getTableView().getItems().get(getIndex());
                    String status = a.getStatus();
                    if (!status.equals("FINISHED") && !status.equals("CANCELED")) {
                        showError("Hãy hủy phiên trước khi xóa."); return;
                    }
                    if (confirmAction("Xác nhận xóa", "Xóa hoàn toàn phiên \"" + a.getAuctionId() + "\"?")) {
                        ClientSocket.getInstance().setResponseListener(msg -> {
                            if (msg.getType() == MessageType.DELETE_AUCTION_SUCCESS) {
                                Platform.runLater(() -> loadData());
                            } else if (msg.getType() == MessageType.ERROR) {
                                Platform.runLater(() -> showError(msg.get("reason")));
                            }
                        });
                        ClientSocket.getInstance().sendDeleteAuction(a.getAuctionId());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                AuctionDTO a = getTableView().getItems().get(getIndex());
                String status = a.getStatus();
                cancelBtn.setDisable(status.equals("FINISHED") || status.equals("CANCELED"));
                deleteBtn.setDisable(!status.equals("FINISHED") && !status.equals("CANCELED"));
                setGraphic(box);
            }
        });
    }

    // ───────────────────────────── DIALOGS ─────────────────────────────

    /**
     * Dialog thêm user mới.
     * Gọi từ nút "Thêm User" trong FXML (onAction="#showAddUserDialog").
     */
    @FXML
    public void showAddUserDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Thêm User mới");
        dialog.setHeaderText(null);

        ButtonType addBtn = new ButtonType("Thêm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f1729;");

        // Form fields
        TextField    nameField = styledTextField("Tên đăng nhập");
        PasswordField pwdField = new PasswordField();
        pwdField.setPromptText("Mật khẩu");
        pwdField.setStyle(nameField.getStyle());

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("BIDDER", "SELLER");
        roleBox.setValue("BIDDER");
        roleBox.setStyle("-fx-background-color: #0a1220; -fx-text-fill: #e8efff;" +
                " -fx-border-color: rgba(37,99,235,0.3); -fx-border-radius: 6;" +
                " -fx-background-radius: 6; -fx-font-size: 12;");
        roleBox.setMaxWidth(Double.MAX_VALUE);

        Label msgLabel = new Label();
        msgLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11;");

        VBox form = new VBox(10,
                styledLabel("Tên đăng nhập"), nameField,
                styledLabel("Mật khẩu"),      pwdField,
                styledLabel("Role"),           roleBox,
                msgLabel
        );
        form.setPadding(new Insets(16));
        form.setPrefWidth(320);
        form.setStyle("-fx-background-color: #0f1729;");
        dialog.getDialogPane().setContent(form);

        // Validate
        javafx.scene.Node okNode = dialog.getDialogPane().lookupButton(addBtn);
        okNode.setDisable(true);
        nameField.textProperty().addListener((o, ov, nv) ->
                okNode.setDisable(nv.isBlank() || pwdField.getText().isBlank()));
        pwdField.textProperty().addListener((o, ov, nv) ->
                okNode.setDisable(nameField.getText().isBlank() || nv.isBlank()));

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == addBtn) {
            try {
                adminService.addUser(nameField.getText().trim(),
                        pwdField.getText(),
                        roleBox.getValue());
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    /** Dialog sửa tên / mật khẩu user */
    private void showEditUserDialog(User user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Sửa User: " + user.getName());
        dialog.setHeaderText(null);

        ButtonType saveBtn = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #0f1729;");

        TextField     nameField = styledTextField(user.getDisplayName());
        nameField.setText(user.getDisplayName());
        PasswordField pwdField  = new PasswordField();
        pwdField.setPromptText("Để trống = giữ nguyên mật khẩu");
        pwdField.setStyle(nameField.getStyle());

        Label roleLabel = styledLabel("Role: " + user.getRole() + "  (không đổi được)");
        roleLabel.setStyle(roleLabel.getStyle() + "-fx-text-fill: #6b7280;");

        VBox form = new VBox(10,
                styledLabel("Tên hiển thị mới"),     nameField,
                styledLabel("Mật khẩu mới"), pwdField,
                roleLabel
        );
        form.setPadding(new Insets(16));
        form.setPrefWidth(320);
        form.setStyle("-fx-background-color: #0f1729;");
        dialog.getDialogPane().setContent(form);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtn) {
            String newDisplayName = nameField.getText().trim();
            String newPwd  = pwdField.getText();
            if (newDisplayName.isBlank()) { showError("Tên hiển thị không được để trống"); return; }
            try {
                adminService.updateUser(user.getName(), newDisplayName,
                        newPwd.isBlank() ? null : newPwd);
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    // ───────────────────────────── STATS ─────────────────────────────

    private void updateStats() {
        long total    = auctionList.size();
        long running = auctionList.stream().filter(a -> a.getStatus().equals("RUNNING") || a.getStatus().equals("OPEN")).count();
        long finished = auctionList.stream().filter(a -> a.getStatus().equals("FINISHED")).count();
        long canceled = auctionList.stream().filter(a -> a.getStatus().equals("CANCELED")).count();

        statRunning.setText(String.valueOf(running));
        statFinished.setText(String.valueOf(finished));
        statCanceled.setText(String.valueOf(canceled));
        statTotalAuctions.setText(String.valueOf(total));
        statTotalUsers.setText(String.valueOf(userList.size()));

        long bidder = userList.stream().filter(u -> u.getRole().equals("BIDDER")).count();
        long seller = userList.stream().filter(u -> u.getRole().equals("SELLER")).count();
        long admin = userList.stream().filter(u -> u.getRole().equals("ADMIN")).count();
        statBidder.setText(String.valueOf(bidder));
        statSeller.setText(String.valueOf(seller));
        statUserSub.setText(String.format("Bidder: %d · Seller: %d · Admin: %d ", bidder, seller, admin));

        // Tỉ lệ thành công = finished / (finished + canceled) nếu có
        long closed = finished + canceled;
        statSuccessRate.setText(closed > 0
                ? String.format("%.1f%%", (double) finished / closed * 100)
                : "0%");
    }

    // ───────────────────────────── NAVIGATION ─────────────────────────────

    @FXML private void showDashboard() { setActivePage(pageDashboard); setActiveNav(navDashboard); pageTitle.setText("Dashboard"); }
    @FXML private void showUsers()     { setActivePage(pageUsers);     setActiveNav(navUsers);     pageTitle.setText("Quản lý User"); }
    @FXML private void showAuctions()  { setActivePage(pageAuctions);  setActiveNav(navAuctions);  pageTitle.setText("Quản lý Phiên đấu giá"); }
    @FXML private void showStats()     { setActivePage(pageStats);     setActiveNav(navStats);     pageTitle.setText("Thống kê hệ thống"); }

    @FXML
    private void handleLogout() {
        ClientSocket.getInstance().removeObserver(this);
        if (authController != null) authController.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginViewMoi.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) adminNameLabel.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Hiển thị một VBox và ẩn các trang còn lại */
    private void setActivePage(VBox activePage) {
        for (VBox p : new VBox[]{pageDashboard, pageUsers, pageAuctions, pageStats}) {
            p.setVisible(p == activePage);
            p.setManaged(p == activePage);
        }
    }

    /** Đổi màu nút đang active (xanh) và reset các nút khác */
    private void setActiveNav(Button activeBtn) {
        String activeStyle   = "-fx-background-color: rgba(37,99,235,0.15); -fx-text-fill: #60a5fa;" +
                " -fx-border-color: #2563eb; -fx-border-width: 0 0 0 3;" +
                " -fx-alignment: center-left; -fx-padding: 8 10; -fx-background-radius: 7;" +
                " -fx-font-size: 12; -fx-cursor: hand;";
        String defaultStyle  = "-fx-background-color: transparent; -fx-text-fill: #5a6a8a;" +
                " -fx-alignment: center-left; -fx-padding: 8 10; -fx-background-radius: 7;" +
                " -fx-font-size: 12; -fx-cursor: hand;";
        for (Button btn : new Button[]{navDashboard, navUsers, navAuctions, navStats}) {
            btn.setStyle(btn == activeBtn ? activeStyle : defaultStyle);
        }
    }

    // ───────────────────────────── HELPERS ─────────────────────────────

    private boolean confirmAction(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private TextField styledTextField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #0a1220; -fx-text-fill: #e8efff;" +
                " -fx-prompt-text-fill: #374151; -fx-font-size: 12;" +
                " -fx-border-color: rgba(37,99,235,0.3); -fx-border-radius: 6;" +
                " -fx-background-radius: 6; -fx-padding: 6 10;");
        return f;
    }

    private Label styledLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11;");
        return l;
    }
}
