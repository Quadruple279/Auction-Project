package server.dao;

import server.model.Auction;
import server.model.item.Item;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    private final ItemDAO itemDAO = new ItemDAO();

    public void save(Auction auction) throws SQLException {
        String sql = "INSERT INTO auctions (id, item_id, current_price, " +
                "leading_bidder, is_finished, end_time) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, auction.getAuctionId());
        pstmt.setString(2, auction.getItem().getId());
        pstmt.setDouble(3, auction.getCurrentPrice());
        // Thay vì set thẳng string "None"
        String leadingBidder = auction.getLeadingBidder().equals("None")
                ? null
                : auction.getLeadingBidder();
        pstmt.setString(4, leadingBidder);

        pstmt.setBoolean(5, auction.isFinished());
        pstmt.setTimestamp(6, Timestamp.valueOf(auction.getEndTime()));
        pstmt.executeUpdate();
    }

    public void updateAfterBid(String auctionId, double currentPrice, String leadingBidder) throws SQLException {
        String sql = "UPDATE auctions SET current_price = ?, leading_bidder = ? WHERE id = ?";

        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setDouble(1, currentPrice);
        pstmt.setString(2, leadingBidder);
        pstmt.setString(3, auctionId);
        pstmt.executeUpdate();
    }

    public void finish(String auctionId) throws SQLException {
        String sql = "UPDATE auctions SET is_finished = TRUE WHERE id = ?";

        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, auctionId);
        pstmt.executeUpdate();
    }

    public Auction findById(String auctionId) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, auctionId);
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            return mapRowToAuction(rs);
        }
        return null;
    }

    public List<Auction> findAll() throws SQLException {
        String sql = "SELECT * FROM auctions";
        List<Auction> auctions = new ArrayList<>();

        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            auctions.add(mapRowToAuction(rs));
        }
        return auctions;
    }

    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        String auctionId     = rs.getString("id");
        String itemId        = rs.getString("item_id");
        double currentPrice  = rs.getDouble("current_price");
        // Đọc có thể null → dùng "None" làm mặc định
        String leadingBidder = rs.getString("leading_bidder");
        if (leadingBidder == null) leadingBidder = "None";
        boolean isFinished   = rs.getBoolean("is_finished");
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();

        Item item = itemDAO.findById(itemId);

        return new Auction(auctionId, item, currentPrice, leadingBidder, isFinished, endTime);
    }
}
