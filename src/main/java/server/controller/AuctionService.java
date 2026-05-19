package server.controller;
import server.dao.AuctionDAO;
import server.dao.BidTransactionDAO;
import server.dao.ItemDAO;
import server.exception.AuctionClosedException;
import server.exception.InvalidBidException;
import server.model.AuctionManager;
import server.model.BidTransaction;
import server.model.item.Item;
import java.time.LocalDateTime;
import java.sql.SQLException;

import server.model.Auction;
import server.model.user.User;

public class AuctionService {

    private AuthenticationController auth;
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final AuctionManager auctionManager = AuctionManager.getInstance();
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
    public AuctionService(AuthenticationController auth) {
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

        } catch (AuctionClosedException | InvalidBidException e) {
            throw new RuntimeException(e.getMessage());
        }
        catch (SQLException e){
            throw new RuntimeException("Lỗi database: " + e.getMessage());
        }
    }
    public void createAuction(String auctionId,Item item,LocalDateTime endTime){
        User currentUser = auth.getCurrentUser();
        if (currentUser == null){
            throw new RuntimeException("Chưa đăng nhập");
        }
        if (!currentUser.getRole().equals("SELLER")){
            throw new RuntimeException("Chỉ SELLER mới được tạo phiên đấu giá");
        }
        Auction auction = new Auction(auctionId,item,endTime,currentUser.getName());
        auctionManager.addAuction(auction);
        try{
            new ItemDAO().save(item);
            auctionDAO.save(auction);
        }
        catch (SQLException e){
            throw new RuntimeException("Lỗi lưu DB: "+e.getMessage());
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
            throw new RuntimeException("Lỗi lưu DB: "+ e.getMessage());
        }
    }
    public void deleteAuction(String auctionId) {
        Auction auction = auctionManager.findById(auctionId);
        if (auction == null) throw new RuntimeException("Không tìm thấy phiên đấu giá");
        String itemId = auction.getItem().getId();
        auctionManager.removeAuction(auction);
        try {
            auctionDAO.delete(auctionId);
            new ItemDAO().delete(itemId);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa DB: " + e.getMessage());
        }
    }
    public void updateAuction(String auctionId, String newName, String newDescription, double newPrice) {
        Auction auction = auctionManager.findById(auctionId);
        if (auction == null) throw new RuntimeException("Không tìm thấy phiên đấu giá");
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
    }


    public void cancelAuction(String auctionId) {
        Auction auction = auctionManager.findById(auctionId);
        if (auction == null) throw new RuntimeException("Không tìm thấy phiên đấu giá");
        boolean ok = auctionManager.cancelAuction(auction);
        if (!ok) throw new RuntimeException("Không thể hủy phiên ở trạng thái: " + auction.getStatus());
    }

    public void markPaid(String auctionId) {
        Auction auction = auctionManager.findById(auctionId);
        if (auction == null) throw new RuntimeException("Không tìm thấy phiên đấu giá");
        boolean ok = auction.markPaid();
        if (!ok) throw new RuntimeException("Chỉ có thể thanh toán phiên FINISHED");
        try { auctionDAO.markPaid(auctionId); }
        catch (SQLException e) { throw new RuntimeException("Lỗi lưu DB: " + e.getMessage()); }
    }

    public void enableAutoBid(
            String auctionId,
            double maxAmount
    ) {

        User currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            throw new RuntimeException("Chưa đăng nhập");
        }

        Auction auction =
                auctionManager.findById(auctionId);

        if (auction == null) {
            throw new RuntimeException(
                    "Không tìm thấy phiên đấu giá"
            );
        }

        auction.enableAutoBid(
                currentUser.getName(),
                maxAmount
        );

        try {

            auctionDAO.updateAutoBid(
                    auctionId,
                    currentUser.getName(),
                    maxAmount
            );

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Lỗi database: "
                            + e.getMessage()
            );
        }
    }
}