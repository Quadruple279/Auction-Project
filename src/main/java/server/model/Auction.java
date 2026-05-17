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
    private AuctionStatus status;
    private LocalDateTime endTime;
    private String owner;
    private boolean cancelled = false;

    private boolean isFinished; // boolean cũ dùng cho constructor cũ (code mới dùng AuctionStatus)

    //Tạo danh sách observer
    private final List<AuctionObserver> observers = new ArrayList<>();

    public Auction(String auId, Item item, LocalDateTime endTime, String owner) {
        this.auctionId     = auId;
        this.item          = item;
        this.currentPrice  = item.getBasePrice();
        this.leadingBidder = "None";
        this.status        = AuctionStatus.OPEN;
        this.endTime       = endTime;
        this.owner         = owner;
    }

    // Constructor dùng khi load từ database
    public Auction(String auctionId, Item item, double currentPrice, String owner,
                   String leadingBidder, AuctionStatus status, LocalDateTime endTime) {
        this.auctionId     = auctionId;
        this.item          = item;
        this.currentPrice  = currentPrice;
        this.owner         = owner;
        this.leadingBidder = leadingBidder;
        this.status        = (status != null) ? status : AuctionStatus.OPEN;
        this.endTime       = endTime;
    }

    // Constructor cũ, dùng để tương thích ngưược với code cũ mà dùng boolean isFinished
    // (code mới dùng AuctionStatus status thay vì boolean isFinished)
    @Deprecated
    public Auction(String auctionId, Item item, double currentPrice, String owner,
                   String leadingBidder, boolean isFinished, LocalDateTime endTime){
        this.auctionId     = auctionId;
        this.item          = item;
        this.currentPrice  = currentPrice;
        this.owner         = owner;
        this.leadingBidder = leadingBidder;
        this.isFinished    = isFinished;
        this.endTime       = endTime;
    }

    @Override
    public void attach(AuctionObserver observer) {
        if (!observers.contains(observer)) observers.add(observer);
    }

    @Override
    public void detach(AuctionObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(AuctionEvent event) {
        for (AuctionObserver observer : observers) observer.onAuctionEvent(event);
    }

    public synchronized void placeBid(String bidderName, double bidAmount)
            throws AuctionClosedException, InvalidBidException{

        // Kiểm tra thời gian phiên đấu kết thúc, không phụ thuộc vào scheduler nữa
        if (LocalDateTime.now().isAfter(endTime)) {
            if (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING) {
                this.status = AuctionStatus.FINISHED;
            }
            throw new AuctionClosedException(
                    "Lỗi: Phiên đấu giá cho " + item.getName() + " đã hết thời gian!");
        }

        if (status == AuctionStatus.FINISHED || status == AuctionStatus.PAID) {
            throw new AuctionClosedException(
                    "Lỗi: Phiên đấu giá cho " + item.getName() + " đã kết thúc!");
        }
        if (status == AuctionStatus.CANCELLED) {
            throw new AuctionClosedException(
                    "Lỗi: Phiên đấu giá cho " + item.getName() + " đã bị hủy!");
        }

        if (bidAmount > currentPrice) {
            this.currentPrice = bidAmount;
            this.leadingBidder = bidderName;

            // OPEN -> RUNNING khi có bid đầu tiên thành công
            if (this.status == AuctionStatus.OPEN) {
                this.status = AuctionStatus.RUNNING;
            }

            BidTransaction bid = new BidTransaction(auctionId, bidderName, bidAmount);
            transactionHistory.add(bid);

            AuctionEvent event = new AuctionEvent(
                    AuctionEvent.Type.BID_PLACED, auctionId, bidderName,
                    leadingBidder, bidAmount, currentPrice);
            notifyObservers(event); // thay thế cho displayTransaction (để đảm bảo Auction chỉ thực hiện 1 task placeBid( SRP ))
        } else {
            AuctionEvent event = new AuctionEvent(
                    AuctionEvent.Type.BID_REJECTED, auctionId, bidderName,
                    leadingBidder, bidAmount, currentPrice);
            notifyObservers(event);
            throw new InvalidBidException("Giá đặt phải lớn hơn giá hiện tại là: " + currentPrice);
        }
    }

    // Kết thúc phiên: OPEN/RUNNING -> FINISHED
    public synchronized void finishAuction() {
        if (status == AuctionStatus.FINISHED || status == AuctionStatus.PAID
                || status == AuctionStatus.CANCELLED) return;

        this.status = AuctionStatus.FINISHED;


        AuctionEvent event = new AuctionEvent(AuctionEvent.Type.AUCTION_ENDED, auctionId,
                leadingBidder, leadingBidder, currentPrice, currentPrice);

        notifyObservers(event);
    }

    // Hủy phiên: OPEN/RUNNING -> CANCELED
    public synchronized boolean cancelAuction() {
        if (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING) {
            this.status = AuctionStatus.CANCELLED;

            AuctionEvent event = new AuctionEvent(
                    AuctionEvent.Type.AUCTION_ENDED, auctionId, leadingBidder,
                    leadingBidder, currentPrice, currentPrice);

            notifyObservers(event);
            return true;
        }
        return false;
    }

    // Đánh dấu phiên đấu đã thanh toán: FINISHED -> PAID
    public synchronized boolean markPaid() {
        if (status == AuctionStatus.FINISHED) {
            this.status = AuctionStatus.PAID;
            return true;
        }
        return false;
    }

    public String        getAuctionId()     {return auctionId;}
    public Item          getItem()          {return item;}
    public double        getCurrentPrice()  {return currentPrice;}
    public String        getLeadingBidder() {return leadingBidder;}
    public LocalDateTime getEndTime()       {return endTime;}
    public String        getItemName()      {return item.getName(); }
    public String        getDescription()   {return item.getDescription();}
    public double        getPrice()         {return item.getBasePrice();}
    public String        getOwner()         {return owner;}
    public AuctionStatus getStatus()        {return status;}

    // Lấy danh sách đấu giá
    public ArrayList<BidTransaction> getTransactionHistory() {
        return transactionHistory;
    }

    // ── Convenience helpers (Tương thích ngược) ───────────────────────────────

    // return true nếu trạng thái là FINISHED hoặc PAID
    public boolean isFinished() {
        return status == AuctionStatus.FINISHED || status == AuctionStatus.PAID;
    }

    // return true nếu trạng thái là CANCELED
    public boolean isCancelled() {
        return status == AuctionStatus.CANCELLED;
    }

    // return true nếu phiên vẫn đang nhận bid mới
    public boolean isActive() {
        return (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING)
                && !LocalDateTime.now().isAfter(endTime);
    }

    public void setPrice(double price) {
        this.currentPrice = price;
    }

    // Dùng cancelAuction() thay thế
    @Deprecated
    public void setCancelled(boolean cancelled) {
        if (cancelled) this.status = AuctionStatus.CANCELLED;
    }
}