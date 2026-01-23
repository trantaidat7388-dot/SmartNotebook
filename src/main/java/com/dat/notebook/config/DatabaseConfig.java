package com.dat.notebook.config;

import com.dat.notebook.util.DBConnection;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Configuration class cho kết nối Database.
 * 
 * Đây chỉ là wrapper class để giữ compatibility với code hiện tại.
 * Logic kết nối thực tế nằm trong DBConnection.java (có TLS 1.0 hack).
 * 
 * @author SmartNotebook Team
 * @version 2.0 - Delegating to DBConnection
 */
public class DatabaseConfig {
    
    /**
     * Lấy connection tới database.
     * Delegate sang DBConnection để sử dụng TLS 1.0 hack.
     * 
     * @return Connection object
     * @throws SQLException nếu không thể kết nối
     */
    public static Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }
    
    /**
     * Thử lấy connection, trả về null nếu không thành công.
     * Phương thức này không throw exception.
     * 
     * @return Connection hoặc null
     */
    public static Connection tryGetConnection() {
        return DBConnection.tryGetConnection();
    }
    
    /**
     * Kiểm tra kết nối database có hoạt động không.
     * 
     * @return true nếu kết nối thành công
     */
    public static boolean testConnection() {
        return DBConnection.testConnection();
    }
}
