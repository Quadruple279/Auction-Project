package server.model;
//Đây là lớp sử dụng để gói thông tin của 1 lượt đặt giá lại(thay vì nhiều tham só thì lúc này nó thành 1 object
public class AuctionEvent {
    public enum Type{
        BID_PLACED,// chap nhan luot bid
        BID_REJECTED,// tu choi luot bid
        AUCTION_ENDED//ket thuc phien
    }
    private final Type type;
    private final String auctionId,bidderName,leadingBidder;
    private final double bidAmount,currentPrice;
    public AuctionEvent(Type type,String auctionId,String bidderName,
                        String leadingBidder,double bidAmount,double currentPrice){
        this.type=type;
        this.auctionId=auctionId;
        this.bidderName=bidderName;
        this.leadingBidder=leadingBidder;
        this.bidAmount=bidAmount;
        this.currentPrice=currentPrice;
    }
    //Cac ham getter
    public Type getType(){return type;}
    public String getAuctionId(){return auctionId;}
    public String getBidderName(){return bidderName;}
    public String getLeadingBidder(){return leadingBidder;}
    public double getBidAmount(){return bidAmount;}
    public double getCurrentPrice(){return currentPrice;}
}
