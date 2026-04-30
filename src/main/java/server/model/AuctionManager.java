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

    public void addItem(Auction auction){
        auctionList.add(auction);
    }
    public ArrayList<Auction> getAuctionList(){
        return auctionList;
    }
}
