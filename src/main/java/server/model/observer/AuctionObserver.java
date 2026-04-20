package server.model.observer;

import server.model.AuctionEvent;

public interface AuctionObserver {
    void onAuctionEvent(AuctionEvent event);
}
