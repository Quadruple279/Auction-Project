package server.exception;

public class AuthenticationException extends Exception{
    //Ngoại lệ xảy ra khi có lỗi xác thực người dùng hoặc quyền truy cập
    public AuthenticationException(String message) {
        super(message);
    }
}
