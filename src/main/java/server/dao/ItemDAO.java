package server.dao;

import server.model.item.Art;
import server.model.item.Electronics;
import server.model.item.Item;
import server.model.item.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {
    public void save(Item item) throws SQLException {
        String sql = "INSERT INTO items (id,name,base_price,description,item_type,seller_name," +
                "car_year,bien_so_xe,artist,warranty_months) VALUES (?,?,?,?,?,?,?,?,?,?) ";
        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1,item.getId());
        pstmt.setString(2,item.getName());
        pstmt.setDouble(3,item.getBasePrice());
        pstmt.setString(4,item.getDescription());
        pstmt.setString(5,item.getType());
        pstmt.setString(6,item.getSeller_name());
        if (item instanceof Vehicle) {
            Vehicle vehicle = (Vehicle) item;
            pstmt.setInt(7,vehicle.getCarYear());
            pstmt.setString(8,vehicle.getBienSoXe());
            pstmt.setString(9,null);
            pstmt.setNull(10, Types.INTEGER);
        }
        else if (item instanceof Art){
            Art art = (Art) item;
            pstmt.setNull(7, Types.INTEGER);
            pstmt.setString(8,null);
            pstmt.setString(9,art.getArtist());
            pstmt.setNull(10, Types.INTEGER);
        }
        else if (item instanceof Electronics){
            Electronics electronics = (Electronics) item;
            pstmt.setNull(7, Types.INTEGER);
            pstmt.setString(8,null);
            pstmt.setString(9,null);
            pstmt.setInt(10, electronics.getWarrantyMonths());
        }
        pstmt.executeUpdate();
    }
    public void delete(String itemId) throws SQLException {
        String sql = "DELETE FROM items WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, itemId);
        pstmt.executeUpdate();
    }
    public void update(Item item) throws SQLException {
        String sql = "UPDATE items SET name = ?, description = ?, base_price = ? WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, item.getName());
        pstmt.setString(2, item.getDescription());
        pstmt.setDouble(3, item.getBasePrice());
        pstmt.setString(4, item.getId());
        pstmt.executeUpdate();
    }

    public Item findById(String id) throws SQLException {
        String sql = "SELECT * FROM items where id = ?";
        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setString(1,id);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()){
            return mapRowToItem(rs);
        }
        return null;
    }
    public List<Item> findAll() throws SQLException{
        String sql = "SELECT * FROM items";
        List<Item> items = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()){
            items.add(mapRowToItem(rs));
        }
        return items;
    }
    private Item mapRowToItem(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        double base_price = rs.getDouble("base_price");
        String des = rs.getString("description");
        String item_type = rs.getString("item_type");
        String seller_name = rs.getString("seller_name");
        int car_year = rs.getInt("car_year");
        String bien_so_xe = rs.getString("bien_so_xe");
        String artist = rs.getString("artist");
        int wm = rs.getInt("warranty_months");
        return switch (item_type){
            case "ART" -> new Art(id,name,base_price,des,seller_name,artist);
            case "VEHICLE" -> new Vehicle(id,name,base_price,des,seller_name,car_year,bien_so_xe);
            case "ELECTRONICS" -> new Electronics(id,name,base_price,des,seller_name,wm);
            default -> throw new SQLException("Unknown item_type: "+item_type);
        };
    }
}
