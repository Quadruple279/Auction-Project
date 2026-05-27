package server.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

// Singleton Event Bus cho Admin.
// Bất kỳ component nào (AuthenticationController, AuctionManager, AuctionService...)
// gọi AdminEventBus.publish(event) để phát sự kiện.
// AdminController đăng ký listener để cập nhật systemLog TextArea theo thời gian thực.

public class AdminEventBus {

    public static final String EVENT_USER_ADDED    = "USER_ADDED";
    public static final String EVENT_USER_DELETED  = "USER_DELETED";
    public static final String EVENT_USER_UPDATED  = "USER_UPDATED";
    public static final String EVENT_AUCTION_CREATED  = "AUCTION_CREATED";
    public static final String EVENT_AUCTION_FINISHED = "AUCTION_FINISHED";
    public static final String EVENT_AUCTION_CANCELED = "AUCTION_CANCELED";
    public static final String EVENT_AUCTION_DELETED  = "AUCTION_DELETED";
    public static final String EVENT_BID_PLACED       = "BID_PLACED";

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private static AdminEventBus instance;

    // Danh sách listener — dùng CopyOnWriteArrayList để thread-safe
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    private AdminEventBus() {}

    public static synchronized AdminEventBus getInstance() {
        if (instance == null) instance = new AdminEventBus();
        return instance;
    }

    // Đăng ký listener nhận log message đã được format sẵn.
    // AdminController gọi hàm này trong setAuthController().
    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<String> listener) {
        listeners.remove(listener);
    }


    // Phát một sự kiện tới tất cả listener.
    public void publish(String eventType, String detail) {
        String time    = LocalDateTime.now().format(FMT);
        String label   = toLabel(eventType);
        String message = String.format("[%s] %s: %s", time, label, detail);
        for (Consumer<String> l : listeners) {
            l.accept(message);
        }
    }

    private String toLabel(String eventType) {
        return switch (eventType) {
            case EVENT_USER_ADDED       -> "✅ Thêm user";
            case EVENT_USER_DELETED     -> "🗑 Xóa user";
            case EVENT_USER_UPDATED     -> "✏️ Sửa user";
            case EVENT_AUCTION_CREATED  -> "🆕 Phiên mới";
            case EVENT_AUCTION_FINISHED -> "🏁 Phiên kết thúc";
            case EVENT_AUCTION_CANCELED -> "❌ Phiên bị hủy";
            case EVENT_AUCTION_DELETED  -> "🗑 Xóa phiên";
            case EVENT_BID_PLACED       -> "💰 Đặt giá";
            default                     -> eventType;
        };
    }
}
