package server.model;

import server.dao.AuctionDAO;
import server.model.AdminEventBus;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class AuctionManager {
    private static AuctionManager instance;
    private ArrayList<Auction> auctionList;

    // Scheduler tự động kết thúc phiên khi hết giờ
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    // Callback để broadcast AUCTION_ENDED đến tất cả client
    // Được ClientHandler đăng ký sau khi server khởi động
    private Consumer<String> auctionEndedBroadcaster;

    private AuctionManager() {
        auctionList = new ArrayList<>();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) instance = new AuctionManager();
        return instance;
    }

    /**
     * Dang ky ham broadcast de AuctionManager co the thong bao
     * ket thuc phien toi tat ca client (ke ca chua subscribe).
     */
    public void setAuctionEndedBroadcaster(Consumer<String> broadcaster) {
        this.auctionEndedBroadcaster = broadcaster;
    }

    public synchronized void addAuction(Auction auction) {
        auctionList.add(auction);
        scheduleFinish(auction);
    }

    public synchronized void removeAuction(Auction auction) {
        auctionList.remove(auction);
    }

    public synchronized void updateAuction(Auction updatedAuction) {
        for (int i = 0; i < auctionList.size(); i++) {
            if (auctionList.get(i).getAuctionId().equals(updatedAuction.getAuctionId())) {
                auctionList.set(i, updatedAuction);
                return;
            }
        }
    }

    /**
     * Huy phien dau gia: OPEN/RUNNING -> CANCELED.
     * Viec thong bao toi observer (subscribers) duoc xu ly trong Auction.cancelAuction().
     */
    public boolean cancelAuction(Auction auction) {
        if (auction == null) return false;
        boolean cancelled = auction.cancelAuction();
        if (cancelled) {
            try {
                new AuctionDAO().cancel(auction.getAuctionId());
            } catch (SQLException e) {
                System.err.println("[AuctionManager] Loi luu DB khi huy phien: " + e.getMessage());
            }
            AdminEventBus.getInstance().publish(
                    AdminEventBus.EVENT_AUCTION_CANCELED,
                    auction.getAuctionId() + " — " + auction.getItem().getName()
            );
            broadcastEnded(auction.getAuctionId());
        }
        return cancelled;
    }

    public ArrayList<Auction> getAuctionList() { return auctionList; }

    public synchronized Auction findById(String auctionId) {
        for (Auction a : auctionList) {
            if (a.getAuctionId().equals(auctionId)) return a;
        }
        return null;
    }

    // ─── Scheduler ────────────────────────────────────────────────

    private void scheduleFinish(Auction auction) {
        if (auction.isFinished() || auction.isCancelled()) return;

        long delay = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        if (delay <= 0) {
            finishAndSave(auction);
            return;
        }
        scheduler.schedule(() -> finishAndSave(auction), delay, TimeUnit.MILLISECONDS);
        System.out.println("[Scheduler] Da len lich ket thuc phien " + auction.getAuctionId()
                + " sau " + delay + " ms");
    }
    public void rescheduleFinish(Auction auction) {
        scheduleFinish(auction);
    }


    private void finishAndSave(Auction auction) {
        if (auction.isFinished() || auction.isCancelled()) return;

        auction.finishAuction(); // thong bao toi tat ca subscribers qua Observer
        try {
            new AuctionDAO().finish(auction.getAuctionId());
        } catch (SQLException e) {
            System.err.println("[Scheduler] Loi luu DB khi ket thuc phien: " + e.getMessage());
        }
        System.out.println("[Scheduler] Phien " + auction.getAuctionId() + " da ket thuc tu dong.");

        AdminEventBus.getInstance().publish(
                AdminEventBus.EVENT_AUCTION_FINISHED,
                auction.getAuctionId() + " — " + auction.getItem().getName()
                        + " | Người thắng: " + auction.getLeadingBidder()
        );

        // Broadcast toi tat ca client (ke ca chua subscribe phien nay)
        broadcastEnded(auction.getAuctionId());
    }

    /**
     * Gui thong bao AUCTION_ENDED toi tat ca client qua broadcaster duoc dang ky.
     * Non-subscribers cung nhan duoc de tu refresh danh sach.
     */
    private void broadcastEnded(String auctionId) {
        if (auctionEndedBroadcaster != null) {
            auctionEndedBroadcaster.accept(auctionId);
        }
    }
}
