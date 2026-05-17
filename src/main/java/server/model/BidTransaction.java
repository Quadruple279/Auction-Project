package server.model;

import java.time.LocalDateTime;

public class BidTransaction {
    private String bidderName;
    private double bidAmount;
    private LocalDateTime bidTime;  //Thời điểm đặt giá
    private String auctionId;       //id của phiên đấu giá liên quan

    public BidTransaction(String auctionId, String bidderName, double bidAmount) {
        this.auctionId = auctionId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();//lấy thời điểm hiện tại
    }
    public BidTransaction(String auctionId, String bidderName, double bidAmount, LocalDateTime bidTime) {
        this.auctionId = auctionId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;   // Lay tu database
    }

    // Getters để sau này có thể thống kê hoặc hiển thị lên bảng lịch sử
    public String getBidderName() { return bidderName; }
    public double getBidAmount() { return bidAmount; }
    public LocalDateTime getBidTime() { return bidTime; }
    public String getAuctionId() { return auctionId; }
}
