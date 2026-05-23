package server.dao;

import server.model.Auction;
import server.model.AuctionStatus;
import server.model.item.Item;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    private final ItemDAO itemDAO = new ItemDAO();

    public void save(Auction auction) throws SQLException {
        String sql = "INSERT INTO auctions (" +
                "id, item_id, current_price, owner, " +
                "leading_bidder, is_finished, status, end_time, " +
                "auto_bidder, max_auto_bid, auto_bid_increment" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auction.getAuctionId());
            pstmt.setString(2, auction.getItem().getId());
            pstmt.setDouble(3, auction.getCurrentPrice());
            pstmt.setString(4, auction.getOwner());
            String leadingBidder = "None".equals(auction.getLeadingBidder())
                    ? null : auction.getLeadingBidder();
            pstmt.setString(5, leadingBidder);
            pstmt.setBoolean(6, auction.isFinished());
            pstmt.setString(7, auction.getStatus().name());
            pstmt.setTimestamp(8, Timestamp.valueOf(auction.getEndTime()));
            pstmt.setString(9,
                    auction.getAutoBidder());

            pstmt.setDouble(10,
                    auction.getMaxAutoBid());
            pstmt.setDouble(
                    11,
                    auction.getAutoBidIncrement()
            );
            pstmt.executeUpdate();

        }
    }

    public void delete(String auctionId) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            // 1. Xóa bid_transactions trước
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM bid_transactions WHERE auction_id = ?")) {
                pstmt.setString(1, auctionId);
                pstmt.executeUpdate();
            }
            // 2. Rồi mới xóa auction
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "DELETE FROM auctions WHERE id = ?")) {
                pstmt.setString(1, auctionId);
                pstmt.executeUpdate();
            }
        }
    }

    public void updateAfterBid(String auctionId, double currentPrice, String leadingBidder)
            throws SQLException {
        // Cập nhật giá và trạng thái RUNNING khi có đầu tiên
        String sql = "UPDATE auctions SET current_price = ?, leading_bidder = ?, " +
                "status = CASE WHEN status = 'OPEN' THEN 'RUNNING' ELSE status END " +
                "WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, currentPrice);
            pstmt.setString(2, leadingBidder);
            pstmt.setString(3, auctionId);
            pstmt.executeUpdate();
        }
    }

    // Kết thúc phiên: đặt status = FINISHED va is_finished = TRUE
    public void finish(String auctionId) throws SQLException {
        String sql = "UPDATE auctions SET is_finished = TRUE, status = 'FINISHED' WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            pstmt.executeUpdate();
        }
    }

    // Hủy phiên: đặt status = CANCELED.
    public void cancel(String auctionId) throws SQLException {
        String sql = "UPDATE auctions SET status = 'CANCELED' WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            pstmt.executeUpdate();
        }
    }

    // Đánh dấu đã thanh toán: đặt status = PAID
    public void markPaid(String auctionId) throws SQLException {
        String sql = "UPDATE auctions SET status = 'PAID' WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            pstmt.executeUpdate();
        }
    }

    public void updateAutoBid(
            String auctionId,
            String autoBidder,
            double maxAutoBid,
            double increment
    ) throws SQLException {

        String sql =
                "UPDATE auctions " +
                        "SET auto_bidder = ?, " +
                        "max_auto_bid = ?, " +
                        "auto_bid_increment = ? " +
                        "WHERE id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt =
                        conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, autoBidder);
            pstmt.setDouble(2, maxAutoBid);
            pstmt.setDouble(3, increment);
            pstmt.setString(4, auctionId);

            pstmt.executeUpdate();
        }
    }

    public Auction findById(String auctionId) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapRowToAuction(rs);
            }
            return null;
        }
    }

    public List<Auction> findAll() throws SQLException {
        String sql = "SELECT * FROM auctions";
        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) auctions.add(mapRowToAuction(rs));
        }
        return auctions;
    }

    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        String auctionId    = rs.getString("id");
        String itemId       = rs.getString("item_id");
        double currentPrice = rs.getDouble("current_price");
        String owner        = rs.getString("owner");
        String leadingBidder = rs.getString("leading_bidder");
        double increment =
                rs.getDouble("auto_bid_increment");
        if (leadingBidder == null) leadingBidder = "None";
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
        String autoBidder =
                rs.getString("auto_bidder");

        double maxAutoBid =
                rs.getDouble("max_auto_bid");

        // Đọc status: nếu cột chưa tồn tại (DB cũ) thì suy ra từ is_finished
        AuctionStatus status;
        try {
            String statusStr = rs.getString("status");
            status = (statusStr != null) ? AuctionStatus.valueOf(statusStr) : deriveStatus(rs);
        } catch (SQLException e) {
            // Cột status chưa được migrate — suy ra từ is_finished
            status = deriveStatus(rs);
        } catch (IllegalArgumentException e) {
            status = AuctionStatus.OPEN;
        }

        Item item = itemDAO.findById(itemId);
        Auction auction = new Auction(
                auctionId,
                item,
                currentPrice,
                owner,
                leadingBidder,
                status,
                endTime
        );

        if (autoBidder != null) {

            auction.enableAutoBid(
                    autoBidder,
                    maxAutoBid,
                    increment
            );
        }

        return auction;
    }

    // Suy ra AuctionStatus từ cột is_finished khi chưa có cột. */
    private AuctionStatus deriveStatus(ResultSet rs) {
        try {
            return rs.getBoolean("is_finished") ? AuctionStatus.FINISHED : AuctionStatus.OPEN;
        } catch (SQLException e) {
            return AuctionStatus.OPEN;
        }
    }
}