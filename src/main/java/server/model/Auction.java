package server.model;

import server.model.item.Item;
import server.model.item.ItemFactory;
import server.model.observer.AuctionObserver;
import server.model.observer.AuctionSubject;
import server.model.observer.subObservers.AuctionEndObserver;
import server.model.observer.subObservers.BidLogObserver;
import server.model.observer.subObservers.LeaderBoardObserver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction implements AuctionSubject {
    private ArrayList<BidTransaction> transactionHistory = new ArrayList<>();
    private String auctionId;
    private Item item;
    private double currentPrice;
    private String leadingBidder;//nguoi tra gia cao nhat
    private boolean isFinished;
    private LocalDateTime endTime;
    //them dsach observer
    private final List<AuctionObserver> observers = new ArrayList<>();

    public Auction(String auId, Item item, LocalDateTime endTime) {
        this.auctionId = auId;
        this.item = item;
        this.currentPrice = item.getBasePrice();
        this.leadingBidder = "None";
        this.isFinished = false;
        this.endTime = endTime;
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

    //Xu li khi co nguoi dat gia moi
    public boolean placeBid(String bidderName, double bidAmount) {
        if (isFinished) {
            System.out.println("Phiên đấu giá đã kết thúc!");
            return false;
        }
        if (bidAmount > currentPrice) {
            this.currentPrice = bidAmount;
            this.leadingBidder = bidderName;
            BidTransaction bid = new BidTransaction(auctionId, bidderName, bidAmount);
            transactionHistory.add(bid);
            //
            AuctionEvent event = new AuctionEvent(AuctionEvent.Type.BID_PLACED, auctionId,
                    bidderName, leadingBidder, bidAmount, currentPrice);
            notifyObservers(event); // thay the cho displayTransaction (de dam bao Auction chi thuc hien 1 task la placeBid( SRP ))
            return true;
        } else {
            AuctionEvent event = new AuctionEvent(AuctionEvent.Type.BID_REJECTED, auctionId,
                    bidderName, leadingBidder, bidAmount, currentPrice);
            notifyObservers(event);
            return false;
        }
    }

    //ket thuc phien dau gia
    public void finishAuction() {
        this.isFinished = true;
        AuctionEvent event = new AuctionEvent(AuctionEvent.Type.AUCTION_ENDED, auctionId, leadingBidder,
                leadingBidder, currentPrice, currentPrice);
        notifyObservers(event);
    }

    //method getter
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

    // get dsach dau gia
    public ArrayList<BidTransaction> getTransactionHistory() {
        return transactionHistory;
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

