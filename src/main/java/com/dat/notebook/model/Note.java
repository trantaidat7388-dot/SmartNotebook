package com.dat.notebook.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Import các class cùng package (không bắt buộc nhưng rõ ràng hơn)
// Các class Tag, Category, User đều ở cùng package nên Java tự động nhận diện

/**
 * Entity class đại diện cho Ghi chú trong hệ thống SmartNotebook.
 * 
 * Bảng: Notes
 * Chức năng: Lưu trữ nội dung ghi chú với các thuộc tính thông minh
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class Note {



    /** Các trạng thái của ghi chú */
    public static final String STATUS_REGULAR = "REGULAR";
    public static final String STATUS_URGENT = "URGENT";
    public static final String STATUS_IDEAS = "IDEAS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    /** Màu mặc định */
    public static final String DEFAULT_COLOR = "#ffffff";



    private int id; // NoteID - Primary Key
    private int userId; // UserID - Foreign Key
    private Integer categoryId; // CategoryID - Foreign Key (nullable)
    private String title; // Tiêu đề ghi chú
    private String content; // Nội dung ghi chú (plain text - backward compatibility)
    private String htmlContent; // Nội dung HTML (Rich Text Editor)
    private String summary; // Tóm tắt tự động (Smart feature)
    private String status; // Trạng thái: REGULAR, URGENT, IDEAS, COMPLETED
    private boolean isFavorite; // Đánh dấu yêu thích
    private boolean isArchived; // Đã lưu trữ
    private String color; // Màu nền ghi chú
    private int viewCount; // Số lần xem
    private LocalDateTime createdAt; // Thời điểm tạo
    private LocalDateTime updatedAt; // Thời điểm cập nhật

    // Quan hệ
    private List<Tag> tags; // Danh sách tags
    private Category category; // Danh mục (optional)
    private User user; // Người tạo

    // Version history (optional - load when needed)
    private Integer versionCount; // Số lượng versions
    private Integer latestVersion; // Version number mới nhất



    /**
     * Constructor mặc định
     */
    public Note() {
        this.status = STATUS_REGULAR;
        this.isFavorite = false;
        this.isArchived = false;
        this.color = DEFAULT_COLOR;
        this.viewCount = 0;
        this.tags = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Constructor với tiêu đề
     * 
     * @param title Tiêu đề ghi chú
     */
    public Note(String title) {
        this();
        this.title = title;
    }

    /**
     * Constructor với tiêu đề và nội dung
     * 
     * @param title   Tiêu đề
     * @param content Nội dung
     */
    public Note(String title, String content) {
        this(title);
        this.content = content;
    }

    /**
     * Constructor với user
     * 
     * @param userId  ID người dùng
     * @param title   Tiêu đề
     * @param content Nội dung
     */
    public Note(int userId, String title, String content) {
        this(title, content);
        this.userId = userId;
    }

    /**
     * Constructor đầy đủ từ database
     */
    public Note(int id, int userId, Integer categoryId, String title, String content,
            String summary, String status, boolean isFavorite, boolean isArchived,
            String color, int viewCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.categoryId = categoryId;
        this.title = title;
        this.content = content;
        this.summary = summary;
        this.status = status != null ? status : STATUS_REGULAR;
        this.isFavorite = isFavorite;
        this.isArchived = isArchived;
        this.color = color != null ? color : DEFAULT_COLOR;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.tags = new ArrayList<>();
    }

    // Constructor cũ để tương thích ngược
    public Note(int id, String title, String content, int categoryId,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this();
        this.id = id;
        this.title = title;
        this.content = content;
        this.categoryId = categoryId > 0 ? categoryId : null;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Note(int id, String title, String content, int categoryId, String status,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, title, content, categoryId, createdAt, updatedAt);
        this.status = status != null ? status : STATUS_REGULAR;
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

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    // Backward compatibility
    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId > 0 ? categoryId : null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Lấy HTML content (ưu tiên htmlContent, fallback về content)
     */
    public String getHtmlContent() {
        // Nếu có htmlContent thì dùng
        if (htmlContent != null && !htmlContent.trim().isEmpty()) {
            return htmlContent;
        }
        // Fallback: convert plain content sang HTML đơn giản
        if (content != null && !content.trim().isEmpty()) {
            return convertPlainToHtml(content);
        }
        return "";
    }

    /**
     * Set HTML content và auto-update plain content
     */
    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
        // Auto-sync plain content từ HTML
        this.content = stripHtmlTags(htmlContent);
    }

    /**
     * Convert plain text sang HTML đơn giản
     */
    private String convertPlainToHtml(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        // Convert line breaks to <p> tags
        String[] lines = plainText.split("\\r?\\n");
        StringBuilder html = new StringBuilder();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                html.append("<p>").append(escapeHtml(line)).append("</p>");
            }
        }
        return html.toString();
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Strip HTML tags để lấy plain text
     */
    private String stripHtmlTags(String html) {
        if (html == null)
            return "";
        return html.replaceAll("<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (isValidStatus(status)) {
            this.status = status;
        }
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color != null ? color : DEFAULT_COLOR;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
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

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
        if (category != null) {
            this.categoryId = category.getId();
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }



    /**
     * Kiểm tra trạng thái hợp lệ
     * 
     * @param status Trạng thái cần kiểm tra
     * @return true nếu hợp lệ
     */
    public static boolean isValidStatus(String status) {
        return STATUS_REGULAR.equals(status) ||
                STATUS_URGENT.equals(status) ||
                STATUS_IDEAS.equals(status) ||
                STATUS_COMPLETED.equals(status);
    }

    /**
     * Lấy preview nội dung (50 ký tự đầu)
     * 
     * @return Preview content
     */
    public String getPreview() {
        return getPreview(100);
    }

    /**
     * Lấy preview nội dung với độ dài tùy chỉnh
     * 
     * @param maxLength Độ dài tối đa
     * @return Preview content
     */
    public String getPreview(int maxLength) {
        // Ưu tiên lấy từ plain content
        String text = (content != null && !content.isEmpty()) ? content : stripHtmlTags(htmlContent);
        if (text == null || text.isEmpty()) {
            return "";
        }
        String cleaned = text.replaceAll("[\\r\\n]+", " ").trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, maxLength) + "...";
    }

    /**
     * Lấy số từ trong nội dung
     * 
     * @return Số từ
     */
    public int getWordCount() {
        String text = (content != null && !content.isEmpty()) ? content : stripHtmlTags(htmlContent);
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    /**
     * Lấy số ký tự trong nội dung
     * 
     * @return Số ký tự
     */
    public int getCharacterCount() {
        String text = (content != null && !content.isEmpty()) ? content : stripHtmlTags(htmlContent);
        return text != null ? text.length() : 0;
    }

    /**
     * Kiểm tra có version history không
     */
    public boolean hasVersionHistory() {
        return versionCount != null && versionCount > 0;
    }

    public Integer getVersionCount() {
        return versionCount;
    }

    public void setVersionCount(Integer versionCount) {
        this.versionCount = versionCount;
    }

    public Integer getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(Integer latestVersion) {
        this.latestVersion = latestVersion;
    }

    /**
     * Thêm tag vào ghi chú
     * 
     * @param tag Tag cần thêm
     */
    public void addTag(Tag tag) {
        if (tag != null && !tags.contains(tag)) {
            tags.add(tag);
        }
    }

    /**
     * Xóa tag khỏi ghi chú
     * 
     * @param tag Tag cần xóa
     */
    public void removeTag(Tag tag) {
        tags.remove(tag);
    }

    /**
     * Tăng số lần xem
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * Kiểm tra ghi chú có trống không
     * 
     * @return true nếu không có nội dung
     */
    public boolean isEmpty() {
        return (title == null || title.trim().isEmpty()) &&
                (content == null || content.trim().isEmpty());
    }

    /**
     * Đánh dấu hoàn thành
     */
    public void markAsCompleted() {
        this.status = STATUS_COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Đánh dấu quan trọng
     */
    public void markAsUrgent() {
        this.status = STATUS_URGENT;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Toggle yêu thích
     */
    public void toggleFavorite() {
        this.isFavorite = !this.isFavorite;
        this.updatedAt = LocalDateTime.now();
    }



    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Note note = (Note) o;
        return id == note.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", isFavorite=" + isFavorite +
                ", tags=" + tags.size() +
                ", createdAt=" + createdAt +
                '}';
    }
}
