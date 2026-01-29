package com.dat.notebook.service;

import com.dat.notebook.model.User;
import com.dat.notebook.repository.UserRepository;
import com.dat.notebook.util.PasswordUtil;

import java.util.Optional;

/**
 * Service class xử lý logic xác thực và quản lý người dùng.
 * 
 * Chức năng:
 * - Đăng nhập / Đăng xuất
 * - Đăng ký tài khoản
 * - Quản lý session người dùng hiện tại
 * - Đổi mật khẩu
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class AuthService {
    

    
    private static AuthService instance;
    
    private AuthService() {
        this.userRepository = new UserRepository();
        this.userService = UserService.getInstance();
    }
    
    /**
     * Lấy instance của AuthService (Singleton pattern)
     * 
     * @return AuthService instance
     */
    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }
    

    
    private final UserRepository userRepository;
    private final UserService userService;
    private User currentUser;  // Người dùng đang đăng nhập
    

    
    /**
     * Đăng nhập với username và password
     * 
     * @param username Tên đăng nhập
     * @param password Mật khẩu (plain text)
     * @return User nếu đăng nhập thành công, null nếu thất bại
     */
    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        if (password == null || password.isEmpty()) {
            return null;
        }
        
        String passwordHash = PasswordUtil.hashPassword(password);
        Optional<User> userOpt = userRepository.authenticate(username.trim(), passwordHash);
        
        if (userOpt.isPresent()) {
            this.currentUser = userOpt.get();
            System.out.println("Đăng nhập thành công: " + currentUser.getUsername());
            return currentUser;
        }
        
        System.out.println("Đăng nhập thất bại: Sai username hoặc password");
        return null;
    }
    
    /**
     * Đăng xuất người dùng hiện tại
     */
    public void logout() {
        if (currentUser != null) {
            System.out.println("Đăng xuất: " + currentUser.getUsername());
        }
        this.currentUser = null;
    }
    
    /**
     * Kiểm tra đã đăng nhập chưa
     * 
     * @return true nếu đã đăng nhập
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Lấy người dùng hiện tại
     * 
     * @return User đang đăng nhập hoặc null
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Lấy ID người dùng hiện tại
     * 
     * @return User ID hoặc -1 nếu chưa đăng nhập
     */
    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }
    

    
    /**
     * Đăng ký tài khoản mới
     * 
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @param email Email (optional)
     * @param fullName Họ tên (optional)
     * @return User mới tạo hoặc null nếu thất bại
     */
    public User register(String username, String password, String email, String fullName) {
        // Validate input
        if (username == null || username.trim().isEmpty()) {
            System.err.println("Username không được để trống");
            return null;
        }
        
        if (password == null || password.length() < 6) {
            System.err.println("Mật khẩu phải có ít nhất 6 ký tự");
            return null;
        }
        
        // Check username exists
        if (userRepository.isUsernameExists(username.trim(), 0)) {
            System.err.println("Username đã tồn tại: " + username);
            return null;
        }
        
        // Create new user
        User newUser = new User();
        newUser.setUsername(username.trim());
        newUser.setPasswordHash(PasswordUtil.hashPassword(password));
        newUser.setEmail(email);
        newUser.setFullName(fullName);
        newUser.setActive(true);
        
        if (userRepository.insert(newUser)) {
            System.out.println("Đăng ký thành công: " + newUser.getUsername());
            return newUser;
        }
        
        System.err.println("Không thể tạo tài khoản mới");
        return null;
    }
    
    /**
     * Đăng ký đơn giản với username và password
     * 
     * @param username Tên đăng nhập
     * @param password Mật khẩu
     * @return User mới tạo hoặc null
     */
    public User register(String username, String password) {
        return register(username, password, null, null);
    }
    

    
    /**
     * Đổi mật khẩu cho người dùng hiện tại
     * 
     * @param oldPassword Mật khẩu cũ
     * @param newPassword Mật khẩu mới
     * @return true nếu đổi thành công
     */
    public boolean changePassword(String oldPassword, String newPassword) {
        if (currentUser == null) {
            System.err.println("Chưa đăng nhập");
            return false;
        }
        
        // Verify old password
        String oldHash = PasswordUtil.hashPassword(oldPassword);
        if (!oldHash.equals(currentUser.getPasswordHash())) {
            System.err.println("Mật khẩu cũ không đúng");
            return false;
        }
        
        // Validate new password
        if (newPassword == null || newPassword.length() < 6) {
            System.err.println("Mật khẩu mới phải có ít nhất 6 ký tự");
            return false;
        }
        
        // Update password
        String newHash = PasswordUtil.hashPassword(newPassword);
        if (userRepository.updatePassword(currentUser.getId(), newHash)) {
            currentUser.setPasswordHash(newHash);
            System.out.println("Đổi mật khẩu thành công");
            return true;
        }
        
        return false;
    }
    
    /**
     * Reset mật khẩu (dùng cho admin)
     * 
     * @param userId ID người dùng
     * @param newPassword Mật khẩu mới
     * @return true nếu thành công
     */
    public boolean resetPassword(int userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return false;
        }
        
        String newHash = PasswordUtil.hashPassword(newPassword);
        return userRepository.updatePassword(userId, newHash);
    }
    

    
    /**
     * Cập nhật thông tin profile
     * 
     * @param fullName Họ tên mới
     * @param email Email mới
     * @return true nếu thành công
     */
    public boolean updateProfile(String fullName, String email) {
        if (currentUser == null) {
            System.err.println("Chưa đăng nhập");
            return false;
        }
        
        // Update in database using UserService
        boolean success = userService.updateUserProfile(currentUser.getId(), fullName, email);
        
        if (success) {
            // Update current user object
            currentUser.setFullName(fullName);
            currentUser.setEmail(email);
            System.out.println("Cập nhật profile thành công");
            return true;
        }
        
        return false;
    }
    
    /**
     * Tìm user theo username
     * 
     * @param username Tên đăng nhập
     * @return Optional chứa User
     */
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    

    
    /**
     * Đăng nhập với tài khoản demo (không cần database)
     * Dùng khi không có kết nối database
     * 
     * @return User demo
     */
    public User loginAsDemo() {
        User demoUser = new User();
        demoUser.setId(1);
        demoUser.setUsername("demo");
        demoUser.setFullName("Demo User");
        demoUser.setEmail("demo@smartnotebook.com");
        demoUser.setActive(true);
        
        this.currentUser = demoUser;
        System.out.println("Đăng nhập chế độ Demo");
        return currentUser;
    }
    
    /**
     * Kiểm tra có đang ở chế độ demo không
     * 
     * @return true nếu đang dùng tài khoản demo
     */
    public boolean isDemoMode() {
        return currentUser != null && "demo".equals(currentUser.getUsername());
    }
}
