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
        String sql = "INSERT INTO users (name,tenHienThi,password,role) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getDisplayName());
            pstmt.setString(3,user.getPassword());
            pstmt.setString(4, user.getRole());
            pstmt.executeUpdate();
        }
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users where id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery();) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
            return null;
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery();) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
            return users;
        }
    }
    private User mapRowToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String displayName = rs.getString("tenHienThi");
        String password = rs.getString("password");
        String role = rs.getString("role");
        return switch (role){
            case "BIDDER" -> new Bidder(id,name,displayName,password);
            case "SELLER" -> new Seller(id,name,displayName,password);
            case "ADMIN" -> new Admin(id,name,displayName,password);
            default -> throw new SQLException("Unknown role: "+role);
        };
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE ID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }


    // Cập nhật tên và mật khẩu của user theo id.
    // Được gọi bởi AdminService.updateUser() và AuthenticationController.updateUser().
    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET tenHienThi = ?, password = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getDisplayName());
            pstmt.setString(2, user.getPassword());
            pstmt.setInt(3, user.getId());
            pstmt.executeUpdate();
        }
    }
}
