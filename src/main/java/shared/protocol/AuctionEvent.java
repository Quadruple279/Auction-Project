package shared.protocol;

// Đây là lớp sử dụng để gói thông tin của 1 lượt đặt giá lại (thay vì nhiều tham số thì lúc này nó thành 1 object)
public class AuctionEvent {
    public enum Type{
        BID_PLACED, // chấp nhận lượt bid
        BID_REJECTED, // từ chối lượt bid
        AUCTION_ENDED, // kết thúc phiên
        NEW_AUCTION
    }
    private final Type type;
    private final String auctionId, bidderName, leadingBidder;
    private final double bidAmount, currentPrice;

    public AuctionEvent(Type type,String auctionId,String bidderName, String leadingBidder,double bidAmount,double currentPrice){
        this.type = type;
        this.auctionId = auctionId;
        this.bidderName = bidderName;
        this.leadingBidder = leadingBidder;
        this.bidAmount = bidAmount;
        this.currentPrice = currentPrice;
    }

    // Các hàm getter
    public Type getType(){
        return type;
    }
    public String getAuctionId(){
        return auctionId;
    }
    public String getBidderName(){
        return bidderName;
    }
    public String getLeadingBidder(){
        return leadingBidder;
    }
    public double getBidAmount(){
        return bidAmount;
    }
    public double getCurrentPrice(){
        return currentPrice;
    }
}
