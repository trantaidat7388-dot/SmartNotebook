package com.dat.notebook.model;

import java.time.LocalDateTime;

/**
 * Model đại diện cho một ghi chú
 */
public class Note {
    private int id;
    private String title;
    private String content;
    private int categoryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Note() {}

    public Note(int id, String title, String content, int categoryId, 
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters và Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
