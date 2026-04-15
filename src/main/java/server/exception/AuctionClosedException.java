package server.exception;

public class AuctionClosedException extends Exception {
    //Ngoại lệ xảy ra khi phiên đấu giá đã đóng nhưng vẫn có thao tác đặt giá
    public AuctionClosedException(String message) {
        super(message);
    }
}