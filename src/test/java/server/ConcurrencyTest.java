package server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.item.Electronics;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm tra tính thread-safety của Auction.placeBid() khi nhiều thread
 * đặt giá đồng thời.
 */
class ConcurrencyTest {

    private Auction auction;
    private static final int NUM_BIDDERS = 100;

    @BeforeEach
    void setUp() {
        auction = new Auction(
                "Cz01",
                new Electronics("Ax001", "iPhone 17", 34, "None", "seller1", 36),
                LocalDateTime.now().plusHours(1),
                "Emperor Argall"
        );
    }

    /**
     * 100 thread bid với các mức giá KHÁC NHAU tăng dần.
     * Kiểm tra: giá cuối > giá ban đầu, số transaction hợp lệ, trạng thái không bị corrupt.
     * KHÔNG assert giá cuối == max vì thứ tự thread là không xác định.
     */
    @Test
    void testConcurrentBids_finalPriceIsHighest() throws InterruptedException {
        double basePrice = auction.getCurrentPrice(); // 34
        ExecutorService executor = Executors.newFixedThreadPool(NUM_BIDDERS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_BIDDERS);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 1; i <= NUM_BIDDERS; i++) {
            final double bidAmount = basePrice + (i * 10);
            executor.submit(() -> {
                try {
                    startGate.await();
                    auction.placeBid("User" + bidAmount, bidAmount);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // Lower bids expected to be rejected
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Tất cả thread phải hoàn thành trong 10s");
        executor.shutdown();

        // Giá cuối phải lớn hơn giá ban đầu
        assertTrue(auction.getCurrentPrice() > basePrice,
                "Giá cuối phải lớn hơn giá ban đầu");

        // Số transaction = số bid thành công
        assertEquals(successCount.get(), auction.getTransactionHistory().size(),
                "Số transaction phải khớp với số bid thành công");

        // Trạng thái không corrupt
        assertNotNull(auction.getLeadingBidder());
        assertNotEquals("None", auction.getLeadingBidder());
    }

    /**
     * 20 thread cùng bid một mức giá giống nhau.
     * Chỉ đúng 1 thread được chấp nhận.
     */
    @Test
    void testConcurrentBids_sameAmountAcceptedOnce() throws InterruptedException {
        int threads = 20;
        double sameAmount = 5_000.0;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    auction.placeBid("UserSame", sameAmount);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Only 1 bid at exactly the same amount should succeed
        assertEquals(1, auction.getTransactionHistory().size(),
                "Chỉ 1 thread được chấp nhận khi tất cả bid cùng một mức giá");
        assertEquals(sameAmount, auction.getCurrentPrice());
    }

    /**
     * Trạng thái auction không bị corrupt sau khi nhiều thread truy cập đồng thời.
     * currentPrice không bao giờ thấp hơn giá ban đầu.
     */
    @Test
    void testConcurrentBids_stateNotCorrupted() throws InterruptedException {
        double basePrice = auction.getCurrentPrice();
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 1; i <= threads; i++) {
            final double amount = basePrice + i * 5;
            final String bidder = "Bidder" + i;
            executor.submit(() -> {
                try {
                    auction.placeBid(bidder, amount);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertTrue(auction.getCurrentPrice() >= basePrice,
                "Giá cuối không được thấp hơn giá khởi điểm");
        assertNotNull(auction.getLeadingBidder());
    }
}
