package client;

import server.model.AuctionEvent;
import server.model.observer.AuctionObserver;

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

    private List<AuctionObserver> observers = new ArrayList<>();

    private static ClientSocket instance;

    public static ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        } return instance;
    }

    private ClientSocket() {}


    // Method connect
    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            running = true;

            startListening();

            System.out.println("[CLIENT] Ket noi toi server THANH CONG");
            return true;
        } catch (IOException e) {
            System.out.println("[CLIENT] Khong the ket noi toi server " + e.getMessage());
            return false;
        }
    }

    /* Tao backgroung Thread chay ngam lien tuc nhan du lieu tu server
    Khi nhan duoc AuctionEvent thi thong bao cho Observer va tu dong cap nhat UI ma khong can refesh
     */
    private void startListening() {
        Thread bgThread = new Thread(() ->{
            System.out.println("[CLIENT] Background Thread dang lang nghe ");
            try {
                String line;
                // Doc lien tuc
                while (running && (line = in.readLine()) != null) {
                    System.out.println("[CLIENT] Nhan tu server:  " + line);
                    handleMessage(line);
                }
            } catch (IOException e) {
                if (running) {
                    System.out.println("[CLIENT] Mat ket noi: " + e.getMessage());
                }
            }
        });

        bgThread.setDaemon(true);
        bgThread.setName("ClientSocket-Listener");
        bgThread.start();
    }

    private void handleMessage(String msg) {
        try {
            if (msg.startsWith("EVENT|")) {
                String[] parts = msg.split("\\|");
                // parts[0] = "EVENT"
                // parts[1] = auctionId
                // parts[2] = bidderName
                // parts[3] = bidAmount
                // parts[4] = currentPrice
                // parts[5] = leadingBidder
                // parts[6] = type (BID_PLACED/BID_REJECTED/AUCTION_ENDED)

                String auctionId = parts[1];
                String bidderName = parts[2];
                double bidAmount = Double.parseDouble(parts[3]);
                double currentPrice = Double.parseDouble(parts[4]);
                String leadingBidder = parts[5];
                AuctionEvent.Type type = AuctionEvent.Type.valueOf(parts[6]);

                AuctionEvent event = new AuctionEvent(type, auctionId, bidderName, leadingBidder, bidAmount, currentPrice);

                notifyObservers(event);
            } else if (msg.startsWith("OK|")) {
                System.out.println("[CLIENT] Server xac nhan: " + msg);
            } else if (msg.startsWith("FAIL|")) {
                System.out.println("[CLIENT] Server tu choi: " + msg);
            }
        } catch (Exception e) {
            System.out.println("[CLIENT] Loi parse: " + e.getMessage());
        }
    }
    // Gui lenh dang nhap len server
    public void sendLogin(String userName, String password) {
        if (out != null) {
            out.println("LOGIN|" + userName + "|" + password);
            System.out.println("[CLIENT] Gui Login: " + userName);
        }
    }
    // Gui lenh dat gia len server
    public void sendBid(String auctionId, double amount) {
        if (out != null) {
            out.println("BID|" + auctionId + "|" + amount);
            System.out.println("[CLIENT] Gui Bid: " + auctionId + " - " + amount);
        }
    }
    // Dang ki nhan thong bao phien dau gia
    public void subscribe(String auctionId) {
        if (out != null) {
            out.println("SUBSCRIBE|" + auctionId);
        }
    }
    // Huy dang ki nhan thong bao
    public void unsubscribe(String auctionId) {
        if (out != null) {
            out.println("UNSUBSCRIBE|" + auctionId);
        }
    }
    // Ngat ket noi khi log out
    public void disconnect() {
        running = false;
        try {
            if (socket != null) socket.close();
            System.out.println("[CLIENT] Đã ngắt kết nối.");
        } catch (IOException ignored) {}
    }



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
