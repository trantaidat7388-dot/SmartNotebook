package com.dat.notebook.repository;

import com.dat.notebook.config.DatabaseConfig;
import com.dat.notebook.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository class cho User - Xử lý các thao tác CRUD với database.
 * 
 * Chức năng:
 * - Tìm kiếm user theo username, email
 * - Xác thực đăng nhập
 * - CRUD operations cho User
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class UserRepository {
    

    
    private static final String SQL_FIND_BY_ID = 
        "SELECT user_id, username, password_hash, email, full_name, is_active, created_at, updated_at " +
        "FROM [User] WHERE user_id = ?";
    
    private static final String SQL_FIND_BY_USERNAME = 
        "SELECT user_id, username, password_hash, email, full_name, is_active, created_at, updated_at " +
        "FROM [User] WHERE username = ?";
    
    private static final String SQL_FIND_BY_EMAIL = 
        "SELECT user_id, username, password_hash, email, full_name, is_active, created_at, updated_at " +
        "FROM [User] WHERE email = ?";
    
    private static final String SQL_FIND_ALL = 
        "SELECT user_id, username, password_hash, email, full_name, is_active, created_at, updated_at " +
        "FROM [User] WHERE is_active = 1 ORDER BY username";
    
    private static final String SQL_INSERT = 
        "INSERT INTO [User] (username, password_hash, email, full_name, is_active, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, 1, GETDATE(), GETDATE())";
    
    private static final String SQL_UPDATE = 
        "UPDATE [User] SET username = ?, email = ?, full_name = ?, updated_at = GETDATE() " +
        "WHERE user_id = ?";
    
    private static final String SQL_UPDATE_PASSWORD = 
        "UPDATE [User] SET password_hash = ?, updated_at = GETDATE() WHERE user_id = ?";
    
    private static final String SQL_DELETE = 
        "UPDATE [User] SET is_active = 0, updated_at = GETDATE() WHERE user_id = ?";
    
    private static final String SQL_CHECK_USERNAME_EXISTS = 
        "SELECT COUNT(*) FROM [User] WHERE username = ? AND user_id != ?";
    
    private static final String SQL_AUTHENTICATE = 
        "SELECT user_id, username, password_hash, email, full_name, is_active, created_at, updated_at " +
        "FROM [User] WHERE username = ? AND password_hash = ?";
    

    
    /**
     * Tìm user theo ID
     * 
     * @param id ID của user
     * @return Optional chứa User nếu tìm thấy
     */
    public Optional<User> findById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Tìm user theo username
     * 
     * @param username Tên đăng nhập
     * @return Optional chứa User nếu tìm thấy
     */
    public Optional<User> findByUsername(String username) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USERNAME)) {
            
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by username: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Tìm user theo email
     * 
     * @param email Email
     * @return Optional chứa User nếu tìm thấy
     */
    public Optional<User> findByEmail(String email) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_EMAIL)) {
            
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by email: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Lấy tất cả users đang hoạt động
     * 
     * @return Danh sách users
     */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all users: " + e.getMessage());
        }
        return users;
    }
    

    
    /**
     * Xác thực đăng nhập
     * 
     * @param username Tên đăng nhập
     * @param passwordHash Mật khẩu đã hash
     * @return Optional chứa User nếu xác thực thành công
     */
    public Optional<User> authenticate(String username, String passwordHash) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_AUTHENTICATE)) {
            
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Kiểm tra username đã tồn tại chưa
     * 
     * @param username Tên đăng nhập cần kiểm tra
     * @param excludeUserId ID user cần loại trừ (dùng khi update)
     * @return true nếu username đã tồn tại
     */
    public boolean isUsernameExists(String username, int excludeUserId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_CHECK_USERNAME_EXISTS)) {
            
            ps.setString(1, username);
            ps.setInt(2, excludeUserId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking username exists: " + e.getMessage());
        }
        return false;
    }
    

    
    /**
     * Thêm user mới
     * 
     * @param user User cần thêm
     * @return true nếu thành công
     */
    public boolean insert(User user) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getFullName());
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error inserting user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Cập nhật thông tin user
     * 
     * @param user User cần cập nhật
     * @return true nếu thành công
     */
    public boolean update(User user) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getFullName());
            ps.setInt(4, user.getId());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Cập nhật mật khẩu
     * 
     * @param userId ID user
     * @param newPasswordHash Mật khẩu mới đã hash
     * @return true nếu thành công
     */
    public boolean updatePassword(int userId, String newPasswordHash) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {
            
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Xóa user (soft delete - chỉ đánh dấu inactive)
     * 
     * @param userId ID user cần xóa
     * @return true nếu thành công
     */
    public boolean delete(int userId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
        return false;
    }
    

    
    /**
     * Map ResultSet thành đối tượng User
     * 
     * @param rs ResultSet từ query
     * @return User object
     * @throws SQLException nếu có lỗi đọc dữ liệu
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        
        // Email và full_name có thể null
        try {
            user.setEmail(rs.getString("email"));
        } catch (SQLException e) {
            user.setEmail("");
        }
        
        try {
            user.setFullName(rs.getString("full_name"));
        } catch (SQLException e) {
            user.setFullName(rs.getString("username"));
        }
        
        user.setAvatarUrl("");
        
        try {
            user.setActive(rs.getBoolean("is_active"));
        } catch (SQLException e) {
            user.setActive(true);
        }
        
        try {
            Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                user.setCreatedAt(createdAt.toLocalDateTime());
            }
        } catch (SQLException e) {
            user.setCreatedAt(LocalDateTime.now());
        }
        
        try {
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                user.setUpdatedAt(updatedAt.toLocalDateTime());
            } else {
                user.setUpdatedAt(LocalDateTime.now());
            }
        } catch (SQLException e) {
            user.setUpdatedAt(LocalDateTime.now());
        }
        
        return user;
    }
}
