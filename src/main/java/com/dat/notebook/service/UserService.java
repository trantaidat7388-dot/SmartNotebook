package com.dat.notebook.service;

import com.dat.notebook.config.DatabaseConfig;
import com.dat.notebook.model.User;
import com.dat.notebook.repository.SimpleUserRepository;
import com.dat.notebook.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Service class xử lý logic nghiệp vụ cho User.
 * 
 * Chức năng:
 * - Đăng ký tài khoản mới
 * - Kiểm tra dữ liệu hợp lệ
 * - Kiểm tra username/email trùng lặp
 * - Insert user vào database
 * 
 * Lớp này đóng vai trò trung gian giữa Controller và Repository,
 * xử lý business logic và trả về kết quả chi tiết.
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class UserService {
    
    // ==================== SINGLETON PATTERN ====================
    
    /** Instance duy nhất của UserService */
    private static UserService instance;
    
    /** Constructor private để đảm bảo Singleton */
    private UserService() {
        this.userRepository = new SimpleUserRepository();
    }
    
    /**
     * Lấy instance của UserService (Singleton)
     * 
     * @return UserService instance
     */
    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }
    
    // ==================== FIELDS ====================
    
    /** Repository để thao tác với database */
    private final SimpleUserRepository userRepository;
    
    // ==================== INNER CLASS: Registration Result ====================
    
    /**
     * Class chứa kết quả đăng ký với status và message chi tiết.
     * Dùng để trả về thông tin chi tiết cho Controller hiển thị.
     */
    public static class RegistrationResult {
        
        /** Enum các trạng thái kết quả đăng ký */
        public enum Status {
            SUCCESS,           // Đăng ký thành công
            USERNAME_EXISTS,   // Username đã tồn tại
            EMAIL_EXISTS,      // Email đã tồn tại
            INVALID_DATA,      // Dữ liệu không hợp lệ
            CONNECTION_ERROR,  // Lỗi kết nối database
            DATABASE_ERROR     // Lỗi database khác
        }
        
        private final Status status;
        private final String message;
        private final User user;  // User mới tạo (nếu thành công)
        
        /**
         * Constructor kết quả đăng ký
         * 
         * @param status Trạng thái kết quả
         * @param message Thông điệp chi tiết
         * @param user User mới tạo (null nếu thất bại)
         */
        public RegistrationResult(Status status, String message, User user) {
            this.status = status;
            this.message = message;
            this.user = user;
        }
        
        /**
         * Constructor kết quả thất bại (không có user)
         */
        public RegistrationResult(Status status, String message) {
            this(status, message, null);
        }
        
        // Getters
        public Status getStatus() { return status; }
        public String getMessage() { return message; }
        public User getUser() { return user; }
        
        /**
         * Kiểm tra đăng ký có thành công không
         * @return true nếu thành công
         */
        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }
    }
    
    // ==================== REGISTRATION METHODS ====================
    
    /**
     * Đăng ký tài khoản mới với đầy đủ thông tin.
     * 
     * Quy trình:
     * 1. Validate dữ liệu đầu vào
     * 2. Kiểm tra username đã tồn tại chưa
     * 3. Kiểm tra email đã tồn tại chưa (nếu có)
     * 4. Hash mật khẩu bằng MD5
     * 5. Insert user mới vào database
     * 
     * @param username Tên đăng nhập (bắt buộc)
     * @param password Mật khẩu (bắt buộc, tối thiểu 6 ký tự)
     * @param email Email (tùy chọn)
     * @return RegistrationResult chứa trạng thái và thông điệp
     */
    public RegistrationResult registerUser(String username, String password, String email) {
        
        System.out.println("=== BẮT ĐẦU ĐĂNG KÝ USER ===");
        System.out.println("Username: " + username);
        System.out.println("Email: " + (email != null && !email.isEmpty() ? email : "(không có)"));
        
        // ===== BƯỚC 1: Validate dữ liệu =====
        
        // 1.1 Kiểm tra username
        if (username == null || username.trim().isEmpty()) {
            System.out.println("❌ Lỗi: Username rỗng");
            return new RegistrationResult(
                RegistrationResult.Status.INVALID_DATA, 
                "Tên đăng nhập không được để trống"
            );
        }
        
        username = username.trim();
        
        // 1.2 Kiểm tra độ dài username
        if (username.length() < 3 || username.length() > 20) {
            System.out.println("❌ Lỗi: Username không hợp lệ (độ dài)");
            return new RegistrationResult(
                RegistrationResult.Status.INVALID_DATA, 
                "Tên đăng nhập phải từ 3-20 ký tự"
            );
        }
        
        // 1.3 Kiểm tra password
        if (password == null || password.length() < 6) {
            System.out.println("❌ Lỗi: Password không hợp lệ");
            return new RegistrationResult(
                RegistrationResult.Status.INVALID_DATA, 
                "Mật khẩu phải có ít nhất 6 ký tự"
            );
        }
        
        // ===== BƯỚC 2: Kiểm tra kết nối database =====
        
        if (!DatabaseConfig.testConnection()) {
            System.out.println("❌ Lỗi: Không kết nối được database");
            return new RegistrationResult(
                RegistrationResult.Status.CONNECTION_ERROR, 
                "Không thể kết nối đến cơ sở dữ liệu"
            );
        }
        
        // ===== BƯỚC 3: Kiểm tra username đã tồn tại =====
        
        try {
            if (isUsernameExists(username)) {
                System.out.println("❌ Lỗi: Username '" + username + "' đã tồn tại");
                return new RegistrationResult(
                    RegistrationResult.Status.USERNAME_EXISTS, 
                    "Tên đăng nhập đã được sử dụng"
                );
            }
            System.out.println("✓ Username chưa tồn tại");
        } catch (SQLException e) {
            System.out.println("❌ Lỗi SQL khi kiểm tra username: " + e.getMessage());
            return new RegistrationResult(
                RegistrationResult.Status.DATABASE_ERROR, 
                "Lỗi khi kiểm tra tên đăng nhập: " + e.getMessage()
            );
        }
        
        // ===== BƯỚC 4: Kiểm tra email đã tồn tại (nếu có) =====
        
        if (email != null && !email.trim().isEmpty()) {
            email = email.trim();
            try {
                if (isEmailExists(email)) {
                    System.out.println("❌ Lỗi: Email '" + email + "' đã tồn tại");
                    return new RegistrationResult(
                        RegistrationResult.Status.EMAIL_EXISTS, 
                        "Email đã được đăng ký"
                    );
                }
                System.out.println("✓ Email chưa tồn tại");
            } catch (SQLException e) {
                System.out.println("❌ Lỗi SQL khi kiểm tra email: " + e.getMessage());
                return new RegistrationResult(
                    RegistrationResult.Status.DATABASE_ERROR, 
                    "Lỗi khi kiểm tra email: " + e.getMessage()
                );
            }
        }
        
        // ===== BƯỚC 5: Hash mật khẩu =====
        
        String passwordHash = PasswordUtil.hashPassword(password);
        System.out.println("✓ Đã hash mật khẩu (MD5)");
        
        // ===== BƯỚC 6: Insert user vào database =====
        
        try {
            int userId = createUserWithEmail(username, passwordHash, email);
            
            if (userId > 0) {
                System.out.println("✅ ĐĂNG KÝ THÀNH CÔNG! User ID: " + userId);
                
                // Tạo object User để trả về
                User newUser = new User();
                newUser.setId(userId);
                newUser.setUsername(username);
                newUser.setEmail(email);
                newUser.setActive(true);
                
                return new RegistrationResult(
                    RegistrationResult.Status.SUCCESS, 
                    "Đăng ký tài khoản thành công!",
                    newUser
                );
            } else {
                System.out.println("❌ Không thể tạo user (ID = -1)");
                return new RegistrationResult(
                    RegistrationResult.Status.DATABASE_ERROR, 
                    "Không thể tạo tài khoản mới"
                );
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi SQL khi insert user: " + e.getMessage());
            e.printStackTrace();
            return new RegistrationResult(
                RegistrationResult.Status.DATABASE_ERROR, 
                "Lỗi khi tạo tài khoản: " + e.getMessage()
            );
        }
    }
    
    /**
     * Đăng ký tài khoản không có email (overload method)
     * 
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @return RegistrationResult
     */
    public RegistrationResult registerUser(String username, String password) {
        return registerUser(username, password, null);
    }
    
    // ==================== DATABASE METHODS ====================
    
    /**
     * Kiểm tra username đã tồn tại trong database chưa
     * 
     * @param username Tên đăng nhập cần kiểm tra
     * @return true nếu username đã tồn tại
     * @throws SQLException Nếu có lỗi database
     */
    public boolean isUsernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM [User] WHERE username = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, username);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
    
    /**
     * Kiểm tra email đã tồn tại trong database chưa
     * 
     * @param email Email cần kiểm tra
     * @return true nếu email đã tồn tại
     * @throws SQLException Nếu có lỗi database
     */
    public boolean isEmailExists(String email) throws SQLException {
        // Kiểm tra xem bảng User có cột email không
        // Nếu không có, trả về false (không kiểm tra)
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
            // Nếu cột email không tồn tại, bỏ qua lỗi
            if (e.getMessage().contains("Invalid column name")) {
                System.out.println("⚠️ Bảng User không có cột email, bỏ qua kiểm tra");
                return false;
            }
            throw e;
        }
        return false;
    }
    
    /**
     * Tạo user mới với email (nếu có)
     * 
     * @param username Tên đăng nhập
     * @param passwordHash Mật khẩu đã hash
     * @param email Email (có thể null)
     * @return ID của user mới tạo, hoặc -1 nếu thất bại
     * @throws SQLException Nếu có lỗi database
     */
    private int createUserWithEmail(String username, String passwordHash, String email) throws SQLException {
        // SQL insert - tùy thuộc vào schema có cột email hay không
        String sql;
        boolean hasEmail = email != null && !email.trim().isEmpty();
        
        // Thử với schema có cột email trước
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
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            // Nếu lỗi do không có cột email, thử lại không có email
            if (hasEmail && e.getMessage().contains("Invalid column name 'email'")) {
                System.out.println("⚠️ Bảng User không có cột email, insert không có email");
                return createUserWithEmail(username, passwordHash, null);
            }
            throw e;
        }
        
        return -1;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Validate định dạng email
     * 
     * @param email Email cần kiểm tra
     * @return true nếu email hợp lệ
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return true;  // Email không bắt buộc
        }
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
    
    /**
     * Validate username
     * 
     * @param username Tên đăng nhập cần kiểm tra
     * @return true nếu username hợp lệ
     */
    public boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        // 3-20 ký tự, chỉ chữ cái, số và underscore
        return username.matches("^[a-zA-Z0-9_]{3,20}$");
    }
    
    /**
     * Validate password
     * 
     * @param password Mật khẩu cần kiểm tra
     * @return true nếu password hợp lệ
     */
    public boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
    
    // ==================== UPDATE PROFILE METHODS ====================
    
    /**
     * Cập nhật thông tin profile user (full name và email)
     * 
     * @param userId ID của user
     * @param fullName Tên hiển thị mới
     * @param email Email mới
     * @return true nếu cập nhật thành công
     */
    public boolean updateUserProfile(int userId, String fullName, String email) {
        System.out.println("=== CẬP NHẬT PROFILE USER ===");
        System.out.println("User ID: " + userId);
        System.out.println("Full Name: " + fullName);
        System.out.println("Email: " + email);
        
        // Validate email if provided
        if (email != null && !email.trim().isEmpty()) {
            if (!isValidEmail(email.trim())) {
                System.out.println("❌ Lỗi: Email không hợp lệ");
                return false;
            }
            
            // Check email exists (exclude current user)
            try {
                if (isEmailExistsExcludeUser(email.trim(), userId)) {
                    System.out.println("❌ Lỗi: Email đã được sử dụng bởi user khác");
                    return false;
                }
            } catch (SQLException e) {
                System.out.println("❌ Lỗi SQL khi kiểm tra email: " + e.getMessage());
                return false;
            }
        }
        
        // Update database
        String sql = "UPDATE [User] SET full_name = ?, email = ?, updated_at = GETDATE() WHERE user_id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setInt(3, userId);
            
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Cập nhật profile thành công!");
                return true;
            } else {
                System.out.println("❌ Không tìm thấy user để cập nhật");
                return false;
            }
            
        } catch (SQLException e) {
            System.out.println("❌ Lỗi SQL khi cập nhật profile: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Kiểm tra email đã tồn tại (loại trừ user hiện tại)
     * 
     * @param email Email cần kiểm tra
     * @param excludeUserId ID user cần loại trừ
     * @return true nếu email đã tồn tại
     * @throws SQLException Nếu có lỗi database
     */
    private boolean isEmailExistsExcludeUser(String email, int excludeUserId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM [User] WHERE email = ? AND user_id != ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, email);
            ps.setInt(2, excludeUserId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            // Nếu cột email không tồn tại, bỏ qua lỗi
            if (e.getMessage().contains("Invalid column name")) {
                System.out.println("⚠️ Bảng User không có cột email, bỏ qua kiểm tra");
                return false;
            }
            throw e;
        }
        return false;
    }
}
