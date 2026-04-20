package server.model.observer.subObservers;

import server.model.AuctionEvent;
import server.model.observer.AuctionObserver;

public class LeaderBoardObserver implements AuctionObserver {
    @Override
    public void onAuctionEvent(AuctionEvent event) {
        if (event.getType() == AuctionEvent.Type.BID_PLACED){
            System.out.println("----Người Đang Dẫn Đầu Phiên----");
            System.out.printf("Khách Quý %s với số tiền đấu giá là %.0f VNĐ %n",event.getLeadingBidder(),event.getCurrentPrice());
            System.out.println("--------------------------------");
        }
    }
}
