package server;

import org.junit.jupiter.api.*;
import server.dao.*;
import server.model.Auction;
import server.model.AuctionStatus;
import server.model.item.*;
import server.model.user.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DAO integration tests chạy trên H2 in-memory database.
 * Không cần MySQL – CI/CD chạy được hoàn toàn offline.
 */
class DBTest {


    // SCREW YOU
    // ─────────────────────────────────────────────────────────────
    //  Schema setup (chạy một lần trước toàn bộ test)
    // ─────────────────────────────────────────────────────────────

    @BeforeAll
    static void createSchema() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    name        VARCHAR(255) NOT NULL,
                    tenHienThi  VARCHAR(255),
                    password    VARCHAR(255),
                    role        VARCHAR(50)
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS items (
                    id               VARCHAR(100) PRIMARY KEY,
                    name             VARCHAR(255),
                    base_price       DOUBLE,
                    description      VARCHAR(1000),
                    item_type        VARCHAR(50),
                    sellerName       VARCHAR(255),
                    car_year         INT,
                    bien_so_xe       VARCHAR(100),
                    artist           VARCHAR(255),
                    warranty_months  INT
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS auctions (
                    id                 VARCHAR(100) PRIMARY KEY,
                    item_id            VARCHAR(100),
                    current_price      DOUBLE,
                    owner              VARCHAR(255),
                    leading_bidder     VARCHAR(255),
                    is_finished        BOOLEAN DEFAULT FALSE,
                    status             VARCHAR(50),
                    end_time           TIMESTAMP,
                    auto_bidder        VARCHAR(255),
                    max_auto_bid       DOUBLE,
                    auto_bid_increment DOUBLE
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS bid_transactions (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    auction_id  VARCHAR(100),
                    bidder_name VARCHAR(255),
                    bid_amount  DOUBLE,
                    bid_time    TIMESTAMP
                )""");
        }
    }

    @AfterAll
    static void dropSchema() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS bid_transactions");
            st.execute("DROP TABLE IF EXISTS auctions");
            st.execute("DROP TABLE IF EXISTS items");
            st.execute("DROP TABLE IF EXISTS users");
        }
    }

    /** Xóa dữ liệu test sau mỗi test case để tránh ảnh hưởng lẫn nhau. */
    @BeforeEach
    void cleanupTestData() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("DELETE FROM bid_transactions WHERE auction_id LIKE 'AU-TEST%'");
            st.execute("DELETE FROM auctions WHERE id LIKE 'AU-TEST%'");
            st.execute("DELETE FROM items WHERE id LIKE 'ITEM-TEST%'");
            st.execute("DELETE FROM users WHERE name LIKE 'TEST-%'");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Test cases
    // ─────────────────────────────────────────────────────────────

    @Test
    void testDatabaseConnection() throws SQLException {
        Connection conn = DBConnection.getConnection();
        assertNotNull(conn, "DBConnection.getConnection() phải trả về connection hợp lệ");
        assertFalse(conn.isClosed(), "Connection không được đóng ngay sau khi lấy");
    }

    // ── UserDAO ──────────────────────────────────────────────────

    @Test
    void testUserDAO_saveAndFindById() throws SQLException {
        UserDAO userDAO = new UserDAO();
        Bidder user = new Bidder(0, "TEST-Nguyen", "Nguyễn Test", "pass123");

        userDAO.save(user);

        List<User> all = userDAO.findAll();
        User saved = all.stream()
                .filter(u -> "TEST-Nguyen".equals(u.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull(saved, "User đã lưu phải tìm thấy được qua findAll()");
        assertEquals("Nguyễn Test", saved.getDisplayName());
        assertEquals("BIDDER", saved.getRole());
    }

    @Test
    void testUserDAO_findAll_returnsNonEmptyAfterSave() throws SQLException {
        UserDAO userDAO = new UserDAO();
        userDAO.save(new Bidder(0, "TEST-Alice", "Alice", "pw"));
        userDAO.save(new Seller(0, "TEST-Bob", "Bob", "pw"));

        List<User> users = userDAO.findAll();
        long testUsers = users.stream()
                .filter(u -> u.getName().startsWith("TEST-"))
                .count();

        assertTrue(testUsers >= 2,
                "Sau khi lưu 2 user TEST- phải có ít nhất 2 bản ghi TEST- trong findAll()");
    }

    // ── ItemDAO ──────────────────────────────────────────────────

    @Test
    void testItemDAO_saveAndFindById_Art() throws SQLException {
        ItemDAO itemDAO = new ItemDAO();
        Art art = new Art("ITEM-TEST-001", "Tranh Test", 5_000_000, "Mô tả", "seller1", "Picasso");

        itemDAO.save(art);
        Item found = itemDAO.findById("ITEM-TEST-001");

        assertNotNull(found, "Item vừa lưu phải tìm thấy qua findById()");
        assertEquals("Tranh Test", found.getName());
        assertInstanceOf(Art.class, found, "Item type phải là Art");
    }

    @Test
    void testItemDAO_saveAndFindById_Electronics() throws SQLException {
        ItemDAO itemDAO = new ItemDAO();
        Electronics e = new Electronics("ITEM-TEST-002", "Laptop X", 20_000_000, "Laptop mới", "seller1", 24);

        itemDAO.save(e);
        Item found = itemDAO.findById("ITEM-TEST-002");

        assertNotNull(found);
        assertInstanceOf(Electronics.class, found);
        assertEquals("Laptop X", found.getName());
    }

    // ── AuctionDAO ───────────────────────────────────────────────

    @Test
    void testAuctionDAO_saveAndFindAll() throws SQLException {
        ItemDAO itemDAO = new ItemDAO();
        AuctionDAO auctionDAO = new AuctionDAO();

        Art art = new Art("ITEM-TEST-003", "Tranh DAO", 3_000_000, "...", "seller1", "Monet");
        itemDAO.save(art);

        Auction auction = new Auction(
                "AU-TEST-001", art,
                LocalDateTime.now().plusHours(2), "seller1"
        );
        auctionDAO.save(auction);

        List<Auction> all = auctionDAO.findAll();
        boolean found = all.stream().anyMatch(a -> "AU-TEST-001".equals(a.getAuctionId()));
        assertTrue(found, "Auction vừa lưu phải xuất hiện trong findAll()");
    }

    @Test
    void testAuctionDAO_updateAfterBid() throws SQLException {
        ItemDAO itemDAO = new ItemDAO();
        AuctionDAO auctionDAO = new AuctionDAO();

        Art art = new Art("ITEM-TEST-004", "Tranh Bid", 1_000_000, "...", "seller1", "Van Gogh");
        itemDAO.save(art);
        Auction auction = new Auction(
                "AU-TEST-002", art,
                LocalDateTime.now().plusHours(1), "seller1"
        );
        auctionDAO.save(auction);

        auctionDAO.updateAfterBid("AU-TEST-002", 2_000_000, "Minh");
        Auction updated = auctionDAO.findById("AU-TEST-002");

        assertNotNull(updated);
        assertEquals(2_000_000, updated.getCurrentPrice(), 0.01,
                "Giá hiện tại phải được cập nhật sau updateAfterBid()");
        assertEquals("Minh", updated.getLeadingBidder(),
                "Leading bidder phải được cập nhật đúng");
    }

    @Test
    void testAuctionDAO_finish() throws SQLException {
        ItemDAO itemDAO = new ItemDAO();
        AuctionDAO auctionDAO = new AuctionDAO();

        Art art = new Art("ITEM-TEST-005", "Tranh Finish", 500_000, "...", "seller1", "Rembrandt");
        itemDAO.save(art);
        Auction auction = new Auction(
                "AU-TEST-003", art,
                LocalDateTime.now().plusHours(1), "seller1"
        );
        auctionDAO.save(auction);

        auctionDAO.finish("AU-TEST-003");
        Auction finished = auctionDAO.findById("AU-TEST-003");

        assertNotNull(finished);
        assertTrue(finished.isFinished() || finished.getStatus() == AuctionStatus.FINISHED,
                "Auction phải được đánh dấu FINISHED sau khi gọi finish()");
    }
}
