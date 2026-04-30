package server;

import server.model.Auction;
import server.model.item.Electronics;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrencyTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("----- CHUẨN BỊ MÔI TRƯỜNG TEST -----");

        Auction auction = new Auction("Cz01", new Electronics("Ax001","iPhone 17", 34,"None", 36), LocalDateTime.now());

        int numberOfBidders = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfBidders);

        CountDownLatch startGate = new CountDownLatch(1);

        System.out.println("Chuẩn bị bidder.");

        for (int i = 1; i <= numberOfBidders; i++) {
            final int bidderId = i;
            final double bidAmount = 2000.0 + (i * 10);

            executor.submit(() -> {
                try {
                    startGate.await();
                    auction.placeBid("User0", bidAmount);
                    System.out.println("User" + bidderId + " bid thành công: " + auction.getCurrentPrice());

                } catch (Exception e) {
                    System.out.println("User" + bidderId + " xịt: " + e.getMessage());
                }
            });
        }

        System.out.println("Mở đấu giá.");
        startGate.countDown();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("--- KẾT QUẢ CUỐI CÙNG ---");
        System.out.println("Giá chốt sổ: " + auction.getCurrentPrice());
    }
}