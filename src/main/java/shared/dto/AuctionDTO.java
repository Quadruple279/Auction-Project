package shared.dto;

public class AuctionDTO {
    private String auctionId;
    private String itemName;
    private String description;
    private double price;         // giá khởi điểm
    private double currentPrice;  // gias hiện tại
    private String leadingBidder;
    private String owner;
    private boolean finished;
    private String status;        // AuctionStatus.name() — OPEN/RUNNING/FINISHED/PAID/CANCELED
    private String endTime;

    // Jackson cần constructor 0 tham số đẻ deserialize
    public AuctionDTO() {}

    public AuctionDTO(String auctionId, String itemName, String description,
                      double price, double currentPrice,
                      String leadingBidder, String owner,
                      boolean finished, String status, String endTime) {
        this.auctionId    = auctionId;
        this.itemName     = itemName;
        this.description  = description;
        this.price        = price;
        this.currentPrice = currentPrice;
        this.leadingBidder = leadingBidder;
        this.owner        = owner;
        this.finished     = finished;
        this.status       = status;
        this.endTime      = endTime;
    }

    // Getters
    public String getAuctionId()    { return auctionId; }
    public String getItemName()     { return itemName; }
    public String getDescription()  { return description; }
    public double getPrice()        { return price; }
    public double getCurrentPrice() { return currentPrice; }
    public String getLeadingBidder(){ return leadingBidder; }
    public String getOwner()        { return owner; }
    public boolean isFinished()     { return finished; }
    public String getStatus()       { return status; }
    public String getEndTime()      { return endTime; }

    // Setters
    public void setAuctionId(String auctionId)      { this.auctionId = auctionId; }
    public void setItemName(String itemName)         { this.itemName = itemName; }
    public void setDescription(String description)   { this.description = description; }
    public void setPrice(double price)               { this.price = price; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setLeadingBidder(String leadingBidder){ this.leadingBidder = leadingBidder; }
    public void setOwner(String owner)               { this.owner = owner; }
    public void setFinished(boolean finished)        { this.finished = finished; }
    public void setStatus(String status)             { this.status = status; }
    public void setEndTime(String endTime)           { this.endTime = endTime; }
}

