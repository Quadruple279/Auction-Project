package server.model;

import server.model.item.Item;

import java.util.ArrayList;

public class AuctionManager {
    // Phải có phương thức getInstance() (và các phương thức khác) để ServerApp.java dùng.
    private static AuctionManager instance;
    private ArrayList<Auction> auctionList;//danh sách lưu trữ các phiên dấu giá
    private AuctionManager() {
        auctionList = new ArrayList<>();
    }
    public static synchronized AuctionManager getInstance () {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }
    // các method quản lí
    public void addItem(Auction auction){
        auctionList.add(auction);
    }
    public ArrayList<Auction> getAuctionList(){
        return auctionList;
    }
}
