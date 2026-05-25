package client.controller;

import server.controller.AuthenticationController;
import server.dao.AuctionDAO;
import server.dao.ItemDAO;
import server.dao.UserDAO;
import server.model.AdminEventBus;
import server.model.Auction;
import server.model.AuctionManager;
import server.model.user.*;

import java.sql.SQLException;
import java.util.List;

/**
 * AdminService: Tất cả chức năng của Admin.
 *
 * Mỗi method đều kiểm tra {@code currentUser.getRole().equals("ADMIN")} trước
 * khi thực thi. Nếu không đủ quyền thì ném {@link SecurityException}.
 *
 * Cũng publish sự kiện tới {@link AdminEventBus} để AdminController cập nhật
 * systemLog theo thời gian thực.
 */

public class AdminService {

    private final User currentAdmin;
    private final AuthenticationController authController;
    private final UserDAO userDAO = new UserDAO();
    private final AuctionManager auctionManager = AuctionManager.getInstance();
    private final AuctionDAO auctionDAO = new AuctionDAO();

    public AdminService(AuthenticationController authController) {
        this.authController = authController;
        this.currentAdmin   = authController.getCurrentUser();
        requireAdmin();
    }

    /**
     * Thêm user mới. Admin không thể tự thêm ADMIN khác.
     *
     * @throws IllegalArgumentException nếu username đã tồn tại hoặc role không hợp lệ
     */

    public void addUser(String name, String password, String role) {
        requireAdmin();

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Tên user không được để trống");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        if (!role.equalsIgnoreCase("BIDDER") && !role.equalsIgnoreCase("SELLER"))
            throw new IllegalArgumentException("Role phải là BIDDER hoặc SELLER");
        if (authController.getAllUsers().stream()
                .anyMatch(u -> u.getName().equalsIgnoreCase(name)))
            throw new IllegalArgumentException("Username '" + name + "' đã tồn tại");

        authController.register(name, password, role.toUpperCase());

        AdminEventBus.getInstance().publish(
                AdminEventBus.EVENT_USER_ADDED,
                name + " (" + role.toUpperCase() + ")"
        );
    }

    /**
     * Sửa tên/mật khẩu user. Admin không thể sửa tài khoản Admin khác.
     *
     * @param targetName  tên user cần sửa
     * @param newName     tên mới (để trống = giữ nguyên)
     * @param newPassword mật khẩu mới (để trống = giữ nguyên)
     */
    public void updateUser(String targetName, String newName, String newPassword) {
        requireAdmin();

        User target = authController.getAllUsers().stream()
                .filter(u -> u.getName().equals(targetName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user: " + targetName));

        if (target.getRole().equals("ADMIN") && !target.getName().equals(currentAdmin.getName()))
            throw new SecurityException("Không thể sửa tài khoản Admin khác");

        String resolvedName = (newName == null || newName.isBlank()) ? targetName : newName;
        String resolvedPwd  = (newPassword == null || newPassword.isBlank()) ? null : newPassword;

        // Kiểm tra trùng tên nếu đổi tên
        if (!resolvedName.equals(targetName)) {
            boolean duplicate = authController.getAllUsers().stream()
                    .anyMatch(u -> u.getName().equalsIgnoreCase(resolvedName));
            if (duplicate)
                throw new IllegalArgumentException("Username '" + resolvedName + "' đã tồn tại");
        }

        // Cập nhật trong memory map
        authController.updateUser(targetName, resolvedName, resolvedPwd != null ? resolvedPwd : target.getPassword());

        // Cập nhật DB (AdminService gọi trực tiếp để không phụ thuộc vào TODO cũ)
        try {
            User updated = authController.getAllUsers().stream()
                    .filter(u -> u.getName().equals(resolvedName))
                    .findFirst().orElse(target);
            userDAO.update(updated);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật DB: " + e.getMessage());
        }

        AdminEventBus.getInstance().publish(
                AdminEventBus.EVENT_USER_UPDATED,
                targetName + " → " + resolvedName
        );
    }

    //Xóa user. Admin không thể tự xóa bản thân.

    public void deleteUser(String targetName) {
        requireAdmin();

        if (targetName.equals(currentAdmin.getName()))
            throw new SecurityException("Không thể tự xóa tài khoản của chính mình");

        User target = authController.getAllUsers().stream()
                .filter(u -> u.getName().equals(targetName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user: " + targetName));

        if (target.getRole().equals("ADMIN"))
            throw new SecurityException("Không thể xóa tài khoản Admin khác");

        authController.removeUser(target);

        AdminEventBus.getInstance().publish(
                AdminEventBus.EVENT_USER_DELETED,
                targetName + " (" + target.getRole() + ")"
        );
    }

    // Lấy danh sách toàn bộ user.
    public List<User> getAllUsers() {
        requireAdmin();
        return authController.getAllUsers();
    }

    // ──────────────────────────────────────────────────────────────
    //  QUẢN LÝ PHIÊN ĐẤU GIÁ
    // ──────────────────────────────────────────────────────────────


    // Admin hủy phiên (bất kể trạng thái OPEN/RUNNING).
    public void cancelAuction(String auctionId) {
        requireAdmin();
        Auction auction = findAuction(auctionId);
        boolean ok = auctionManager.cancelAuction(auction);
        if (!ok)
            throw new RuntimeException("Không thể hủy phiên ở trạng thái: " + auction.getStatus());

        AdminEventBus.getInstance().publish(
                AdminEventBus.EVENT_AUCTION_CANCELED,
                auctionId + " — " + auction.getItem().getName()
        );
    }

    // Admin xóa hoàn toàn phiên khỏi hệ thống (chỉ xóa phiên đã FINISHED/CANCELED).
    public void deleteAuction(String auctionId) {
        requireAdmin();
        Auction auction = findAuction(auctionId);

        if (!auction.isFinished() && !auction.isCancelled())
            throw new RuntimeException("Chỉ xóa được phiên đã kết thúc hoặc đã hủy. Hãy hủy phiên trước.");

        String itemId   = auction.getItem().getId();
        String itemName = auction.getItem().getName();
        auctionManager.removeAuction(auction);
        try {
            auctionDAO.delete(auctionId);
            new ItemDAO().delete(itemId);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa DB: " + e.getMessage());
        }

        AdminEventBus.getInstance().publish(
                AdminEventBus.EVENT_AUCTION_DELETED,
                auctionId + " — " + itemName
        );
    }

    // Admin sửa tên/mô tả/giá khởi điểm của phiên (chỉ khi chưa có người đấu).
    public void updateAuction(String auctionId, String newName,
                               String newDescription, double newPrice) {
        requireAdmin();
        Auction auction = findAuction(auctionId);

        if (auction.getCurrentPrice() > auction.getItem().getBasePrice())
            throw new RuntimeException("Không thể sửa giá khi đã có người đấu");

        auction.getItem().setName(newName);
        auction.getItem().setDescription(newDescription);
        auction.getItem().setPrice(newPrice);
        try {
            new ItemDAO().update(auction.getItem());
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi cập nhật DB: " + e.getMessage());
        }

        AdminEventBus.getInstance().publish(
                AdminEventBus.EVENT_AUCTION_CANCELED,   // dùng tạm; có thể thêm EVENT_AUCTION_UPDATED
                "Sửa phiên " + auctionId + " → tên: " + newName
        );
    }

    // ──────────────────────────────────────────────────────────────
    //  HELPER
    // ──────────────────────────────────────────────────────────────

    private void requireAdmin() {
        if (currentAdmin == null || !currentAdmin.getRole().equals("ADMIN"))
            throw new SecurityException("Chỉ ADMIN mới có quyền thực hiện thao tác này");
    }

    private Auction findAuction(String auctionId) {
        Auction a = auctionManager.findById(auctionId);
        if (a == null)
            throw new IllegalArgumentException("Không tìm thấy phiên: " + auctionId);
        return a;
    }
}
