package client;

import shared.protocol.AuctionEvent;
import shared.protocol.AuctionObserver;
import shared.protocol.Message;
import shared.protocol.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientSocket {
    private static final String HOST;
    private static final int PORT;

    static {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream is = ClientSocket.class.getResourceAsStream("/server.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) {
            System.out.println("[CLIENT] Không đọc được server.properties, dùng mặc định.");
        }
        HOST = props.getProperty("server.host", "localhost");
        PORT = Integer.parseInt(props.getProperty("server.port", "8080"));
    }

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean running = false;

    // Observer nhận AuctionEvent realtime
    private List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    // Listener map: mỗi MessageType có thể có nhiều listener — không bị đè nhau
    private final ConcurrentHashMap<
            MessageType,
            CopyOnWriteArrayList<ResponseListener>
            > listenerMap = new ConcurrentHashMap<>();

    private static volatile ClientSocket instance;

    /**
     * Callback nhận phản hồi không phải AUCTION_UPDATE (login, register, lỗi…).
     */
    public interface ResponseListener {
        void onResponse(Message message);
    }

    public static ClientSocket getInstance() {
        if (instance == null) {
            synchronized (ClientSocket.class) {
                if (instance == null) {
                    instance = new ClientSocket();
                }
            }
        }
        return instance;
    }

    private ClientSocket() {
    }

    // ─── Kết nối ─────────────────────────────────────────────────────────────

    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            running = true;
            startListening();
            System.out.println("[CLIENT] Kết nối tới server THÀNH CÔNG");
            return true;
        } catch (IOException e) {
            System.out.println("[CLIENT] Không thể kết nối tới server: " + e.getMessage());
            return false;
        }
    }

    /**
     * Background thread lắng nghe liên tục, tự động cập nhật UI qua Observer.
     */
    private void startListening() {
        Thread bgThread = new Thread(() -> {
            System.out.println("[CLIENT] Background Thread đang lắng nghe");
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    System.out.println("[CLIENT] Nhận từ server: " + line);
                    handleMessage(line);
                }
            } catch (IOException e) {
                if (running) {
                    System.out.println("[CLIENT] Mất kết nối: " + e.getMessage());
                }
            }
        });
        bgThread.setDaemon(true);
        bgThread.setName("ClientSocket-Listener");
        bgThread.start();
    }

    /**
     * Phân loại Message nhận được: AUCTION_UPDATE → notifyObservers,
     * còn lại → dispatch tới tất cả listener đăng ký cho MessageType đó.
     */
    private void handleMessage(String json) {
        try {
            Message msg = Message.fromJson(json);

            if (msg.getType() == MessageType.AUCTION_UPDATE) {
                String auctionId = msg.get("auctionId");
                String eventType = msg.get("eventType");
                double currentPrice = Double.parseDouble(msg.get("currentPrice"));
                String leadingBidder = msg.get("leadingBidder");
                String bidderName = msg.getOrDefault("bidderName", "");
                double bidAmount = Double.parseDouble(msg.getOrDefault("bidAmount", "0"));

                AuctionEvent.Type type = AuctionEvent.Type.valueOf(eventType);
                AuctionEvent event;

                if (type == AuctionEvent.Type.TIME_EXTENDED) {
                    long newEndTimeEpoch = Long.parseLong(msg.get("newEndTimeEpoch"));
                    event = new AuctionEvent(type, auctionId, newEndTimeEpoch);
                } else {
                    event = new AuctionEvent(type, auctionId, bidderName, leadingBidder, bidAmount, currentPrice);
                }

                notifyObservers(event);

            } else if (msg.getType() == MessageType.NEW_AUCTION) {
                AuctionEvent event = new AuctionEvent(
                        AuctionEvent.Type.NEW_AUCTION, msg.get("auctionId"), "", "", 0, 0);
                notifyObservers(event);
            } else if (msg.getType() == MessageType.USER_DELETED) {
                AuctionEvent event = new AuctionEvent(
                        AuctionEvent.Type.USER_DELETED, msg.get("username"), "", "", 0, 0);
                notifyObservers(event);
            } else {
                // Dispatch tới tất cả listener đã đăng ký cho type này
                CopyOnWriteArrayList<ResponseListener> listeners = listenerMap.get(msg.getType());
                if (listeners != null) {
                    for (ResponseListener listener : listeners) {
                        listener.onResponse(msg);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("[CLIENT] Lỗi parse: " + e.getMessage());
        }
    }

    // ─── Listener management ──────────────────────────────────────────────────

    public void addResponseListener(MessageType type, ResponseListener listener) {
        listenerMap.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void removeResponseListener(MessageType type, ResponseListener listener) {
        CopyOnWriteArrayList<ResponseListener> listeners = listenerMap.get(type);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public void clearResponseListeners(MessageType type) {
        listenerMap.remove(type);
    }

    // ─── API gửi lệnh ─────────────────────────────────────────────────────────

    public void sendLogin(String username, String password) {
        send(Message.of(MessageType.LOGIN)
                .put("username", username)
                .put("password", password));
    }

    public void sendRegister(String username, String password, String role) {
        send(Message.of(MessageType.REGISTER)
                .put("username", username)
                .put("password", password)
                .put("role", role));
    }

    public void sendBid(String auctionId, double amount) {
        send(Message.of(MessageType.BID)
                .put("auctionId", auctionId)
                .put("amount", String.valueOf(amount)));
    }

    public void sendUpdateUser(String newDisplayName, String newPassword) {
        Message msg = Message.of(MessageType.UPDATE_USER)
                .put("newDisplayName", newDisplayName);
        if (newPassword != null && !newPassword.isEmpty()) {
            msg.put("newPassword", newPassword);
        }
        send(msg);
    }

    public void sendGetAuctions() {
        send(Message.of(MessageType.GET_AUCTIONS));
    }
    public void sendGetSystemLog() {
        send(Message.of(MessageType.GET_SYSTEM_LOG));
    }


    public void subscribe(String auctionId) {
        send(Message.of(MessageType.SUBSCRIBE).put("auctionId", auctionId));
    }

    public void unsubscribe(String auctionId) {
        send(Message.of(MessageType.UNSUBSCRIBE).put("auctionId", auctionId));
    }

    public void disconnect() {
        if (out != null) {
            send(Message.of(MessageType.LOGOUT));
        }
        running = false;
        try {
            if (socket != null) socket.close();
            System.out.println("[CLIENT] Đã ngắt kết nối.");
        } catch (IOException ignored) {
        }
    }

    private void send(Message msg) {
        if (out != null) {
            out.println(msg.toJson());
            System.out.println("[CLIENT] Gửi: " + msg.getType());
        }
    }

    // ─── Observer ─────────────────────────────────────────────────────────────

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(AuctionEvent event) {
        for (AuctionObserver observer : observers) {
            observer.onAuctionEvent(event);
        }
    }

    public void sendCreateAuction(String itemType, String itemName, String description,
                                  double price, int durationMinutes, String info1, String info2) {
        send(Message.of(MessageType.CREATE_AUCTION)
                .put("itemType", itemType)
                .put("itemName", itemName)
                .put("description", description)
                .put("price", String.valueOf(price))
                .put("durationMinutes", String.valueOf(durationMinutes))
                .put("info1", info1)
                .put("info2", info2));
    }

    public void sendDeleteAuction(String auctionId) {
        send(Message.of(MessageType.DELETE_AUCTION).put("auctionId", auctionId));
    }

    public void sendUpdateAuction(String auctionId, String newName, String newDescription, double newPrice) {
        send(Message.of(MessageType.UPDATE_AUCTION)
                .put("auctionId", auctionId)
                .put("newName", newName)
                .put("newDescription", newDescription)
                .put("newPrice", String.valueOf(newPrice)));
    }

    public void sendFinishAuction(String auctionId) {
        send(Message.of(MessageType.FINISH_AUCTION).put("auctionId", auctionId));
    }

    public void sendCancelAuction(String auctionId) {
        send(Message.of(MessageType.CANCEL_AUCTION).put("auctionId", auctionId));
    }

    public void sendGetBidHistory(String auctionId) {
        send(Message.of(MessageType.GET_BID_HISTORY).put("auctionId", auctionId));
    }

    public void sendDeleteUser(String username) {
        send(Message.of(MessageType.DELETE_USER).put("username", username));
    }

    public void sendMarkPaid(String auctionId) {
        send(Message.of(MessageType.MARK_PAID).put("auctionId", auctionId));
    }

    public void sendEnableAutoBid(String auctionId, double maxBid, double increment) {
        send(Message.of(MessageType.ENABLE_AUTO_BID)
                .put("auctionId", auctionId)
                .put("maxBid", String.valueOf(maxBid))
                .put("increment", String.valueOf(increment)));
    }

    public void sendGetUsers() {
        send(Message.of(MessageType.GET_USERS));
    }

    public void sendAddUser(String username, String password, String role) {
        send(Message.of(MessageType.ADD_USER)
                .put("username", username)
                .put("password", password)
                .put("role", role));
    }

    public void sendUpdateUserAdmin(String targetUsername, String newDisplayName, String newPassword) {
        Message msg = Message.of(MessageType.UPDATE_USER_ADMIN)
                .put("targetUsername", targetUsername)
                .put("newDisplayName", newDisplayName);
        if (newPassword != null && !newPassword.isEmpty()) {
            msg.put("newPassword", newPassword);
        }
        send(msg);
    }

    public void sendGetBidHistoryByUser(String username) {
        send(Message.of(MessageType.GET_BID_HISTORY_BY_USER).put("username", username));
    }
}
