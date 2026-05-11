package server;

import server.dao.AuctionDAO;
import server.dao.DataStorage;
import server.model.Auction;
import server.model.AuctionManager;
import server.network.ClientHandler;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ServerApp {
    public static void main (String[]args){
        System.out.println("=========================================");
        System.out.println("   HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN - SERVER  ");
        System.out.println("   Đang khởi động...                     ");
        System.out.println("=========================================");

        try {
            // 1. Load auctions từ DB vào AuctionManager (RAM)
            AuctionDAO auctionDAO = new AuctionDAO();
            AuctionManager manager = AuctionManager.getInstance();

            List<Auction> auctions = auctionDAO.findAll();
            for (Auction auction : auctions) {
                manager.addItem(auction);
            }
            System.out.println("[O] Đã load " + auctions.size() + " phiên đấu giá từ DB.");

            // 2. Mở cổng và chờ client kết nối
            ServerSocket serverSocket = new ServerSocket(8080);
            System.out.println("[O] Server đã sẵn sàng tại cổng 8080!");

            while (true) {
                Socket clientSocket = serverSocket.accept();   // chờ client kết nối
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();                   // mỗi client 1 thread riêng
                System.out.println("[O] Client mới kết nối!");
            }

        } catch (Exception e) {
            System.err.println("[X] Không thể khởi động Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

