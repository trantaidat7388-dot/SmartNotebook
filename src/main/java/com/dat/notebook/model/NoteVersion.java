package com.dat.notebook.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Model class đại diện cho phiên bản lịch sử của Note.
 * Mỗi lần lưu ghi chú sẽ tạo một version mới để có thể rollback.
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class NoteVersion {
    
    // ==================== FIELDS ====================
    
    private int versionId;              // Primary Key
    private int noteId;                 // Foreign Key - Note
    private String title;               // Tiêu đề tại thời điểm này
    private String htmlContent;         // Nội dung HTML
    private String plainTextContent;    // Nội dung plain text (để search)
    private int versionNumber;          // Version number (1, 2, 3,...)
    private LocalDateTime createdAt;    // Thời điểm tạo version
    private Integer createdBy;          // UserID người tạo version
    private String changeDescription;   // Mô tả thay đổi (optional)
    
    // Optional - thông tin người tạo
    private String createdByUsername;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Constructor mặc định
     */
    public NoteVersion() {
        this.createdAt = LocalDateTime.now();
        this.versionNumber = 1;
    }
    
    /**
     * Constructor với noteId
     */
    public NoteVersion(int noteId) {
        this();
        this.noteId = noteId;
    }
    
    /**
     * Constructor đầy đủ
     */
    public NoteVersion(int noteId, String title, String htmlContent, 
                       int versionNumber, Integer createdBy) {
        this();
        this.noteId = noteId;
        this.title = title;
        this.htmlContent = htmlContent;
        this.versionNumber = versionNumber;
        this.createdBy = createdBy;
        this.plainTextContent = stripHtmlTags(htmlContent);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Loại bỏ HTML tags để tạo plain text
     */
    private String stripHtmlTags(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
    
    /**
     * Tạo plain text từ HTML content
     */
    public void generatePlainText() {
        this.plainTextContent = stripHtmlTags(this.htmlContent);
    }
    
    /**
     * Lấy snippet của nội dung (100 ký tự đầu)
     */
    public String getContentPreview() {
        if (plainTextContent == null || plainTextContent.isEmpty()) {
            generatePlainText();
        }
        
        if (plainTextContent.length() <= 100) {
            return plainTextContent;
        }
        return plainTextContent.substring(0, 100) + "...";
    }
    
    /**
     * Format hiển thị version
     */
    public String getVersionLabel() {
        return "Version " + versionNumber;
    }
    
    /**
     * Kiểm tra có mô tả thay đổi không
     */
    public boolean hasChangeDescription() {
        return changeDescription != null && !changeDescription.trim().isEmpty();
    }
    
    // ==================== GETTERS & SETTERS ====================
    
    public int getVersionId() {
        return versionId;
    }
    
    public void setVersionId(int versionId) {
        this.versionId = versionId;
    }
    
    public int getNoteId() {
        return noteId;
    }
    
    public void setNoteId(int noteId) {
        this.noteId = noteId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getHtmlContent() {
        return htmlContent;
    }
    
    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
        // Auto-generate plain text khi set HTML
        generatePlainText();
    }
    
    public String getPlainTextContent() {
        return plainTextContent;
    }
    
    public void setPlainTextContent(String plainTextContent) {
        this.plainTextContent = plainTextContent;
    }
    
    public int getVersionNumber() {
        return versionNumber;
    }
    
    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Integer getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getChangeDescription() {
        return changeDescription;
    }
    
    public void setChangeDescription(String changeDescription) {
        this.changeDescription = changeDescription;
    }
    
    public String getCreatedByUsername() {
        return createdByUsername;
    }
    
    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }
    
    // ==================== EQUALS & HASHCODE ====================
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteVersion that = (NoteVersion) o;
        return versionId == that.versionId;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(versionId);
    }
    
    // ==================== TO STRING ====================
    
    @Override
    public String toString() {
        return "NoteVersion{" +
                "versionId=" + versionId +
                ", noteId=" + noteId +
                ", title='" + title + '\'' +
                ", versionNumber=" + versionNumber +
                ", createdAt=" + createdAt +
                ", createdBy=" + createdBy +
                ", changeDescription='" + changeDescription + '\'' +
                '}';
    }
}
