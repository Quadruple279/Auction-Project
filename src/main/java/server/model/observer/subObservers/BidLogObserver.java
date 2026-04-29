package server.model.observer.subObservers;

import server.exception.InvalidBidException;
import server.model.AuctionEvent;
import server.model.observer.AuctionObserver;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BidLogObserver implements AuctionObserver {
    @Override
    public void onAuctionEvent(AuctionEvent event) {
        LocalDateTime bidTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        switch (event.getType()){
            case BID_PLACED -> System.out.printf(
                    "[BID] [%s] Khách Quý %s vừa đặt giá %.0f VNĐ cho phiên %s.%n",
                    bidTime.format(formatter),event.getBidderName(),event.getBidAmount(),event.getAuctionId()
            );
            case BID_REJECTED -> System.out.printf("[BID] [%s] Khách Quý %s vừa đặt giá không thành công cho phiên %s" +
                            " vì bạn đang đặt giá thấp hơn so với giá hiện tại của phiên.%n",
                    bidTime.format(formatter),event.getBidderName(),event.getBidAmount(),event.getAuctionId()
            );
            case AUCTION_ENDED -> {}
        }
    }
}
