package server.model;

import server.exception.AuctionClosedException;
import server.exception.InvalidBidException;
import server.model.item.Item;
import shared.protocol.AuctionEvent;
import shared.protocol.AuctionObserver;
import server.model.observer.AuctionSubject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction implements AuctionSubject {
    private ArrayList<BidTransaction> transactionHistory = new ArrayList<>();
    private String auctionId;
    private Item item;
    private double currentPrice;
    private String leadingBidder;
    private boolean isFinished;
    private LocalDateTime endTime;
    private String owner;
    private boolean cancelled = false;
    //them dsach observer
    private final List<AuctionObserver> observers = new ArrayList<>();

    public Auction(String auId, Item item, LocalDateTime endTime, String owner) {
        this.auctionId = auId;
        this.item = item;
        this.currentPrice = item.getBasePrice();
        this.leadingBidder = "None";
        this.isFinished = false;
        this.endTime = endTime;
        this.owner = owner;
    }
    public Auction(String auctionId,Item item,double currentPrice,String owner,String leadingBidder,boolean isFinished,LocalDateTime endTime){
        this.auctionId=auctionId;
        this.item=item;
        this.currentPrice=currentPrice;
        this.owner=owner;
        this.leadingBidder=leadingBidder;
        this.isFinished=isFinished;
        this.endTime=endTime;
    }

    @Override
    public void attach(AuctionObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void detach(AuctionObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(AuctionEvent event) {
        for (AuctionObserver observer : observers) {
            observer.onAuctionEvent(event);
        }
    }

    public synchronized void placeBid(String bidderName, double bidAmount) throws AuctionClosedException, InvalidBidException{
        if (isFinished) {
            throw new AuctionClosedException("Lỗi: Phiên đấu giá cho " + item.getName() + " đã kết thúc!");
        }
        if (bidAmount > currentPrice) {
            this.currentPrice = bidAmount;
            this.leadingBidder = bidderName;
            BidTransaction bid = new BidTransaction(auctionId, bidderName, bidAmount);
            transactionHistory.add(bid);

            AuctionEvent event = new AuctionEvent(AuctionEvent.Type.BID_PLACED, auctionId, bidderName, leadingBidder, bidAmount, currentPrice);
            notifyObservers(event); // thay the cho displayTransaction (de dam bao Auction chi thuc hien 1 task la placeBid( SRP ))
        } else {
            AuctionEvent event = new AuctionEvent(AuctionEvent.Type.BID_REJECTED, auctionId, bidderName, leadingBidder, bidAmount, currentPrice);
            notifyObservers(event);
            throw new InvalidBidException("Giá đặt phải lớn hơn giá gốc là: " + currentPrice);
        }
    }
    public synchronized void finishAuction() {
        this.isFinished = true;
        AuctionEvent event = new AuctionEvent(AuctionEvent.Type.AUCTION_ENDED, auctionId, leadingBidder, leadingBidder, currentPrice, currentPrice);
        notifyObservers(event);
    }

    public String getAuctionId() {
        return auctionId;
    }

    public Item getItem() {
        return item;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getLeadingBidder() {
        return leadingBidder;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public LocalDateTime getEndTime() { return endTime;}

    public String getItemName()    { return item.getName(); }

    public String getDescription() { return item.getDescription(); }

    public double getPrice()       { return item.getBasePrice(); }

    public String getOwner() { return owner; }

    // get dsach dau gia
    public ArrayList<BidTransaction> getTransactionHistory() {
        return transactionHistory;
    }

    public void setPrice(double price) {
        this.currentPrice = price;
    }
    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
    //test main
/*    public static void main(String[] args) {
          Item vision = ItemFactory.creatItem("vehicle","HD-VS-01","vision-sportVersion",10_000_000,"2025","29AE-57650");
          Auction auction = new Auction("AU001", vision, LocalDateTime.now().plusHours(2));
          vision.displayDetails();
          // Đăng ký các Observer thêm/bớt thoải mái mà không đụng Auction
          auction.attach(new BidLogObserver());
          auction.attach(new LeaderBoardObserver());
          auction.attach(new AuctionEndObserver());
          // cac luot dau gia
          auction.placeBid("An",   16_000_000);
          auction.placeBid("Bình", 14_000_000); // reject
          auction.placeBid("Cúc",  18_500_000);
          auction.placeBid("Đạt",  18_000_000); // reject
          ket thuc phien
          auction.finishAuction();
      }*/

