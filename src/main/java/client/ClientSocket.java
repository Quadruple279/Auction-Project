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
import java.util.ArrayList;
import java.util.List;

public class ClientSocket {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean running = false;

    // Observer nhận AuctionEvent realtime
    private List<AuctionObserver> observers = new ArrayList<>();

    // Listener nhận phản hồi một lần (LOGIN_SUCCESS, LOGIN_FAILED, ERROR, …)
    private ResponseListener responseListener;

    private static ClientSocket instance;

    /** Callback nhận phản hồi không phải AUCTION_UPDATE (login, register, lỗi…). */
    public interface ResponseListener {
        void onResponse(Message message);
    }

    public static ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    private ClientSocket() {}

    // ─── Kết nối ─────────────────────────────────────────────────────────────

    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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

    /** Background thread lắng nghe liên tục, tự động cập nhật UI qua Observer. */
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

    /** Phân loại Message nhận được: AUCTION_UPDATE → notifyObservers, còn lại → responseListener. */
    private void handleMessage(String json) {
        try {
            Message msg = Message.fromJson(json);

            if (msg.getType() == MessageType.AUCTION_UPDATE) {
                String auctionId     = msg.get("auctionId");
                String eventType     = msg.get("eventType");
                double currentPrice  = Double.parseDouble(msg.get("currentPrice"));
                String leadingBidder = msg.get("leadingBidder");
                String bidderName    = msg.getOrDefault("bidderName", "");
                double bidAmount     = Double.parseDouble(msg.getOrDefault("bidAmount", "0"));

                AuctionEvent.Type type = AuctionEvent.Type.valueOf(eventType);
                AuctionEvent event = new AuctionEvent(
                        type, auctionId, bidderName, leadingBidder, bidAmount, currentPrice);
                notifyObservers(event);

            } else if (responseListener != null) {
                responseListener.onResponse(msg);
            }

        } catch (Exception e) {
            System.out.println("[CLIENT] Lỗi parse: " + e.getMessage());
        }
    }

    // ─── API gửi lệnh ─────────────────────────────────────────────────────────

    public void setResponseListener(ResponseListener listener) {
        this.responseListener = listener;
    }

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
        } catch (IOException ignored) {}
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
}
