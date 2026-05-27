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

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void testConcurrentBidsDoNotCorruptState() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_BIDDERS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_BIDDERS);

        for (int i = 1; i <= NUM_BIDDERS; i++) {
            final double bidAmount = 2000.0 + (i * 10);
            executor.submit(() -> {
                try {
                    startGate.await();
                    auction.placeBid("User", bidAmount);
                } catch (Exception ignored) {
                    // Expected: lower bids rejected
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "All threads should finish within timeout");
        executor.shutdown();

        // Final price must equal the highest valid bid (100 * 10 + 2000 = 3000)
        double expectedMax = 2000.0 + (NUM_BIDDERS * 10);
        assertEquals(expectedMax, auction.getCurrentPrice(),
                "Final price phải bằng mức bid cao nhất sau khi tất cả thread chạy xong");

        // Transaction history should have only valid (strictly increasing) bids
        int txCount = auction.getTransactionHistory().size();
        assertTrue(txCount >= 1 && txCount <= NUM_BIDDERS,
                "Số lượng transaction hợp lệ phải nằm trong khoảng [1, " + NUM_BIDDERS + "]");
    }

    @Test
    void testConcurrentBidsWithSameAmountOnlyAcceptsOnce() throws InterruptedException {
        int threads = 20;
        double sameAmount = 5000.0;
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
}
