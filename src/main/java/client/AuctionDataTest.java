package client;

import server.model.Auction;
import server.model.AuctionManager;
import server.model.item.Item;
import server.model.item.ItemFactory;
import server.controller.AuthenticationController;

import java.time.LocalDateTime;

public class AuctionDataTest {

    // ── Tạo các phiên đấu giá mẫu ────────────────────────────
    public static void setupAuctions() {
        AuctionManager manager = AuctionManager.getInstance();

        Item iPhone = ItemFactory.creatItem(
                "electronics", "ITEM001", "iPhone 15 Pro",
                20000000, "Điện thoại mới 100%, chưa kích hoạt","seller1",
                "12", ""
        );

        Item tranh = ItemFactory.creatItem(
                "art", "ITEM002", "Tranh sơn dầu",
                5000000, "Tác phẩm nghệ thuật độc đáo","seller1",
                "Picasso", ""
        );

        Item xe = ItemFactory.creatItem(
                "vehicle", "ITEM003", "Toyota Camry 2020",
                800000000, "Xe đẹp, ít sử dụng, còn mới 95%","seller1",
                "2020", "Toyota"
        );

        manager.addAuction(new Auction("AU001", iPhone,
                LocalDateTime.now().plusMinutes(5), "Tran Quang Lam"));
        manager.addAuction(new Auction("AU002", tranh,
                LocalDateTime.now().plusMinutes(10), "Tran Quang Lam"));
        manager.addAuction(new Auction("AU003", xe,
                LocalDateTime.now().plusHours(1), "Tran Quang Lam"));

        System.out.println("[TEST] Đã tạo "
                + manager.getAuctionList().size() + " phiên đấu giá.");
    }

    // ── Tạo các tài khoản mẫu ────────────────────────────────
    public static void setupUsers(AuthenticationController auth) {
        auth.register("user1",   "Nguyen Van A", "123456", "BIDDER");
        auth.register("user2",   "Tran Thi B",   "123456", "BIDDER");
        auth.register("seller1", "Le Van C",      "123456", "SELLER");

        System.out.println("[TEST] Đã tạo 3 tài khoản mẫu.");
        System.out.println("[TEST] Đăng nhập: user1 / 123456");
    }
}