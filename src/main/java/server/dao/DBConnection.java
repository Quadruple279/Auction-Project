package server.dao;

import java.sql.*;


public class DBConnection {

    private static final String URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;
    static{
        try{
            java.util.Properties props = new java.util.Properties();
            props.load(DBConnection.class.getClassLoader()
                    .getResourceAsStream("db.properties"));
            URL = props.getProperty("db.url");
            DB_USER = props.getProperty("db.user");
            DB_PASSWORD = props.getProperty("db.password");
        }
        catch (Exception e){
            throw new ExceptionInInitializerError(
                    "Không tìm thấy db.properties: " + e.getMessage()
            );
        }
    }
    public static Connection getConnection() throws SQLException{
        return DriverManager.getConnection(URL,DB_USER,DB_PASSWORD);
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
