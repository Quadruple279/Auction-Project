package server;

import server.dao.DataStorage;
import server.model.AuctionManager;
import java.net.ServerSocket;

public class ServerApp {
    public static void main(String[] args){
        DataStorage.loadData();
        System.out.println("=========================================");
        System.out.println("   HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN - SERVER  ");
        System.out.println("   Đang khởi động...                     ");
        System.out.println("=========================================");

        try {
            AuctionManager manager = AuctionManager.getInstance();
            System.out.println("[O] Đã khởi tạo trình quản lý đấu giá.");

            DataStorage.loadData();
            System.out.println("[O] Đã tải dữ liệu hệ thống.");

            ServerSocket serverSocket = new ServerSocket(8080);
            System.out.println("[O] Server đã sẵn sàng và đang chạy!");

            while (true) {
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            System.err.println("[X] Không thể khởi động Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
