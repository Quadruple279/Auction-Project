package server.model;

import server.exception.AuctionClosedException;
import server.exception.InvalidBidException;
import server.model.item.Item;
import shared.protocol.AuctionEvent;
import shared.protocol.AuctionObserver;
import server.model.observer.AuctionSubject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.time.temporal.ChronoUnit;
import java.util.PriorityQueue;

import javafx.application.Platform;

public class Auction implements AuctionSubject {
    private ArrayList<BidTransaction> transactionHistory = new ArrayList<>();
    private String auctionId;
    private Item item;
    private double currentPrice;
    private String leadingBidder;
    private String autoBidder;
    private double maxAutoBid;
    private double autoBidIncrement = 10;
    private AuctionStatus status;
    private LocalDateTime endTime;
    private String owner;
    private boolean cancelled = false;


    private boolean isFinished; // boolean cũ dùng cho constructor cũ (code mới dùng AuctionStatus)

    // ===== AUTO BIDDING — PriorityQueue hỗ trợ nhiều auto-bidder =====
    private static class AutoBidEntry implements Comparable<AutoBidEntry> {
        String bidderName;
        double maxBid;
        double increment;

        AutoBidEntry(String bidderName, double maxBid, double increment) {
            this.bidderName = bidderName;
            this.maxBid     = maxBid;
            this.increment  = increment;
        }

        @Override
        public int compareTo(AutoBidEntry other) {
            return Double.compare(other.maxBid, this.maxBid); // maxBid cao hơn = ưu tiên hơn
        }
    }

    private final PriorityQueue<AutoBidEntry> autoBidQueue = new PriorityQueue<>();
    //Tạo danh sách observer
    private final List<AuctionObserver> observers = new ArrayList<>();

    public Auction(
            String auId,
            Item item,
            LocalDateTime endTime,
            String owner
    ) {

        this.auctionId = auId;
        this.item = item;
        this.currentPrice = item.getBasePrice();

        // mặc định chưa ai bid
        this.leadingBidder = "None";

        this.status = AuctionStatus.OPEN;
        this.endTime = endTime;
        this.owner = owner;
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
                    "Phiên đấu giá cho " + item.getName() + " đã hết thời gian!");
        }

        if (status == AuctionStatus.FINISHED || status == AuctionStatus.PAID) {
            throw new AuctionClosedException(
                    "Phiên đấu giá cho " + item.getName() + " đã kết thúc!");
        }
        if (status == AuctionStatus.CANCELED) {
            throw new AuctionClosedException(
                    "Phiên đấu giá cho " + item.getName() + " đã bị hủy!");
        }
        if (bidderName.equals(leadingBidder)) {
            throw new InvalidBidException("Bạn đang là người đặt giá cao nhất");
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

            // ===== AUTO BIDDING với PriorityQueue =====
            // FIX: PriorityQueue iterator không đảm bảo thứ tự heap — phải poll() để lấy đúng top entry
            // Lấy entry cao nhất mà không phải người vừa bid
            AutoBidEntry top = null;
            List<AutoBidEntry> skipped = new ArrayList<>();
            while (!autoBidQueue.isEmpty()) {
                AutoBidEntry candidate = autoBidQueue.poll();
                if (!candidate.bidderName.equals(bidderName)) {
                    top = candidate;
                    autoBidQueue.add(candidate); // đưa lại vào queue
                    break;
                }
                skipped.add(candidate);
            }
            autoBidQueue.addAll(skipped); // đưa lại các entry đã skip

            if (top != null
                    && currentPrice + top.increment <= top.maxBid
                    && status != AuctionStatus.FINISHED
                    && status != AuctionStatus.CANCELED) {

                double autoBidPrice = currentPrice + top.increment;
                currentPrice  = autoBidPrice;
                leadingBidder = top.bidderName;

                BidTransaction autoBidTx = new BidTransaction(auctionId, top.bidderName, autoBidPrice);
                transactionHistory.add(autoBidTx);

                AuctionEvent autoEvent = new AuctionEvent(
                        AuctionEvent.Type.BID_PLACED,
                        auctionId, top.bidderName, top.bidderName,
                        autoBidPrice, currentPrice);
                notifyObservers(autoEvent);

                System.out.println("Auto-bid by " + top.bidderName + ": " + autoBidPrice);
            }

            if (getRemainingSeconds() <= 30) {
                extendAuctionTime();
            }
        }


        else {
            AuctionEvent event = new AuctionEvent(
                    AuctionEvent.Type.BID_REJECTED, auctionId, bidderName,
                    leadingBidder, bidAmount, currentPrice);
            notifyObservers(event);
            throw new InvalidBidException("Giá đặt phải lớn hơn giá hiện tại là: " + currentPrice);
        }
    }

    // Kết thúc phiên: OPEN/RUNNING -> FINISHED
    public synchronized void finishAuction() {
        if (status == AuctionStatus.FINISHED || status == AuctionStatus.PAID
                || status == AuctionStatus.CANCELED) return;

        this.status = AuctionStatus.FINISHED;

        autoBidQueue.clear();

        AuctionEvent event = new AuctionEvent(AuctionEvent.Type.AUCTION_ENDED, auctionId,
                leadingBidder, leadingBidder, currentPrice, currentPrice);

        notifyObservers(event);
    }

    // Hủy phiên: OPEN/RUNNING -> CANCELED
    public synchronized boolean cancelAuction() {
        if (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING) {
            this.status = AuctionStatus.CANCELED;
            autoBidQueue.clear();

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

    public String getAutoBidder() {
        AutoBidEntry top = autoBidQueue.peek();
        return top != null ? top.bidderName : null;
    }

    public double getMaxAutoBid() {
        AutoBidEntry top = autoBidQueue.peek();
        return top != null ? top.maxBid : 0;
    }

    // Lấy danh sách đấu giá
    public List<BidTransaction> getTransactionHistory() {
        return List.copyOf(transactionHistory);
    }

    // ── Convenience helpers (Tương thích ngược) ───────────────────────────────

    // return true nếu trạng thái là FINISHED hoặc PAID
    public boolean isFinished() {
        return status == AuctionStatus.FINISHED || status == AuctionStatus.PAID;
    }

    // return true nếu trạng thái là CANCELED
    public boolean isCancelled() {
        return status == AuctionStatus.CANCELED;
    }

    // return true nếu phiên vẫn đang nhận bid mới
    public boolean isActive() {
        return (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING)
                && !LocalDateTime.now().isAfter(endTime);
    }

    public long getRemainingSeconds() {
        return LocalDateTime.now().until(endTime, ChronoUnit.SECONDS);
    }

    public void extendAuctionTime() {
        endTime = endTime.plusSeconds(10);
        AuctionEvent event =
                new AuctionEvent(
                        AuctionEvent.Type.TIME_EXTENDED,
                        auctionId,
                        endTime.toEpochSecond(ZoneOffset.UTC)
                );

        notifyObservers(event);

        System.out.println("Auction được gia hạn thêm 10 giây!");
        // Reschedule lại scheduler theo endTime mới
        AuctionManager.getInstance().rescheduleFinish(this); // ← THÊM DÒNG NÀY
    }

    public synchronized void enableAutoBid(String bidderName, double maxAmount, double increment) {
        // Nếu người này đã đăng ký trước thì xóa entry cũ trước khi thêm mới
        autoBidQueue.removeIf(e -> e.bidderName.equals(bidderName));
        autoBidQueue.add(new AutoBidEntry(bidderName, maxAmount, increment));
        System.out.println("Auto-bid enabled for " + bidderName
                + " max: " + maxAmount + " increment: " + increment);
        // Nếu người khác đang dẫn đầu → bid ngay lập tức không cần chờ bid thủ công
        if (!bidderName.equals(leadingBidder)
                && currentPrice + increment <= maxAmount
                && status != AuctionStatus.FINISHED
                && status != AuctionStatus.CANCELED) {

            double autoBidPrice = currentPrice + increment;
            currentPrice  = autoBidPrice;
            leadingBidder = bidderName;

            transactionHistory.add(new BidTransaction(auctionId, bidderName, autoBidPrice));

            notifyObservers(new AuctionEvent(
                    AuctionEvent.Type.BID_PLACED,
                    auctionId, bidderName, bidderName,
                    autoBidPrice, currentPrice));

            System.out.println("Auto-bid immediate by " + bidderName + ": " + autoBidPrice);
        }
    }

    public void disableAutoBid(String bidderName) {
        autoBidQueue.removeIf(e -> e.bidderName.equals(bidderName));
        System.out.println("Auto-bid disabled for " + bidderName);
    }

    public void setPrice(double price) {
        this.currentPrice = price;
    }

    // Dùng cancelAuction() thay thế
    @Deprecated
    public void setCancelled(boolean cancelled) {
        if (cancelled) this.status = AuctionStatus.CANCELED;
    }
    public double getAutoBidIncrement() {
        AutoBidEntry top = autoBidQueue.peek();
        return top != null ? top.increment : 10;
    }
}