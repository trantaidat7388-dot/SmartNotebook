package com.dat.notebook.repository;

import com.dat.notebook.config.DatabaseConfig;
import com.dat.notebook.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Simple UserRepository khớp với schema database thực tế.
 * 
 * Bảng User có cột: user_id, username, password_hash, created_at
 * 
 * @author SmartNotebook Team
 * @version 2.0 - Simplified for actual schema
 */
public class SimpleUserRepository {
    
    /**
     * Xác thực đăng nhập với username và password hash.
     * 
     * @param username Tên đăng nhập
     * @param passwordHash Mật khẩu đã hash (MD5)
     * @return Optional chứa User nếu xác thực thành công
     */
    public Optional<User> authenticate(String username, String passwordHash) {
        String sql = "SELECT user_id, username, password_hash, created_at " +
                    "FROM [User] WHERE username = ? AND password_hash = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setFullName(rs.getString("username")); // Dùng username làm display name
                    user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    user.setActive(true);
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi xác thực: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Kiểm tra username đã tồn tại chưa.
     * 
     * @param username Tên đăng nhập cần kiểm tra
     * @return true nếu username đã tồn tại
     */
    public boolean isUsernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM [User] WHERE username = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, username);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra username: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Tạo user mới (chỉ username và password).
     * 
     * @param username Tên đăng nhập
     * @param passwordHash Mật khẩu đã hash
     * @return ID của user mới tạo, hoặc -1 nếu thất bại
     */
    public int createUser(String username, String passwordHash) {
        return createUserWithEmail(username, passwordHash, null);
    }
    
    /**
     * Tạo user mới với email.
     * 
     * @param username Tên đăng nhập
     * @param passwordHash Mật khẩu đã hash (MD5)
     * @param email Email người dùng (có thể null)
     * @return ID của user mới tạo, hoặc -1 nếu thất bại
     */
    public int createUserWithEmail(String username, String passwordHash, String email) {
        String sql;
        boolean hasEmail = email != null && !email.trim().isEmpty();
        
        if (hasEmail) {
            sql = "INSERT INTO [User] (username, password_hash, email, created_at) " +
                  "VALUES (?, ?, ?, GETDATE()); SELECT SCOPE_IDENTITY();";
        } else {
            sql = "INSERT INTO [User] (username, password_hash, created_at) " +
                  "VALUES (?, ?, GETDATE()); SELECT SCOPE_IDENTITY();";
        }
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            if (hasEmail) {
                ps.setString(3, email);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    System.out.println("✅ Tạo user thành công! ID: " + newId);
                    return newId;
                }
            }
        } catch (SQLException e) {
            // Nếu lỗi do không có cột email, thử lại không có email
            if (hasEmail && e.getMessage().contains("Invalid column name 'email'")) {
                System.out.println("⚠️ Bảng User không có cột email, thử lại...");
                return createUserWithEmail(username, passwordHash, null);
            }
            System.err.println("❌ Lỗi tạo user: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }
    
    /**
     * Kiểm tra email đã tồn tại trong database chưa.
     * 
     * @param email Email cần kiểm tra
     * @return true nếu email đã tồn tại
     */
    public boolean isEmailExists(String email) {
        String sql = "SELECT COUNT(*) FROM [User] WHERE email = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, email);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            // Nếu cột email không tồn tại, bỏ qua
            if (e.getMessage().contains("Invalid column name")) {
                System.out.println("⚠️ Bảng User không có cột email");
                return false;
            }
            System.err.println("❌ Lỗi kiểm tra email: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Tìm user theo ID.
     * 
     * @param userId ID của user
     * @return Optional chứa User nếu tìm thấy
     */
    public Optional<User> findById(int userId) {
        String sql = "SELECT user_id, username, created_at FROM [User] WHERE user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setFullName(rs.getString("username"));
                    user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    user.setActive(true);
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi tìm user: " + e.getMessage());
        }
        return Optional.empty();
    }
}
