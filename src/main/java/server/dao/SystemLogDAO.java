package server.dao;

import server.model.SystemLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import shared.dto.SystemLogDTO;
import java.time.format.DateTimeFormatter;


public class SystemLogDAO {

    public void save(SystemLog log) throws SQLException {
        String sql = "INSERT INTO system_log (admin_name, action, detail, created_at) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, log.getAdminName());
            pstmt.setString(2, log.getAction());
            pstmt.setString(3, log.getDetail());
            pstmt.setTimestamp(4, Timestamp.valueOf(log.getCreatedAt()));
            pstmt.executeUpdate();
        }
    }

    public List<SystemLogDTO> findAll() throws SQLException {
        String sql = "SELECT * FROM system_log ORDER BY created_at DESC";
        List<SystemLogDTO> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                result.add(new SystemLogDTO(
                        rs.getString("admin_name"),
                        rs.getString("action"),
                        rs.getString("detail"),
                        rs.getTimestamp("created_at").toLocalDateTime().format(fmt)
                ));
            }
        }
        return result;
    }

}

