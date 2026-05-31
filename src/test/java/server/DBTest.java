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
 * db.properties trong src/test/resources/ override MySQL → H2 khi chạy mvn test.
 * Không cần MySQL – CI/CD chạy được hoàn toàn offline.
 */
class DBTest {

    // ─────────────────────────────────────────────────────────────
    //  Schema setup (chạy một lần trước toàn bộ test suite)
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

            st.execute("""
                CREATE TABLE IF NOT EXISTS system_logs (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    timestamp   TIMESTAMP,
                    level       VARCHAR(20),
                    message     VARCHAR(1000)
                )""");
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
    //  DBConnection
    // ─────────────────────────────────────────────────────────────

    @Test
    void testDatabaseConnection_isValid() throws SQLException {
        Connection conn = DBConnection.getConnection();
        assertNotNull(conn, "DBConnection.getConnection() phải trả về connection hợp lệ");
        assertFalse(conn.isClosed(), "Connection không được đóng ngay sau khi lấy");
    }

    // ─────────────────────────────────────────────────────────────
    //  UserDAO
    // ─────────────────────────────────────────────────────────────

    @Test
    void testUserDAO_saveAndFindAll_bidder() throws SQLException {
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
    void testUserDAO_saveMultiple_allFoundInFindAll() throws SQLException {
        UserDAO userDAO = new UserDAO();
        userDAO.save(new Bidder(0, "TEST-Alice", "Alice", "pw"));
        userDAO.save(new Seller(0, "TEST-Bob", "Bob", "pw"));

        List<User> users = userDAO.findAll();
        long testUsers = users.stream()
                .filter(u -> u.getName().startsWith("TEST-"))
                .count();

        assertTrue(testUsers >= 2,
                "Sau khi lưu 2 user TEST- phải có ít nhất 2 bản ghi trong findAll()");
    }

    @Test
    void testUserDAO_checkPassword_correct() throws SQLException {
        UserDAO userDAO = new UserDAO();
        Bidder user = new Bidder(0, "TEST-PassCheck", "Test User", "secret123");
        userDAO.save(user);

        List<User> all = userDAO.findAll();
        User found = all.stream()
                .filter(u -> "TEST-PassCheck".equals(u.getName()))
                .findFirst().orElse(null);

        assertNotNull(found);
        assertTrue(found.checkPassword("secret123"), "Mật khẩu đúng phải trả về true");
        assertFalse(found.checkPassword("wrongpass"), "Mật khẩu sai phải trả về false");
    }

    @Test
    void testUserDAO_delete_removesUser() throws SQLException {
        UserDAO userDAO = new UserDAO();
        Bidder user = new Bidder(0, "TEST-Delete", "To Delete", "pw");
        userDAO.save(user);

        // id đã được set bởi save()
        userDAO.delete(user.getId());

        User found = userDAO.findById(user.getId());
        assertNull(found, "User đã xóa không được tìm thấy qua findById()");
    }

    // ─────────────────────────────────────────────────────────────
    //  ItemDAO
    // ─────────────────────────────────────────────────────────────

    @Test
    void testItemDAO_saveAndFindById_art() throws SQLException {
        ItemDAO itemDAO = new ItemDAO();
        Art art = new Art("ITEM-TEST-001", "Tranh Test", 5_000_000, "Mô tả", "seller1", "Picasso");

        itemDAO.save(art);
        Item found = itemDAO.findById("ITEM-TEST-001");

        assertNotNull(found, "Item vừa lưu phải tìm thấy qua findById()");
        assertEquals("Tranh Test", found.getName());
        assertInstanceOf(Art.class, found, "Item type phải là Art");
    }

    @Test
    void testItemDAO_saveAndFindById_electronics() throws SQLException {
        ItemDAO itemDAO = new ItemDAO();
        Electronics e = new Electronics("ITEM-TEST-002", "Laptop X", 20_000_000, "Laptop mới", "seller1", 24);

        itemDAO.save(e);
        Item found = itemDAO.findById("ITEM-TEST-002");

        assertNotNull(found);
        assertInstanceOf(Electronics.class, found);
        assertEquals("Laptop X", found.getName());
        assertEquals(20_000_000, found.getBasePrice(), 0.01);
    }

    @Test
    void testItemDAO_findById_returnsNull_whenNotExist() throws SQLException {
        ItemDAO itemDAO = new ItemDAO();
        Item found = itemDAO.findById("ITEM-NOT-EXISTS-9999");
        assertNull(found, "findById() phải trả về null khi item không tồn tại");
    }

    // ─────────────────────────────────────────────────────────────
    //  AuctionDAO
    // ─────────────────────────────────────────────────────────────

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
    void testAuctionDAO_findById_returnsCorrectAuction() throws SQLException {
        ItemDAO itemDAO = new ItemDAO();
        AuctionDAO auctionDAO = new AuctionDAO();

        Art art = new Art("ITEM-TEST-006", "Tranh FindById", 2_000_000, "...", "seller1", "Raphael");
        itemDAO.save(art);
        Auction auction = new Auction(
                "AU-TEST-005", art,
                LocalDateTime.now().plusHours(1), "seller1"
        );
        auctionDAO.save(auction);

        Auction found = auctionDAO.findById("AU-TEST-005");
        assertNotNull(found, "findById() phải trả về auction đúng");
        assertEquals("AU-TEST-005", found.getAuctionId());
        assertEquals(2_000_000, found.getCurrentPrice(), 0.01);
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
        assertTrue(
            finished.isFinished() || finished.getStatus() == AuctionStatus.FINISHED,
            "Auction phải được đánh dấu FINISHED sau khi gọi finish()"
        );
    }
}
