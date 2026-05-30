package shared.dto;

public class BidHistoryDTO {
    private String auctionId;
    private double bidAmount;
    private String bidTime;

    public BidHistoryDTO() {}

    public BidHistoryDTO(String auctionId, double bidAmount, String bidTime) {
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.bidTime   = bidTime;
    }

    public String getAuctionId() { return auctionId; }
    public double getBidAmount() { return bidAmount; }
    public String getBidTime()   { return bidTime; }
}

