package server.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    //Hiển thị thông tin giao dịch theo định dạng đẹp
    public void displayTransaction() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        System.out.println(String.format("[%s] Người dùng %s đã đặt giá %.2f cho phiên %s",
                bidTime.format(formatter), bidderName, bidAmount, auctionId));
    }
    //Getters Để sau này có thể thống kê hoặc hiển thị lên bảng lịch sử
    public String getBidderName() { return bidderName; }
    public double getBidAmount() { return bidAmount; }
    public LocalDateTime getBidTime() { return bidTime; }
    public String getAuctionId() { return auctionId; }
}
