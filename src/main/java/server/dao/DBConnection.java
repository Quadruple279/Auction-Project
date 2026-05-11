package server.dao;

import java.sql.*;


public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/dbdaugia?autoReconnect=true&useSSL=false";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";
    private static Connection connecton;
    public static Connection getConnection() throws SQLException{
        //Nạp Driver
        if (connecton == null || connecton.isClosed()){
            connecton = DriverManager.getConnection(URL,DB_USER,DB_PASSWORD);
        }
        return connecton;
    }

    public static void main(String[] args) throws SQLException {
        Connection con = getConnection();
        if (con != null){
            System.out.println("Successful");
        }
        else{
            System.out.println("Failed");
        }
    }
}
