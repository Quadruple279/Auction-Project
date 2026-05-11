package server.network;

import server.controller.AuctionController;
import server.controller.AuthenticationController;
import server.exception.AuthenticationException;
import server.model.Auction;
import server.model.AuctionManager;
import server.model.user.User;
import shared.protocol.AuctionEvent;
import shared.protocol.AuctionObserver;
import shared.protocol.Message;
import shared.protocol.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable, AuctionObserver {

    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    // Mỗi client có controller riêng, tránh dùng chung trạng thái currentUser
    private final AuthenticationController authController;
    private final AuctionController auctionController;

    // Danh sách phiên mà client này đã đăng ký nhận update
    private final Set<String> subscriptions = new HashSet<>();

    // Registry tĩnh: auctionId → tập ClientHandler đang subscribe phiên đó.
    // Dùng ConcurrentHashMap + newKeySet() để an toàn đa luồng.
    static final ConcurrentHashMap<String, Set<ClientHandler>> subscriberMap
            = new ConcurrentHashMap<>();

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.authController    = new AuthenticationController();
        this.auctionController = new AuctionController(authController);
    }

    // Vòng lặp chính: Đọc từng JSON từ socket
    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            System.out.println("[SERVER] Client kết nối: " + clientSocket.getInetAddress());

            String line;
            while ((line = in.readLine()) != null) {
                Message request  = Message.fromJson(line);
                Message response = dispatch(request);
                if (response != null) {
                    send(response);
                }
            }

        } catch (IOException e) {
            System.out.println("[SERVER] Client ngắt kết nối: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    // Tự động được gọi khi auction có sự kiện — thay thế broadcast() thủ công
    @Override
    public void onAuctionEvent(AuctionEvent event) {
        switch (event.getType()) {
            case BID_PLACED -> send(
                    Message.of(MessageType.AUCTION_UPDATE)
                            .put("auctionId",     event.getAuctionId())
                            .put("currentPrice",  String.valueOf(event.getCurrentPrice()))
                            .put("leadingBidder", event.getLeadingBidder())
                            .put("eventType",     "BID_PLACED")
                            .put("bidderName",    event.getBidderName())
                            .put("bidAmount",     String.valueOf(event.getBidAmount()))
            );
            case AUCTION_ENDED -> send(
                    Message.of(MessageType.AUCTION_UPDATE)
                            .put("auctionId",     event.getAuctionId())
                            .put("currentPrice",  String.valueOf(event.getCurrentPrice()))
                            .put("leadingBidder", event.getLeadingBidder())
                            .put("eventType",     "AUCTION_ENDED")
            );
            case BID_REJECTED -> {}  // bidder nhận ERROR trực tiếp từ handleBid()
        }
    }

    // Điều phối theo MessageType
    private Message dispatch(Message msg) {
        // Message.fromJson() trả về ERROR nếu parse thất bại
        if (msg.getType() == MessageType.ERROR) {
            return msg;
        }

        return switch (msg.getType()) {
            case LOGIN       -> handleLogin(msg);
            case REGISTER    -> handleRegister(msg);
            case BID         -> handleBid(msg);
            case SUBSCRIBE   -> handleSubscribe(msg);
            case UNSUBSCRIBE -> handleUnsubscribe(msg);
            case LOGOUT      -> { cleanup(); yield null; }
            default -> Message.of(MessageType.ERROR)
                    .put("reason", "Lệnh không xác định: " + msg.getType());
        };
    }

    // Xử lý từng loại lệnh
    private Message handleLogin(Message msg) {
        try {
            String username = msg.get("username");
            String password = msg.get("password");
            User user = authController.login(username, password);
            System.out.println("[SERVER] Đăng nhập thành công: " + user.getName());
            return Message.of(MessageType.LOGIN_SUCCESS)
                    .put("username", user.getName())
                    .put("role",     user.getRole());
        } catch (AuthenticationException e) {
            return Message.of(MessageType.LOGIN_FAILED).put("reason", e.getMessage());
        } catch (IllegalStateException e) {
            return Message.of(MessageType.ERROR).put("reason", "Thiếu trường: " + e.getMessage());
        }
    }

    private Message handleRegister(Message msg) {
        try {
            String username = msg.get("username");
            String password = msg.get("password");
            String role     = msg.get("role");
            authController.register(username, username, password, role);
            System.out.println("[SERVER] Đăng ký thành công: " + username);
            return Message.of(MessageType.REGISTER_SUCCESS);
        } catch (Exception e) {
            return Message.of(MessageType.REGISTER_FAILED)
                    .put("reason", e.getMessage());
        }
    }

    private Message handleBid(Message msg) {
        try {
            String auctionId = msg.get("auctionId");
            double amount    = Double.parseDouble(msg.get("amount"));

            // placeBid() → auction.placeBid() → notifyObservers()
            // → onAuctionEvent() của mỗi ClientHandler tự gửi update
            auctionController.placeBid(auctionId, amount);

            return null;  // update được gửi tự động qua onAuctionEvent()

        } catch (NumberFormatException e) {
            return Message.of(MessageType.ERROR).put("reason", "Số tiền không hợp lệ");
        } catch (RuntimeException e) {
            return Message.of(MessageType.ERROR).put("reason", e.getMessage());
        }
    }
    private Message handleSubscribe(Message msg) {
        String auctionId = msg.get("auctionId");
        Auction auction  = AuctionManager.getInstance().findById(auctionId);

        if (auction == null) {
            return Message.of(MessageType.ERROR)
                    .put("reason", "Không tìm thấy phiên: " + auctionId);
        }

        auction.attach(this);        // đăng ký làm observer của phiên
        subscriptions.add(auctionId);
        System.out.println("[SERVER] Client subscribe phiên: " + auctionId);
        return null;
    }


    // Gửi
    private Message handleUnsubscribe(Message msg) {
        unsubscribeFrom(msg.get("auctionId"));
        return null;
    }

    public synchronized void send(Message msg) {
        if (out != null) {
            out.println(msg.toJson());
        }
    }

    private void unsubscribeFrom(String auctionId) {
        subscriptions.remove(auctionId);
        Auction auction = AuctionManager.getInstance().findById(auctionId);
        if (auction != null) {
            auction.detach(this);    // hủy đăng ký observer
        }
    }

    private void cleanup() {
        for (String auctionId : new HashSet<>(subscriptions)) {
            unsubscribeFrom(auctionId);
        }
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException ignored) {}
        System.out.println("[SERVER] Đã giải phóng tài nguyên cho client.");
    }
}
