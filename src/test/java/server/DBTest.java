package server;

import server.dao.*;

import server.model.Auction;
import server.model.item.*;
import server.model.user.Bidder;
import server.model.user.User;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public class DBTest {
    public static void main(String[] args) throws Exception {

        // TEST 1: Kết nối
        System.out.println("===== TEST KẾT NỐI =====");
        Connection conn = DBConnection.getConnection();
        System.out.println(conn != null ? "[OK] Kết nối thành công!" : "[FAIL] Kết nối thất bại!");

        // Xóa dữ liệu test cũ nếu tồn tại (để có thể chạy lại nhiều lần)
        System.out.println("\n===== CLEANUP =====");
        conn.createStatement().executeUpdate("DELETE FROM bid_transactions WHERE auction_id = 'AU-TEST-001'");
        conn.createStatement().executeUpdate("DELETE FROM auctions WHERE id = 'AU-TEST-001'");
        conn.createStatement().executeUpdate("DELETE FROM items WHERE id = 'ITEM001'");
        conn.createStatement().executeUpdate("DELETE FROM users WHERE id = 'TEST001'");
        System.out.println("[OK] Đã xóa dữ liệu test cũ.");

        // TEST 2: UserDAO
        System.out.println("\n===== TEST USER DAO =====");
        UserDAO userDAO = new UserDAO();

        // Thêm user mới
        Bidder testUser = new Bidder(1, "Nguyen Test","Nguyen Test", "123456");
        userDAO.save(testUser);
        System.out.println("[OK] Đã lưu user: " + testUser.getName());

        // Tìm lại theo id
        User found = userDAO.findById(1);
        System.out.println(found != null ? "[OK] findById: " + found.getName() : "[FAIL] Không tìm thấy!");

        // Load tất cả
        List<User> allUsers = userDAO.findAll();
        System.out.println("[OK] Tổng số user trong DB: " + allUsers.size());
        // TEST 3: ItemDAO
        System.out.println("\n===== TEST ITEM DAO =====");
        ItemDAO itemDAO = new ItemDAO();

        // Thêm item mới
        Art testArt = new Art("ITEM001", "Tranh Test", 5_000_000, "Tranh đẹp", "Minh", "Picasso");
        itemDAO.save(testArt);
        System.out.println("[OK] Đã lưu item: " + testArt.getName());

        // Tìm lại
        Item foundItem = itemDAO.findById("ITEM001");
        System.out.println(foundItem != null ? "[OK] findById: " + foundItem.getName() : "[FAIL] Không tìm thấy!");
        // TEST 4: AuctionDAO
        System.out.println("\n===== TEST AUCTION DAO =====");
        AuctionDAO auctionDAO = new AuctionDAO();

        // Tạo auction dùng item ở bước 3
        Auction testAuction = new Auction("AU-TEST-001", foundItem, LocalDateTime.now().plusHours(2),"Minh");
        auctionDAO.save(testAuction);
        System.out.println("[OK] Đã lưu auction: " + testAuction.getAuctionId());

        // Load tất cả
        List<Auction> allAuctions = auctionDAO.findAll();
        System.out.println("[OK] Tổng số auction trong DB: " + allAuctions.size());

        // Test updateAfterBid
        auctionDAO.updateAfterBid("AU-TEST-001", 6_000_000, "Nguyen Test");
        Auction updated = auctionDAO.findById("AU-TEST-001");
        System.out.println("[OK] Giá sau bid: " + updated.getCurrentPrice());

        // Test finish
        auctionDAO.finish("AU-TEST-001");
        Auction finished = auctionDAO.findById("AU-TEST-001");
        System.out.println("[OK] isFinished: " + finished.isFinished());
    }

}