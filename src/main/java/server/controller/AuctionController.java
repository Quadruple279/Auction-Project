package server.controller;
import server.dao.AuctionDAO;
import server.dao.BidTransactionDAO;
import server.exception.AuctionClosedException;
import server.exception.InvalidBidException;
import server.model.AuctionManager;
import server.model.BidTransaction;
import server.model.item.Item;
import java.time.LocalDateTime;
import java.sql.SQLException;

import server.model.Auction;
import server.model.user.User;
import server.dao.DataStorage;

public class AuctionController {

    private AuthenticationController auth;
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final AuctionManager auctionManager = AuctionManager.getInstance();
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
    public AuctionController(AuthenticationController auth) {
        this.auth = auth;
    }

    public void placeBid(String auctionId, double amount) {
        User currentUser = auth.getCurrentUser();
        // 1. chưa login
        if (currentUser == null) {
            throw new RuntimeException("Chưa đăng nhập");
        }
        // 2. check quyền
        if (!currentUser.getRole().equals("BIDDER")) {
            throw new RuntimeException("Chỉ BIDDER mới được đặt giá");
        }
        Auction auction = auctionManager.findById(auctionId);
        if (auction == null){
            throw new RuntimeException("Không tìm thấy phiên đấu giá");
        }
        try {
            // 3. gọi Auction (TRUYỀN TÊN, không phải object)
            auction.placeBid(currentUser.getName(), amount);
            auctionDAO.updateAfterBid(auctionId,auction.getCurrentPrice(),auction.getLeadingBidder());
            BidTransaction lastBid = auction.getTransactionHistory().get(auction.getTransactionHistory().size()-1);
            bidTransactionDAO.save(lastBid);

        } catch (SQLException | AuctionClosedException | InvalidBidException e) {
            System.out.println("Lỗi lưu database: " + e.getMessage());
        }
    }
    public void createAuction(String auctionId,Item item,LocalDateTime endTime){
        User currentUser = auth.getCurrentUser();
        if (currentUser == null){
            System.out.println("Chưa đăng nhập");
        }
        if (!currentUser.getRole().equals("SELLER")){
            throw new RuntimeException("Chỉ SELLER mới được tạo phiên đấu giá");
        }
        Auction auction = new Auction(auctionId,item,endTime);
        auctionManager.addItem(auction);
        try{
            auctionDAO.save(auction);
        }
        catch (SQLException e){
            System.out.printf("Lỗi lưu DB: "+e.getMessage());
        }
    }
    public void finishAuction(String auctionId){
        Auction auction = auctionManager.findById(auctionId);
        if (auction == null){
            throw new RuntimeException("Không tìm thấy phiên đấu giá");
        }
        auction.finishAuction();
        try{
            auctionDAO.finish(auctionId);
        }
        catch (SQLException e){
            System.out.println("Lỗi lưu DB: "+ e.getMessage());
        }
    }
}