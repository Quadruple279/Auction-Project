package server.model;

import server.dao.AuctionDAO;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class AuctionManager {
    private static AuctionManager instance;
    private ArrayList<Auction> auctionList;
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private AuctionManager() {
        auctionList = new ArrayList<>();
    }

    public static synchronized AuctionManager getInstance () {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public synchronized void addAuction(Auction auction) {
        auctionList.add(auction);
        scheduleFinish(auction);
    }
    public synchronized void removeAuction(Auction auction){
        auctionList.remove(auction);
    }
    public synchronized void updateAuction(Auction updatedAuction){
        for (int i = 0; i < auctionList.size(); i++) {
            if (auctionList.get(i).getAuctionId()
                    .equals(updatedAuction.getAuctionId())) {

                auctionList.set(i, updatedAuction);
                return;
            }
        }
    }
    public boolean cancelAuction(Auction auction) {
        // Chỉ cho phép huỷ nếu phiên chưa kết thúc và chưa bị huỷ trước đó
        if (auction != null && !auction.isFinished() && !auction.isCancelled()) {
            auction.setCancelled(true);

            // TODO: Sau này làm phần Socket,có thể gọi thêm logic
            // broadcast thông báo huỷ phiên đến các Client ở đây

            return true;
        }
        return false;
    }

    public ArrayList<Auction> getAuctionList(){
        return auctionList;
    }

    // Tìm phiên theo ID, trả về null nếu không tồn tại
    public synchronized Auction findById(String auctionId) {
        for (Auction a : auctionList) {
            if (a.getAuctionId().equals(auctionId)) {
                return a;
            }
        }
        return null;
    }
    private void scheduleFinish(Auction auction){
        if (auction.isFinished()) return ;
        long delay = Duration.between(LocalDateTime.now(),auction.getEndTime()).toMillis();
        if (delay <=0 ){
            finishAndSave(auction);
            return;
        }
        scheduler.schedule(() -> finishAndSave(auction),delay,TimeUnit.MILLISECONDS);
    }
    private void finishAndSave(Auction auction){
        if (auction.isFinished()) return ;
        auction.finishAuction();
        try{
            new AuctionDAO().finish(auction.getAuctionId());
        }
        catch (SQLException e){
            System.out.println("[Scheduler] Lỗi lưu DB khi kết thúc phiên: " + e.getMessage());
        }
        System.out.println("[Scheduler] Phiên " + auction.getAuctionId() + " đã kết thúc tự động.");
    }
}
