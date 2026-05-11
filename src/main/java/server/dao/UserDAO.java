package server.dao;

import server.model.user.Admin;
import server.model.user.Bidder;
import server.model.user.Seller;
import server.model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    public void save(User user) throws SQLException {
        String sql = "INSERT INTO users (id,name,password,role) VALUES (?,?,?,?)";
        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1,user.getId());
        pstmt.setString(2, user.getName());
        pstmt.setString(3,user.getPassword());
        pstmt.setString(4, user.getRole());
        pstmt.executeUpdate();
    }
    public User findById(String id) throws SQLException {
        String sql = "SELECT * FROM users where id = ?";
        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1,id);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()){
            return mapRowToUser(rs);
        }
        return null;
    }
    public List<User> findAll() throws SQLException{
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()){
            users.add(mapRowToUser(rs));
        }
        return users;
    }
    private User mapRowToUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String password = rs.getString("password");
        String role = rs.getString("role");
        return switch (role){
            case "BIDDER" -> new Bidder(id,name,password);
            case "SELLER" -> new Seller(id,name,password);
            case "ADMIN" -> new Admin(id,name,password);
            default -> throw new SQLException("Unknown role: "+role);
        };
    }
}
