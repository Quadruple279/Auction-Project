package server.model.observer;

import shared.protocol.AuctionEvent;
import shared.protocol.AuctionObserver;

public interface AuctionSubject {
    void attach(AuctionObserver observer);
    void detach(AuctionObserver observer);
    void notifyObservers(AuctionEvent event);
}
