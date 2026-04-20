package server.model.observer;

import server.model.AuctionEvent;

public interface AuctionSubject {
    void attach(AuctionObserver observer);
    void detach(AuctionObserver observer);
    void notifyObservers(AuctionEvent event);
}
