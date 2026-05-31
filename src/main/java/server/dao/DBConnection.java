package server.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.Properties;


public class DBConnection {
    private static HikariDataSource pool;

    static {
        try {
            Properties props = new Properties();
            props.load(DBConnection.class.getClassLoader()
                    .getResourceAsStream("db.properties"));

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(props.getProperty("db.url"));
            config.setUsername(props.getProperty("db.user"));
            config.setPassword(props.getProperty("db.password"));
            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(5000);
            pool = new HikariDataSource(config);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return pool.getConnection(); // lấy connection từ pool, không mở mới
    }
}
