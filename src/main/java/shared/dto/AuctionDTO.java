package shared.dto;

public class AuctionDTO {
    private String auctionId;
    private String itemName;
    private String description;
    private double price;         // giá khởi điểm
    private double currentPrice;  // giá hiện tại
    private String leadingBidder;
    private String owner;
    private boolean finished;
    private String endTime;   // dùng String để tránh phức tạp khi serialize LocalDateTime

    // constructor không tham số (Jackson cần để deserialize)
    public AuctionDTO() {
    }

    // ② Constructor tiện lợi — dùng khi server tạo DTO từ Auction
    public AuctionDTO(String auctionId, String itemName, String description,
                      double price, double currentPrice,
                      String leadingBidder, String owner, boolean finished,String endTime) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.description = description;
        this.price = price;
        this.currentPrice = currentPrice;
        this.leadingBidder = leadingBidder;
        this.owner = owner;
        this.finished = finished;
        this.endTime=endTime;
    }

    // ③ Getters — Jackson dùng để serialize (Object → JSON)
    public String getAuctionId() {
        return auctionId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getLeadingBidder() {
        return leadingBidder;
    }

    public String getOwner() {
        return owner;
    }

    public boolean isFinished() {
        return finished;
    }

    public String getEndTime() {
        return endTime;
    }

    // ④ Setters — Jackson dùng để deserialize (JSON → Object)
    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setLeadingBidder(String leadingBidder) {
        this.leadingBidder = leadingBidder;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}


