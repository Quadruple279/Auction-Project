package server.network;

import java.net.Socket;

public class ClientHandler implements Runnable{
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // Tuần tới Minh và Nam sẽ làm phần Giao thức (Message) và Serialization ở đây
            System.out.println("Đang xử lý luồng cho Client: " + Thread.currentThread().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
