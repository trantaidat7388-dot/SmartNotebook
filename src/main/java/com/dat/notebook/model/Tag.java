package com.dat.notebook.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity class đại diện cho Tag (nhãn/từ khóa) trong hệ thống SmartNotebook.
 * 
 * Bảng: Tags
 * Chức năng: Phân loại và gắn nhãn cho ghi chú, hỗ trợ tìm kiếm nhanh
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class Tag {
    

    
    private int id;                    // TagID - Primary Key
    private int userId;                // UserID - Foreign Key (tag thuộc về user nào)
    private String name;               // Tên tag (unique per user)
    private String color;              // Màu hiển thị (hex color)
    private int usageCount;            // Số lần sử dụng
    private LocalDateTime createdAt;   // Thời điểm tạo
    

    
    /**
     * Constructor mặc định
     */
    public Tag() {
        this.color = "#95a5a6";  // Màu xám mặc định
        this.usageCount = 0;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Constructor với tên tag
     * 
     * @param name Tên tag
     */
    public Tag(String name) {
        this();
        this.name = normalizeTagName(name);
    }
    
    /**
     * Constructor với user và tên
     * 
     * @param userId ID người dùng
     * @param name Tên tag
     */
    public Tag(int userId, String name) {
        this(name);
        this.userId = userId;
    }
    
    /**
     * Constructor đầy đủ
     * 
     * @param id ID tag
     * @param userId ID người dùng
     * @param name Tên tag
     * @param color Màu sắc
     */
    public Tag(int id, int userId, String name, String color) {
        this(userId, name);
        this.id = id;
        this.color = color;
    }
    

    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = normalizeTagName(name);
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public int getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    

    
    /**
     * Chuẩn hóa tên tag: lowercase, thay khoảng trắng bằng dấu gạch ngang
     * 
     * @param rawName Tên tag gốc
     * @return Tên tag đã chuẩn hóa
     */
    public static String normalizeTagName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "";
        }
        return rawName.trim()
                .toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-zA-Z0-9\\-àáảãạăắằẳẵặâấầẩẫậèéẻẽẹêếềểễệìíỉĩịòóỏõọôốồổỗộơớờởỡợùúủũụưứừửữựỳýỷỹỵđ]", "");
    }
    
    /**
     * Tăng số lần sử dụng
     */
    public void incrementUsage() {
        this.usageCount++;
    }
    
    /**
     * Giảm số lần sử dụng
     */
    public void decrementUsage() {
        if (this.usageCount > 0) {
            this.usageCount--;
        }
    }
    
    /**
     * Lấy tên hiển thị với ký tự #
     * 
     * @return Tên tag có prefix #
     */
    public String getDisplayName() {
        return "#" + name;
    }
    

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return userId == tag.userId && Objects.equals(name, tag.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, name);
    }
    
    @Override
    public String toString() {
        return "Tag{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", usageCount=" + usageCount +
                '}';
    }
}
