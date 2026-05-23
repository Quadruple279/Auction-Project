package server.dao;

import server.model.BidTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAO {
    public void save(BidTransaction transaction) throws SQLException {
        String sql = "INSERT INTO bid_transactions (auction_id,bidder_name,bid_amount,bid_time) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, transaction.getAuctionId());
            pstmt.setString(2, transaction.getBidderName());
            pstmt.setDouble(3, transaction.getBidAmount());
            pstmt.setTimestamp(4, Timestamp.valueOf(transaction.getBidTime()));
            pstmt.executeUpdate();
        }
    }

    public List<BidTransaction> findByAuctionId(String auctionId) throws SQLException {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_time ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            List<BidTransaction> trans = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery();) {
                while (rs.next()) {
                    String auctionid = rs.getString("auction_id");
                    String bidder_name = rs.getString("bidder_name");
                    double bid_amount = rs.getDouble("bid_amount");
                    LocalDateTime bid_time = rs.getTimestamp("bid_time").toLocalDateTime();
                    BidTransaction tran = new BidTransaction(auctionid, bidder_name, bid_amount, bid_time);
                    trans.add(tran);
                }
            }
            return trans;
        }
    }

    public List<BidTransaction> findByBidderName(String bidderName) throws SQLException {
        String sql = "SELECT * FROM bid_transactions " +
                "WHERE bidder_name = ? " +
                "ORDER BY bid_time DESC";

        List<BidTransaction> result = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, bidderName);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String auctionId   = rs.getString("auction_id");
                    String bidderName2 = rs.getString("bidder_name");
                    double bidAmount   = rs.getDouble("bid_amount");
                    LocalDateTime bidTime = rs.getTimestamp("bid_time")
                            .toLocalDateTime();

                    result.add(new BidTransaction(
                            auctionId, bidderName2, bidAmount, bidTime
                    ));
                }
            }
        }
        return result;
    }
}
