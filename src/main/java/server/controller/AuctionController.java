package server.controller;

import server.model.Auction;
import server.model.user.User;

public class AuctionController {

    private AuthenticationController auth;

    public AuctionController(AuthenticationController auth) {
        this.auth = auth;
    }

    public void placeBid(Auction auction, double amount) {

        User currentUser = auth.getCurrentUser();

        // 1. chưa login
        if (currentUser == null) {
            throw new RuntimeException("Chưa đăng nhập");
        }

        // 2. check quyền
        if (!currentUser.getRole().equals("BIDDER")) {
            throw new RuntimeException("Chỉ BIDDER mới được đặt giá");
        }

        try {
            // 3. gọi Auction (TRUYỀN TÊN, không phải object)
            auction.placeBid(currentUser.getName(), amount);

            System.out.println("Đặt giá thành công bởi: " + currentUser.getName());

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}