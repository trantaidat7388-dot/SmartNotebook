package com.dat.notebook.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity class đại diện cho người dùng trong hệ thống SmartNotebook.
 * 
 * Bảng: Users
 * Chức năng: Lưu trữ thông tin đăng nhập và profile người dùng
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class User {
    

    
    private int id;                    // UserID - Primary Key
    private String username;           // Tên đăng nhập (unique)
    private String passwordHash;       // Mật khẩu đã hash (SHA-256)
    private String email;              // Email người dùng
    private String fullName;           // Họ tên đầy đủ
    private String avatarUrl;          // Đường dẫn ảnh đại diện
    private boolean isActive;          // Trạng thái hoạt động
    private LocalDateTime createdAt;   // Thời điểm tạo
    private LocalDateTime updatedAt;   // Thời điểm cập nhật
    

    
    /**
     * Constructor mặc định
     */
    public User() {
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Constructor với thông tin cơ bản
     * 
     * @param username Tên đăng nhập
     * @param passwordHash Mật khẩu đã hash
     */
    public User(String username, String passwordHash) {
        this();
        this.username = username;
        this.passwordHash = passwordHash;
    }
    
    /**
     * Constructor đầy đủ
     * 
     * @param id ID người dùng
     * @param username Tên đăng nhập
     * @param passwordHash Mật khẩu đã hash
     * @param email Email
     * @param fullName Họ tên
     */
    public User(int id, String username, String passwordHash, String email, String fullName) {
        this(username, passwordHash);
        this.id = id;
        this.email = email;
        this.fullName = fullName;
    }
    

    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    

    
    /**
     * Lấy tên hiển thị (ưu tiên fullName, nếu không có thì dùng username)
     * 
     * @return Tên hiển thị
     */
    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        return username;
    }
    
    /**
     * Lấy chữ cái đầu của tên (dùng cho avatar)
     * 
     * @return Chữ cái đầu viết hoa
     */
    public String getInitials() {
        String displayName = getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            return "?";
        }
        
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return displayName.substring(0, Math.min(2, displayName.length())).toUpperCase();
    }
    

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && Objects.equals(username, user.username);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}
