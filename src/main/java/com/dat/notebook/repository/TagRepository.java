package com.dat.notebook.repository;

import com.dat.notebook.config.DatabaseConfig;
import com.dat.notebook.model.Tag;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository class cho Tag - Xử lý các thao tác CRUD với database.
 * 
 * Chức năng:
 * - CRUD operations cho Tag
 * - Quản lý quan hệ Note-Tag
 * - Gợi ý tags phổ biến
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class TagRepository {
    

    
    private static final String SQL_FIND_BY_ID = 
        "SELECT TagID, UserID, Name, Color, CreatedAt " +
        "FROM Tags WHERE TagID = ?";
    
    private static final String SQL_FIND_BY_USER = 
        "SELECT TagID, UserID, Name, Color, CreatedAt " +
        "FROM Tags WHERE UserID = ? ORDER BY Name";
    
    private static final String SQL_FIND_BY_NAME = 
        "SELECT TagID, UserID, Name, Color, CreatedAt " +
        "FROM Tags WHERE UserID = ? AND Name = ?";
    
    private static final String SQL_FIND_BY_NOTE = 
        "SELECT t.TagID, t.UserID, t.Name, t.Color, t.CreatedAt " +
        "FROM Tags t " +
        "INNER JOIN NoteTags nt ON t.TagID = nt.TagID " +
        "WHERE nt.NoteID = ? " +
        "ORDER BY t.Name";
    
    private static final String SQL_FIND_POPULAR = 
        "SELECT TOP(?) TagID, UserID, Name, Color, CreatedAt " +
        "FROM Tags WHERE UserID = ? " +
        "ORDER BY Name";
    
    private static final String SQL_SEARCH = 
        "SELECT TagID, UserID, Name, Color, CreatedAt " +
        "FROM Tags WHERE UserID = ? AND Name LIKE ? " +
        "ORDER BY Name";
    
    private static final String SQL_INSERT = 
        "INSERT INTO Tags (UserID, Name, Color, CreatedAt) " +
        "VALUES (?, ?, ?, GETDATE())";
    
    private static final String SQL_UPDATE = 
        "UPDATE Tags SET Name = ?, Color = ? WHERE TagID = ?";
    
    private static final String SQL_DELETE = 
        "DELETE FROM Tags WHERE TagID = ?";
    
    private static final String SQL_ADD_TAG_TO_NOTE = 
        "INSERT INTO NoteTags (NoteID, TagID) VALUES (?, ?)";
    
    private static final String SQL_REMOVE_TAG_FROM_NOTE = 
        "DELETE FROM NoteTags WHERE NoteID = ? AND TagID = ?";
    
    private static final String SQL_REMOVE_ALL_TAGS_FROM_NOTE = 
        "DELETE FROM NoteTags WHERE NoteID = ?";
    
    private static final String SQL_CHECK_NOTE_HAS_TAG = 
        "SELECT COUNT(*) FROM NoteTags WHERE NoteID = ? AND TagID = ?";
    

    
    /**
     * Tìm tag theo ID
     * 
     * @param id ID của tag
     * @return Optional chứa Tag nếu tìm thấy
     */
    public Optional<Tag> findById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding tag by ID: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Lấy tất cả tags của user
     * 
     * @param userId ID người dùng
     * @return Danh sách tags
     */
    public List<Tag> findByUser(int userId) {
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USER)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapResultSetToTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding tags by user: " + e.getMessage());
        }
        return tags;
    }
    
    /**
     * Tìm tag theo tên (cho user cụ thể)
     * 
     * @param userId ID người dùng
     * @param name Tên tag
     * @return Optional chứa Tag nếu tìm thấy
     */
    public Optional<Tag> findByName(int userId, String name) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NAME)) {
            
            ps.setInt(1, userId);
            ps.setString(2, Tag.normalizeTagName(name));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding tag by name: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Lấy tags của một ghi chú
     * 
     * @param noteId ID ghi chú
     * @return Danh sách tags
     */
    public List<Tag> findByNote(int noteId) {
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NOTE)) {
            
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapResultSetToTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding tags by note: " + e.getMessage());
        }
        return tags;
    }
    
    /**
     * Lấy tags phổ biến nhất
     * 
     * @param userId ID người dùng
     * @param limit Số lượng tối đa
     * @return Danh sách tags phổ biến
     */
    public List<Tag> findPopular(int userId, int limit) {
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_POPULAR)) {
            
            ps.setInt(1, limit);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapResultSetToTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding popular tags: " + e.getMessage());
        }
        return tags;
    }
    
    /**
     * Tìm kiếm tags theo từ khóa
     * 
     * @param userId ID người dùng
     * @param keyword Từ khóa
     * @return Danh sách tags khớp
     */
    public List<Tag> search(int userId, String keyword) {
        List<Tag> tags = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SEARCH)) {
            
            ps.setInt(1, userId);
            ps.setString(2, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tags.add(mapResultSetToTag(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching tags: " + e.getMessage());
        }
        return tags;
    }
    

    
    /**
     * Thêm tag mới
     * 
     * @param tag Tag cần thêm
     * @return true nếu thành công
     */
    public boolean insert(Tag tag) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, tag.getUserId());
            ps.setString(2, tag.getName());
            ps.setString(3, tag.getColor());
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        tag.setId(keys.getInt(1));
                    }
                }
                tag.setCreatedAt(LocalDateTime.now());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error inserting tag: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Tìm hoặc tạo tag mới
     * 
     * @param userId ID người dùng
     * @param tagName Tên tag
     * @return Tag tìm được hoặc tạo mới
     */
    public Tag findOrCreate(int userId, String tagName) {
        String normalizedName = Tag.normalizeTagName(tagName);
        Optional<Tag> existing = findByName(userId, normalizedName);
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        Tag newTag = new Tag(userId, normalizedName);
        if (insert(newTag)) {
            return newTag;
        }
        
        return null;
    }
    
    /**
     * Cập nhật tag
     * 
     * @param tag Tag cần cập nhật
     * @return true nếu thành công
     */
    public boolean update(Tag tag) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            
            ps.setString(1, tag.getName());
            ps.setString(2, tag.getColor());
            ps.setInt(3, tag.getId());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating tag: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Tăng số lần sử dụng - NOT IMPLEMENTED (UsageCount column removed)
     * 
     * @param tagId ID tag
     * @return true nếu thành công
     */
    public boolean incrementUsage(int tagId) {
        // UsageCount column đã bị xóa khỏi database schema
        return true;
        /*
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INCREMENT_USAGE)) {
            
            ps.setInt(1, tagId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error incrementing tag usage: " + e.getMessage());
        }
        return false;
        */
    }
    
    /**
     * Giảm số lần sử dụng - NOT IMPLEMENTED (UsageCount column removed)
     * 
     * @param tagId ID tag
     * @return true nếu thành công
     */
    public boolean decrementUsage(int tagId) {
        // UsageCount column đã bị xóa khỏi database schema
        return true;
        /*
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DECREMENT_USAGE)) {
            
            ps.setInt(1, tagId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error decrementing tag usage: " + e.getMessage());
        }
        return false;
        */
    }
    
    /**
     * Xóa tag
     * 
     * @param tagId ID tag cần xóa
     * @return true nếu thành công
     */
    public boolean delete(int tagId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            
            ps.setInt(1, tagId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting tag: " + e.getMessage());
        }
        return false;
    }
    

    
    /**
     * Gắn tag vào ghi chú
     * 
     * @param noteId ID ghi chú
     * @param tagId ID tag
     * @return true nếu thành công
     */
    public boolean addTagToNote(int noteId, int tagId) {
        // Kiểm tra đã có chưa
        if (noteHasTag(noteId, tagId)) {
            return true;
        }
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_ADD_TAG_TO_NOTE)) {
            
            ps.setInt(1, noteId);
            ps.setInt(2, tagId);
            
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                incrementUsage(tagId);
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error adding tag to note: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Gỡ tag khỏi ghi chú
     * 
     * @param noteId ID ghi chú
     * @param tagId ID tag
     * @return true nếu thành công
     */
    public boolean removeTagFromNote(int noteId, int tagId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_REMOVE_TAG_FROM_NOTE)) {
            
            ps.setInt(1, noteId);
            ps.setInt(2, tagId);
            
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                decrementUsage(tagId);
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error removing tag from note: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Gỡ tất cả tags khỏi ghi chú
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    public boolean removeAllTagsFromNote(int noteId) {
        // Lấy danh sách tags trước để giảm usage count
        List<Tag> tags = findByNote(noteId);
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_REMOVE_ALL_TAGS_FROM_NOTE)) {
            
            ps.setInt(1, noteId);
            ps.executeUpdate();
            
            // Giảm usage count cho tất cả tags
            for (Tag tag : tags) {
                decrementUsage(tag.getId());
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Error removing all tags from note: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Kiểm tra ghi chú có tag không
     * 
     * @param noteId ID ghi chú
     * @param tagId ID tag
     * @return true nếu có
     */
    public boolean noteHasTag(int noteId, int tagId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_CHECK_NOTE_HAS_TAG)) {
            
            ps.setInt(1, noteId);
            ps.setInt(2, tagId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking note has tag: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Cập nhật tags cho ghi chú (xóa cũ, thêm mới)
     * 
     * @param noteId ID ghi chú
     * @param tagIds Danh sách ID tags mới
     * @return true nếu thành công
     */
    public boolean updateNoteTags(int noteId, List<Integer> tagIds) {
        removeAllTagsFromNote(noteId);
        
        for (Integer tagId : tagIds) {
            if (!addTagToNote(noteId, tagId)) {
                return false;
            }
        }
        return true;
    }
    

    
    /**
     * Map ResultSet thành đối tượng Tag
     * 
     * @param rs ResultSet từ query
     * @return Tag object
     * @throws SQLException nếu có lỗi đọc dữ liệu
     */
    private Tag mapResultSetToTag(ResultSet rs) throws SQLException {
        Tag tag = new Tag();
        tag.setId(rs.getInt("TagID"));
        tag.setUserId(rs.getInt("UserID"));
        tag.setName(rs.getString("Name"));
        tag.setColor(rs.getString("Color"));
        
        try {
            tag.setUsageCount(rs.getInt("UsageCount"));
        } catch (SQLException e) {
            tag.setUsageCount(0); // Column không tồn tại
        }
        
        try {
            Timestamp createdAt = rs.getTimestamp("CreatedAt");
            if (createdAt != null) {
                tag.setCreatedAt(createdAt.toLocalDateTime());
            }
        } catch (SQLException e) {
            tag.setCreatedAt(LocalDateTime.now());
        }
        
        return tag;
    }
}
