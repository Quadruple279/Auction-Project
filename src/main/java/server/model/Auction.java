package server.model;

import server.exception.AuctionClosedException;
import server.exception.InvalidBidException;
import server.model.item.Item;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class Auction {
    private ArrayList<BidTransaction> transactionHistory = new ArrayList<>();
    private String auctionId;
    private Item item;
    private double currentPrice;
    private String leadingBidder;
    private boolean isFinished;
    private LocalDateTime endTime;

    public Auction(String auId, Item item, LocalDateTime endTime){
        this.auctionId = auId;
        this.item = item;
        this.currentPrice = item.getBasePrice();
        this.leadingBidder = null;
        this.isFinished = false;
        this.endTime = endTime;
    }

    public synchronized void placeBid(String bidderName, double bidAmount) throws AuctionClosedException, InvalidBidException {
        if (isFinished){
            throw new AuctionClosedException("[X] Phiên đấu giá đã kết thúc.");
        }
        if (bidAmount <= currentPrice){
            throw new InvalidBidException("[X] Giá đặt hiện tại (" + bidAmount + ") thấp hơn giá đang được đấu (" + currentPrice + ")");
        }

        // Xóa if-else thay bằng if(!...) cho code trông gọn hơn.
        this.currentPrice = bidAmount;
        this.leadingBidder = bidderName;

        BidTransaction bid = new BidTransaction(auctionId,bidderName,bidAmount);
        transactionHistory.add(bid);

        bid.displayTransaction();
    }

    public synchronized void finishAuction(){
        this.isFinished = true;
        System.out.println("--- PHIÊN ĐẤU GIÁ KẾT THÚC ---");
        System.out.println("Người thắng cuộc: " + leadingBidder);
        System.out.println("Vật phẩm đấu giá: " + item);
        System.out.println("Mức đấu giá: " + currentPrice);
    }

    public String getAuctionId(){
        return auctionId;
    }
    public Item getItem(){
        return item;
    }
    public double getCurrentPrice(){
        return currentPrice;
    }
    public String getLeadingBidder(){
        return leadingBidder;
    }
    public boolean isFinished(){
        return isFinished;
    }
    public String getItemName(){
        return item.getName();
    }
    public String getDescription(){
        return item.getDescription();
    }
    public double getPrice(){
        return item.getBasePrice();
    }
    public ArrayList<BidTransaction> getTransactionHistory(){
        return transactionHistory;
    }
}
