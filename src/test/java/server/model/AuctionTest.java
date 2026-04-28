package server.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.exception.InvalidBidException;
import server.model.item.Item;
import server.model.item.ItemFactory;
import server.model.observer.AuctionObserver;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuctionTest {
    private Item item;
    private Auction auction;
    @BeforeEach
    void setUp(){
        item = ItemFactory.creatItem("vehicle","HD-VS-01","vision-sportVersion",10_000_000,"Xe rac","2025","29AE-57650");
        auction = new Auction("AU001", item, LocalDateTime.now().plusHours(2));
    }
    @Test
    void testPlaceBidDetailed() throws Exception{
        auction.placeBid("Minh",12_000_000);
        assertAll(" Kiểm tra dữ liệu phiên đấu giá ",
                () -> assertEquals(12_000_000,auction.getCurrentPrice(),"Sai giá hiện tại"),
                () -> assertEquals("Minh",auction.getLeadingBidder(),"Sai tên người đặt"),
                () -> assertEquals(1,auction.getTransactionHistory().size())
        );
    }
    @Test
    void testPlaceBidThrowsException(){
        assertThrows(InvalidBidException.class,() -> {
            auction.placeBid("Ngu",9_000_000);
        });
    }
    @Test
    void testNotifyObserverisCalled() throws Exception{
        AuctionObserver mockObserver = mock(AuctionObserver.class);
        auction.attach(mockObserver);
        auction.placeBid("Minh",13_000_000);
        verify(mockObserver,times(1)).onAuctionEvent(any(AuctionEvent.class));
    }
    @Test
    void testConcurrentBidding() throws InterruptedException {
        int numberOfThreads = 10; // gia lap 10 nguoi dat gia cung luc
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        double startingPrice = auction.getCurrentPrice();
        double bidAmount = 13_000_000;
        for (int i=0;i<numberOfThreads;i++){
            String bidderName = "User "+i;
            service.execute(() -> {
                try{
                    auction.placeBid(bidderName,bidAmount);
                }
                catch (Exception e){
                    System.out.println(bidderName + "dat gia that bai: "+ e.getMessage() );
                }
                finally {
                    latch.countDown(); // giảm bộ đếm luồng
                }
            });
        }
        latch.await();
        assertAll(" Kiểm tra dữ liệu lượt đấu giá ",
                () -> assertEquals(bidAmount , auction.getCurrentPrice()),
                () -> assertEquals(1,auction.getTransactionHistory().size(),"Chỉ 1 người được đặt giá khi đặt cùng giá")
        );
    }
}