package server.model;

import server.model.item.Item;

import java.util.ArrayList;

public class AuctionManager {
    private static AuctionManager instance;
    private ArrayList<Auction> auctionList;

    private AuctionManager() {
        auctionList = new ArrayList<>();
    }

    public static synchronized AuctionManager getInstance () {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void addAuction(Auction auction){
        auctionList.add(auction);
    }
    public void removeAuction(Auction auction){
        auctionList.remove(auction);
    }
    public void updateAuction(Auction updatedAuction){
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
    public Auction findById(String auctionId) {
        for (Auction a : auctionList) {
            if (a.getAuctionId().equals(auctionId)) {
                return a;
            }
        }
        return null;
    }
}
