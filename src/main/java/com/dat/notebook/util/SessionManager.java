package com.dat.notebook.util;

import com.dat.notebook.model.User;

/**
 * SessionManager - Quản lý session người dùng đang đăng nhập.
 * 
 * Singleton pattern để đảm bảo chỉ có 1 session duy nhất.
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class SessionManager {
    

    
    private static SessionManager instance;
    
    private SessionManager() {
        // Private constructor
    }
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    

    
    private User currentUser;
    private boolean isDemo = false;
    

    
    /**
     * Lưu user vào session sau khi đăng nhập thành công
     * 
     * @param user User đã đăng nhập
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.isDemo = false;
    }
    
    /**
     * Lấy user hiện tại đang đăng nhập
     * 
     * @return User hoặc null nếu chưa đăng nhập
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Lấy ID của user hiện tại
     * 
     * @return User ID hoặc -1 nếu chưa đăng nhập
     */
    public int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }
    
    /**
     * Lấy username của user hiện tại
     * 
     * @return Username hoặc null
     */
    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
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
     * Đăng xuất - xóa session
     */
    public void logout() {
        System.out.println("SessionManager: Đăng xuất user " + 
            (currentUser != null ? currentUser.getUsername() : "null"));
        this.currentUser = null;
        this.isDemo = false;
    }
    
    /**
     * Đăng nhập chế độ Demo
     */
    public void loginAsDemo() {
        User demoUser = new User();
        demoUser.setId(1);
        demoUser.setUsername("demo");
        demoUser.setFullName("Demo User");
        demoUser.setEmail("demo@smartnotebook.com");
        demoUser.setActive(true);
        
        this.currentUser = demoUser;
        this.isDemo = true;
        
        System.out.println("SessionManager: Đăng nhập chế độ Demo");
    }
    
    /**
     * Kiểm tra có đang ở chế độ Demo không
     * 
     * @return true nếu đang dùng tài khoản demo
     */
    public boolean isDemoMode() {
        return isDemo;
    }
    
    /**
     * Cập nhật thông tin user trong session
     * (Dùng sau khi user cập nhật profile)
     * 
     * @param user User đã cập nhật
     */
    public void updateCurrentUser(User user) {
        if (currentUser != null && user != null && currentUser.getId() == user.getId()) {
            this.currentUser = user;
        }
    }
}
