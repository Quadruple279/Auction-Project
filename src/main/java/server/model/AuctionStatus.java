package server.model;

/**
 * State machine cho vòng đời phiên đấu giá.
 *
 * Luồng hợp lệ:
 *   OPEN  ──► RUNNING ──► FINISHED ──► PAID
 *    │                        │
 *    └────────────────────────┴──► CANCELED
 */
public enum AuctionStatus {
    OPEN,      // Mới tạo, chưa có ai đặt giá.
    RUNNING,   // Đang diễn ra, đã có ít nhất 1 bid thành công.
    FINISHED,  // Đã kết thúc tự nhiên (hết giờ) hoặc kết thúc thủ công bởi Admin/Seller.
    PAID,      // Người thắng đã hoàn tất thanh toán.
    CANCELLED   // Phiên bị hủy trước khi kết thúc.
}
