package shared.protocol;

public interface AuctionObserver {
    void onAuctionEvent(AuctionEvent event);
}
