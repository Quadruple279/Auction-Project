package server.model.observer.subObservers;

import server.model.AuctionEvent;
import server.model.observer.AuctionObserver;

public class AuctionEndObserver implements AuctionObserver {
    @Override
    public void onAuctionEvent(AuctionEvent event) {
        if (event.getType()==AuctionEvent.Type.AUCTION_ENDED){
            System.out.println("----------------------------");
            System.out.println("   Phiên Đấu Giá Kết Thúc   ");
            System.out.println("----------------------------");
            if ("None".equals(event.getLeadingBidder())){
                System.out.println("Chưa có ai đấu giá sản phẩm này.");
            }
            else{
                System.out.println("Người thắng phiên: "+event.getLeadingBidder());
                System.out.printf("Với số tiền đấu giá là: %.0f VNĐ %n",event.getCurrentPrice());
            }
        }
    }
}
