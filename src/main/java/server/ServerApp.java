package server;

import server.dao.DataStorage;
import server.model.AuctionManager;
import java.net.ServerSocket;

public class ServerApp {
    public static void main(String[] args){
        System.out.println("=========================================");
        System.out.println("   HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN - SERVER  ");
        System.out.println("   Đang khởi động...                     ");
        System.out.println("=========================================");

        try {
             // 1. Khởi tạo các thành phần cốt lõi (Ví dụ: AuctionManager do Minh phụ trách)
             AuctionManager manager = AuctionManager.getInstance();
             System.out.println("[O] Đã khởi tạo trình quản lý đấu giá.");

            // 2. Tải dữ liệu từ file lên (Ví dụ: FileStorage do Nam/Lâm phụ trách sau này)
            DataStorage.loadData();
            System.out.println("[O] Đã tải dữ liệu hệ thống.");

            // 3. (Tuần 7) Mở cổng Socket lắng nghe Client kết nối
            ServerSocket serverSocket = new ServerSocket(8080);
            System.out.println("[O] Server đã sẵn sàng và đang chạy!");

            // Vòng lặp vô tận để giữ Server luôn mở (chờ cập nhật ở các tuần sau)
            while (true) {
                Thread.sleep(10000); // Tạm thời để Thread nghỉ, tránh ăn CPU
            }

        } catch (Exception e) {
            System.err.println("[X] Không thể khởi động Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
