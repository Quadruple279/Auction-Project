package server.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.exception.AuctionClosedException;
import server.exception.InvalidBidException;
import server.model.item.Art;
import server.model.item.Electronics;
import server.model.item.Item;
import server.model.item.ItemFactory;
import shared.protocol.AuctionEvent;
import shared.protocol.AuctionObserver;

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
    void setUp() {
        item = ItemFactory.createItem(
            "vehicle", "HD-VS-01", "Vision Sport",
            10_000_000, "Xe máy", "seller1", "2025", "29AE-57650"
        );
        // endTime xa tương lai để tránh AuctionClosedException trong các test thông thường
        auction = new Auction("AU001", item, LocalDateTime.now().plusHours(2), "seller1");
    }

    // ── placeBid cơ bản ──────────────────────────────────────────

    @Test
    void testPlaceBid_updatesStateCorrectly() throws Exception {
        auction.placeBid("Minh", 12_000_000);
        assertAll("Kiểm tra trạng thái sau bid",
            () -> assertEquals(12_000_000, auction.getCurrentPrice(), "Giá phải cập nhật"),
            () -> assertEquals("Minh", auction.getLeadingBidder(), "Leading bidder phải cập nhật"),
            () -> assertEquals(1, auction.getTransactionHistory().size(), "Phải có 1 transaction")
        );
    }

    @Test
    void testPlaceBid_statusChangesToRunning() throws Exception {
        assertEquals(AuctionStatus.OPEN, auction.getStatus(), "Trạng thái ban đầu phải là OPEN");
        auction.placeBid("Minh", 12_000_000);
        assertEquals(AuctionStatus.RUNNING, auction.getStatus(), "Sau bid đầu tiên phải chuyển sang RUNNING");
    }

    @Test
    void testPlaceBid_throwsInvalidBidException_whenBidTooLow() {
        assertThrows(InvalidBidException.class,
            () -> auction.placeBid("Ngu", 9_000_000),
            "Bid thấp hơn giá hiện tại phải throw InvalidBidException"
        );
    }

    @Test
    void testPlaceBid_throwsInvalidBidException_whenLeadingBidderBidsAgain() throws Exception {
        auction.placeBid("Minh", 12_000_000);
        assertThrows(InvalidBidException.class,
            () -> auction.placeBid("Minh", 13_000_000),
            "Leading bidder không được bid lại"
        );
    }

    @Test
    void testPlaceBid_throwsAuctionClosedException_whenFinished() throws Exception {
        auction.finishAuction();
        assertThrows(AuctionClosedException.class,
            () -> auction.placeBid("Huy", 15_000_000),
            "Phiên đã kết thúc phải throw AuctionClosedException"
        );
    }

    @Test
    void testPlaceBid_throwsAuctionClosedException_whenCancelled() throws Exception {
        auction.cancelAuction();
        assertThrows(AuctionClosedException.class,
            () -> auction.placeBid("Huy", 15_000_000),
            "Phiên bị hủy phải throw AuctionClosedException"
        );
    }

    @Test
    void testPlaceBid_throwsAuctionClosedException_whenExpired() {
        // Tạo auction đã hết hạn (endTime trong quá khứ)
        Auction expired = new Auction(
            "AU-EXP", item, LocalDateTime.now().minusSeconds(1), "seller1"
        );
        assertThrows(AuctionClosedException.class,
            () -> expired.placeBid("Huy", 15_000_000),
            "Phiên đã hết giờ phải throw AuctionClosedException"
        );
    }

    // ── Observer pattern ─────────────────────────────────────────

    @Test
    void testObserver_isCalledOnBid() throws Exception {
        AuctionObserver mockObserver = mock(AuctionObserver.class);
        auction.attach(mockObserver);
        auction.placeBid("Minh", 13_000_000);
        verify(mockObserver, times(1)).onAuctionEvent(any(AuctionEvent.class));
    }

    @Test
    void testObserver_isNotCalledAfterDetach() throws Exception {
        AuctionObserver mockObserver = mock(AuctionObserver.class);
        auction.attach(mockObserver);
        auction.detach(mockObserver);
        auction.placeBid("Minh", 13_000_000);
        verify(mockObserver, never()).onAuctionEvent(any());
    }

    // ── finishAuction / cancelAuction ────────────────────────────

    @Test
    void testFinishAuction_setsStatusFinished() {
        auction.finishAuction();
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertTrue(auction.isFinished());
    }

    @Test
    void testFinishAuction_isIdempotent() {
        auction.finishAuction();
        auction.finishAuction(); // gọi lại không được crash
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    @Test
    void testCancelAuction_setsStatusCancelled() {
        boolean result = auction.cancelAuction();
        assertTrue(result, "cancelAuction() phải trả về true khi hủy thành công");
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        assertTrue(auction.isCancelled());
    }

    @Test
    void testCancelAuction_returnsFalse_whenAlreadyFinished() {
        auction.finishAuction();
        boolean result = auction.cancelAuction();
        assertFalse(result, "Không thể hủy phiên đã FINISHED");
    }

    @Test
    void testMarkPaid_works_afterFinish() {
        auction.finishAuction();
        boolean result = auction.markPaid();
        assertTrue(result, "markPaid() phải trả về true sau khi FINISHED");
        assertEquals(AuctionStatus.PAID, auction.getStatus());
    }

    @Test
    void testMarkPaid_returnsFalse_whenNotFinished() {
        boolean result = auction.markPaid();
        assertFalse(result, "markPaid() phải trả về false khi chưa FINISHED");
    }

    // ── Auto Bidding ─────────────────────────────────────────────

    @Test
    void testAutoBidding_triggersWhenOutbid() throws Exception {
        Item artItem = new Art("I001", "Mona Lisa", 500, "Tranh nổi tiếng", "seller1", "Da Vinci");
        Auction autoBidAuction = new Auction(
            "A001", artItem, LocalDateTime.now().plusHours(1), "seller1"
        );
        autoBidAuction.enableAutoBid("Nam", 10_000_000, 1_000);

        autoBidAuction.placeBid("Huy", 700);

        // Auto-bid của Nam phải kích hoạt: 700 + 1000 = 1700
        assertEquals(1_700, autoBidAuction.getCurrentPrice(), 0.01,
            "Auto-bid phải tăng giá lên currentPrice + increment");
        assertEquals("Nam", autoBidAuction.getLeadingBidder(),
            "Nam phải là leading bidder sau auto-bid");
    }

    @Test
    void testAutoBidding_doesNotExceedMaxBid() throws Exception {
        Item artItem = new Art("I002", "Starry Night", 500, "Tranh Van Gogh", "seller1", "Van Gogh");
        Auction autoBidAuction = new Auction(
            "A002", artItem, LocalDateTime.now().plusHours(1), "seller1"
        );
        autoBidAuction.enableAutoBid("Nam", 1_000, 600); // maxBid = 1000, increment = 600

        // Bid 800: auto-bid sẽ cố thêm 600 = 1400 > maxBid => không kích hoạt
        autoBidAuction.placeBid("Huy", 800);

        // Nam không auto-bid vì 800+600=1400 > 1000
        assertEquals("Huy", autoBidAuction.getLeadingBidder(),
            "Auto-bid không được vượt quá maxBid");
    }

    // ── Concurrency ──────────────────────────────────────────────

    @Test
    void testConcurrentBidding_onlyOneBidAtSameAmount() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        double bidAmount = 13_000_000;

        for (int i = 0; i < numberOfThreads; i++) {
            String bidderName = "User" + i;
            service.execute(() -> {
                try {
                    auction.placeBid(bidderName, bidAmount);
                } catch (Exception e) {
                    System.out.println(bidderName + "dat gia that bai: " + e.getMessage());
                } finally {
                    latch.countDown(); // giảm bộ đếm luồng
                }
            });
        }
        latch.await();
        service.shutdown();

        assertAll("Kiểm tra sau concurrent bid cùng giá",
            () -> assertEquals(bidAmount, auction.getCurrentPrice(), "Giá phải bằng bidAmount"),
            () -> assertEquals(1, auction.getTransactionHistory().size(),
                "Chỉ 1 bid được chấp nhận khi tất cả bid cùng giá")
        );
    }

    // ── ItemFactory ──────────────────────────────────────────────

    @Test
    void testItemFactory_createArt() {
        Item art = ItemFactory.createItem("art", "A1", "Mona Lisa", 1000, "desc", "seller", "Da Vinci", "");
        assertNotNull(art);
        assertEquals("ART", art.getType());
    }

    @Test
    void testItemFactory_createElectronics() {
        Item e = ItemFactory.createItem("electronics", "E1", "Laptop", 5000, "desc", "seller", "24", "");
        assertNotNull(e);
        assertEquals("ELECTRONICS", e.getType());
    }

    @Test
    void testItemFactory_createVehicle() {
        Item v = ItemFactory.createItem("vehicle", "V1", "Honda Wave", 3000, "desc", "seller", "2022", "51A-123");
        assertNotNull(v);
        assertEquals("VEHICLE", v.getType());
    }

    @Test
    void testItemFactory_unknownType_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> ItemFactory.createItem("magic_carpet", "X1", "Thảm Bay", 9999, "desc", "seller", "", ""),
            "Loại item không hợp lệ phải throw IllegalArgumentException"
        );
    }
}
