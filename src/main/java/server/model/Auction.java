package server.model;

import server.model.item.Item;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class Auction {
    private ArrayList<BidTransaction> transactionHistory = new ArrayList<>();
    private String auctionId;
    private Item item;
    private double currentPrice;
    private String leadingBidder;//nguoi tra gia cao nhat
    private boolean isFinished;
    private LocalDateTime endTime;
    public Auction(String auId,Item item,LocalDateTime endTime){
        this.auctionId=auId;
        this.item=item;
        this.currentPrice = item.getBasePrice();
        this.leadingBidder = "None";
        this.isFinished=false;
        this.endTime=endTime;
    }
    //Xu li khi co nguoi dat gia moi
    public boolean placeBid(String bidderName,double bidAmount){
        if (isFinished){
            System.out.println("Phiên đấu giá đã kết thúc!");
            return false;
        }
        if(bidAmount > currentPrice){
            this.currentPrice = bidAmount;
            this.leadingBidder = bidderName;
            BidTransaction bid = new BidTransaction(auctionId,bidderName,bidAmount);
            transactionHistory.add(bid);
            bid.displayTransaction();//in ra trang thai
            return true;
        }
        else{
            System.out.println("Giá đặt phải cao hơn giá hiện tại: "+currentPrice);
            return false;
        }
    }
    //ket thuc phien dau gia
    public void finishAuction() {
        this.isFinished = true;
        System.out.println("--- PHIÊN ĐẤU GIÁ KẾT THÚC ---");
        System.out.println("Người thắng cuộc: " + leadingBidder + " với mức giá: " + currentPrice);
    }
    //method getter
    public String getAuctionId() { return auctionId; }
    public Item getItem() { return item; }
    public double getCurrentPrice() { return currentPrice; }
    public String getLeadingBidder() { return leadingBidder; }
    public boolean isFinished() { return isFinished; }
    public String getItemName() { return item.getName();}
    public String getDescription() { return item.getDescription();}
    public double getPrice() { return item.getBasePrice();}
    // get dsach dau gia
    public ArrayList<BidTransaction> getTransactionHistory() {
        return transactionHistory;
    }
}
