package server.network;

import server.controller.AuctionController;
import server.controller.AuthenticationController;
import server.exception.AuthenticationException;
import server.model.Auction;
import server.model.AuctionManager;
import server.model.user.User;
import shared.protocol.Message;
import shared.protocol.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {

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

            Auction auction = AuctionManager.getInstance().findById(auctionId);
            if (auction == null) {
                return Message.of(MessageType.ERROR)
                        .put("reason", "Không tìm thấy phiên: " + auctionId);
            }

            // Đặt giá — AuctionController kiểm tra đăng nhập, quyền, rồi gọi auction.placeBid()
            auctionController.placeBid(auction, amount);

            User current = authController.getCurrentUser();
            String bidderName = current != null ? current.getName() : "unknown";

            Message update = Message.of(MessageType.AUCTION_UPDATE)
                    .put("auctionId",     auctionId)
                    .put("currentPrice",  String.valueOf(auction.getCurrentPrice()))
                    .put("leadingBidder", auction.getLeadingBidder())
                    .put("eventType",     "BID_PLACED")
                    .put("bidderName",    bidderName)
                    .put("bidAmount",     String.valueOf(amount));

            // Broadcast tới tất cả client đang theo dõi phiên này (trừ người đặt giá)
            broadcast(auctionId, update);

            return update;

        } catch (NumberFormatException e) {
            return Message.of(MessageType.ERROR).put("reason", "Số tiền không hợp lệ");
        } catch (RuntimeException e) {
            // Bắt InvalidBidException, AuctionClosedException, chưa đăng nhập, sai quyền, v.v.
            return Message.of(MessageType.ERROR).put("reason", e.getMessage());
        }
    }

    private Message handleSubscribe(Message msg) {
        String auctionId = msg.get("auctionId");
        subscriptions.add(auctionId);
        subscriberMap
                .computeIfAbsent(auctionId, k -> ConcurrentHashMap.newKeySet())
                .add(this);
        System.out.println("[SERVER] Client subscribe phiên: " + auctionId);
        return null; // không cần phản hồi
    }

    private Message handleUnsubscribe(Message msg) {
        String auctionId = msg.get("auctionId");
        unsubscribeFrom(auctionId);
        return null;
    }

    // Gửi/Broadcast
    public synchronized void send(Message msg) {
        if (out != null) {
            out.println(msg.toJson());
        }
    }

    // Broadcast AUCTION_UPDATE tới các subscriber của phiên, trừ handler hiện tại
    // (người đặt giá đã nhận response riêng rồi)
    private void broadcast(String auctionId, Message msg) {
        Set<ClientHandler> subs = subscriberMap.get(auctionId);
        if (subs == null) return;
        for (ClientHandler handler : subs) {
            if (handler != this) {
                handler.send(msg);
            }
        }
    }

    // Dọn dẹp
    private void unsubscribeFrom(String auctionId) {
        subscriptions.remove(auctionId);
        Set<ClientHandler> set = subscriberMap.get(auctionId);
        if (set != null) set.remove(this);
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
