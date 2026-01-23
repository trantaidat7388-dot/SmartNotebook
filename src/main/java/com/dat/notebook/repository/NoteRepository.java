package com.dat.notebook.repository;

import com.dat.notebook.config.DatabaseConfig;
import com.dat.notebook.model.Note;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository class cho Note - Xử lý các thao tác CRUD với database.
 * 
 * Chức năng:
 * - CRUD operations cho Note
 * - Tìm kiếm, lọc ghi chú
 * - Thống kê ghi chú
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class NoteRepository {
    
    // ==================== SQL QUERIES ====================
    
    private static final String SQL_FIND_BY_ID = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes WHERE NoteID = ?";
    
    private static final String SQL_FIND_BY_USER = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes WHERE UserID = ? AND IsArchived = 0 " +
        "ORDER BY UpdatedAt DESC";
    
    private static final String SQL_FIND_BY_USER_AND_STATUS = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes WHERE UserID = ? AND Status = ? AND IsArchived = 0 " +
        "ORDER BY UpdatedAt DESC";
    
    private static final String SQL_FIND_BY_USER_AND_CATEGORY = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes WHERE UserID = ? AND CategoryID = ? AND IsArchived = 0 " +
        "ORDER BY UpdatedAt DESC";
    
    private static final String SQL_FIND_FAVORITES = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes WHERE UserID = ? AND IsFavorite = 1 AND IsArchived = 0 " +
        "ORDER BY UpdatedAt DESC";
    
    private static final String SQL_SEARCH = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes " +
        "WHERE UserID = ? AND IsArchived = 0 " +
        "AND (Title LIKE ? OR Content LIKE ? OR Summary LIKE ?) " +
        "ORDER BY UpdatedAt DESC";
    
    private static final String SQL_SEARCH_ADVANCED = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes " +
        "WHERE UserID = ? AND IsArchived = 0 " +
        "AND (Title LIKE ? OR Content LIKE ? OR Summary LIKE ?) " +
        "AND (? IS NULL OR Status = ?) " +
        "AND (? IS NULL OR CategoryID = ?) " +
        "AND (? IS NULL OR IsFavorite = ?) " +
        "ORDER BY UpdatedAt DESC";
    
    private static final String SQL_INSERT = 
        "INSERT INTO Notes (UserID, CategoryID, Title, Content, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, GETDATE(), GETDATE())";
    
    private static final String SQL_UPDATE = 
        "UPDATE Notes SET CategoryID = ?, Title = ?, Content = ?, Summary = ?, " +
        "Status = ?, IsFavorite = ?, IsArchived = ?, Color = ?, UpdatedAt = GETDATE() " +
        "WHERE NoteID = ?";
    
    private static final String SQL_UPDATE_CONTENT = 
        "UPDATE Notes SET Title = ?, Content = ?, Summary = ?, UpdatedAt = GETDATE() " +
        "WHERE NoteID = ?";
    
    private static final String SQL_TOGGLE_FAVORITE = 
        "UPDATE Notes SET IsFavorite = CASE WHEN IsFavorite = 1 THEN 0 ELSE 1 END, " +
        "UpdatedAt = GETDATE() WHERE NoteID = ?";
    
    private static final String SQL_UPDATE_STATUS = 
        "UPDATE Notes SET Status = ?, UpdatedAt = GETDATE() WHERE NoteID = ?";
    
    private static final String SQL_INCREMENT_VIEW = 
        "UPDATE Notes SET ViewCount = ViewCount + 1 WHERE NoteID = ?";
    
    private static final String SQL_ARCHIVE = 
        "UPDATE Notes SET IsArchived = 1, UpdatedAt = GETDATE() WHERE NoteID = ?";
    
    private static final String SQL_DELETE = 
        "DELETE FROM Notes WHERE NoteID = ?";
    
    private static final String SQL_COUNT_BY_USER = 
        "SELECT COUNT(*) FROM Notes WHERE UserID = ? AND IsArchived = 0";
    
    private static final String SQL_STATISTICS = 
        "SELECT " +
        "  COUNT(*) AS TotalNotes, " +
        "  SUM(CASE WHEN Status = 'REGULAR' THEN 1 ELSE 0 END) AS RegularCount, " +
        "  SUM(CASE WHEN Status = 'URGENT' THEN 1 ELSE 0 END) AS UrgentCount, " +
        "  SUM(CASE WHEN Status = 'IDEAS' THEN 1 ELSE 0 END) AS IdeasCount, " +
        "  SUM(CASE WHEN Status = 'COMPLETED' THEN 1 ELSE 0 END) AS CompletedCount, " +
        "  SUM(CASE WHEN IsFavorite = 1 THEN 1 ELSE 0 END) AS FavoriteCount " +
        "FROM Notes WHERE UserID = ? AND IsArchived = 0";
    
    // ==================== FIND OPERATIONS ====================
    
    /**
     * Tìm ghi chú theo ID
     * 
     * @param id ID ghi chú
     * @return Optional chứa Note nếu tìm thấy
     */
    public Optional<Note> findById(int id) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding note by ID: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Lấy tất cả ghi chú của user
     * 
     * @param userId ID người dùng
     * @return Danh sách ghi chú
     */
    public List<Note> findByUser(int userId) {
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USER)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding notes by user: " + e.getMessage());
        }
        return notes;
    }
    
    /**
     * Lấy ghi chú theo status
     * 
     * @param userId ID người dùng
     * @param status Trạng thái
     * @return Danh sách ghi chú
     */
    public List<Note> findByUserAndStatus(int userId, String status) {
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USER_AND_STATUS)) {
            
            ps.setInt(1, userId);
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding notes by status: " + e.getMessage());
        }
        return notes;
    }
    
    /**
     * Lấy ghi chú theo category
     * 
     * @param userId ID người dùng
     * @param categoryId ID danh mục
     * @return Danh sách ghi chú
     */
    public List<Note> findByUserAndCategory(int userId, int categoryId) {
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USER_AND_CATEGORY)) {
            
            ps.setInt(1, userId);
            ps.setInt(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding notes by category: " + e.getMessage());
        }
        return notes;
    }
    
    /**
     * Lấy ghi chú yêu thích
     * 
     * @param userId ID người dùng
     * @return Danh sách ghi chú yêu thích
     */
    public List<Note> findFavorites(int userId) {
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_FAVORITES)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding favorite notes: " + e.getMessage());
        }
        return notes;
    }
    
    /**
     * Tìm kiếm ghi chú theo từ khóa
     * 
     * @param userId ID người dùng
     * @param keyword Từ khóa tìm kiếm
     * @return Danh sách ghi chú khớp
     */
    public List<Note> search(int userId, String keyword) {
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SEARCH)) {
            
            String pattern = "%" + keyword + "%";
            ps.setInt(1, userId);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching notes: " + e.getMessage());
        }
        return notes;
    }
    
    /**
     * Tìm kiếm nâng cao với nhiều filter
     * 
     * @param userId ID người dùng
     * @param keyword Từ khóa
     * @param status Status filter (null = all)
     * @param categoryId Category filter (null = all)
     * @param favoriteOnly Chỉ lấy yêu thích
     * @return Danh sách ghi chú
     */
    public List<Note> searchAdvanced(int userId, String keyword, String status, 
                                      Integer categoryId, Boolean favoriteOnly) {
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SEARCH_ADVANCED)) {
            
            String pattern = "%" + (keyword != null ? keyword : "") + "%";
            ps.setInt(1, userId);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            
            // Status filter
            ps.setString(5, status);
            ps.setString(6, status);
            
            // Category filter
            if (categoryId != null) {
                ps.setInt(7, categoryId);
                ps.setInt(8, categoryId);
            } else {
                ps.setNull(7, Types.INTEGER);
                ps.setNull(8, Types.INTEGER);
            }
            
            // Favorite filter
            if (favoriteOnly != null && favoriteOnly) {
                ps.setBoolean(9, true);
                ps.setBoolean(10, true);
            } else {
                ps.setNull(9, Types.BOOLEAN);
                ps.setNull(10, Types.BOOLEAN);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error in advanced search: " + e.getMessage());
        }
        return notes;
    }
    
    // ==================== CRUD OPERATIONS ====================
    
    /**
     * Thêm ghi chú mới
     * 
     * @param note Ghi chú cần thêm
     * @return true nếu thành công
     */
    public boolean insert(Note note) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, note.getUserId());
            
            if (note.getCategoryId() != null && note.getCategoryId() > 0) {
                ps.setInt(2, note.getCategoryId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            
            ps.setString(3, note.getTitle());
            ps.setString(4, note.getContent());
            ps.setString(5, note.getSummary());
            ps.setString(6, note.getStatus() != null ? note.getStatus() : "REGULAR");
            ps.setBoolean(7, note.isFavorite());
            ps.setBoolean(8, note.isArchived());
            ps.setString(9, note.getColor() != null ? note.getColor() : "#ffffff");
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        note.setId(keys.getInt(1));
                    }
                }
                note.setCreatedAt(LocalDateTime.now());
                note.setUpdatedAt(LocalDateTime.now());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error inserting note: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Cập nhật ghi chú
     * 
     * @param note Ghi chú cần cập nhật
     * @return true nếu thành công
     */
    public boolean update(Note note) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            
            if (note.getCategoryId() != null && note.getCategoryId() > 0) {
                ps.setInt(1, note.getCategoryId());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            ps.setString(4, note.getSummary());
            ps.setString(5, note.getStatus());
            ps.setBoolean(6, note.isFavorite());
            ps.setBoolean(7, note.isArchived());
            ps.setString(8, note.getColor());
            ps.setInt(9, note.getId());
            
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                note.setUpdatedAt(LocalDateTime.now());
            }
            return success;
        } catch (SQLException e) {
            System.err.println("Error updating note: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Cập nhật nội dung ghi chú
     * 
     * @param noteId ID ghi chú
     * @param title Tiêu đề mới
     * @param content Nội dung mới
     * @param summary Tóm tắt mới
     * @return true nếu thành công
     */
    public boolean updateContent(int noteId, String title, String content, String summary) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_CONTENT)) {
            
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setString(3, summary);
            ps.setInt(4, noteId);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating note content: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Toggle trạng thái yêu thích
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    public boolean toggleFavorite(int noteId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_TOGGLE_FAVORITE)) {
            
            ps.setInt(1, noteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error toggling favorite: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Cập nhật status
     * 
     * @param noteId ID ghi chú
     * @param status Status mới
     * @return true nếu thành công
     */
    public boolean updateStatus(int noteId, String status) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            
            ps.setString(1, status);
            ps.setInt(2, noteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating status: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Tăng số lần xem
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    public boolean incrementViewCount(int noteId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INCREMENT_VIEW)) {
            
            ps.setInt(1, noteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error incrementing view count: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Archive ghi chú
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    public boolean archive(int noteId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_ARCHIVE)) {
            
            ps.setInt(1, noteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error archiving note: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Xóa ghi chú vĩnh viễn
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    /**
     * Soft delete - move note to trash (set IsArchived = 1)
     * 
     * @param noteId ID của note
     * @return true nếu thành công
     */
    public boolean delete(int noteId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_ARCHIVE)) {
            
            ps.setInt(1, noteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error archiving note: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Permanently delete a note (hard delete)
     * USE WITH CAUTION - this cannot be undone
     * 
     * @param noteId ID của note
     * @return true nếu thành công
     */
    public boolean deletePermanently(int noteId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            
            ps.setInt(1, noteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error permanently deleting note: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Restore archived note (set IsArchived = 0)
     * 
     * @param noteId ID của note
     * @return true nếu thành công
     */
    public boolean restore(int noteId) {
        String SQL_RESTORE = "UPDATE Notes SET IsArchived = 0, UpdatedAt = GETDATE() WHERE NoteID = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_RESTORE)) {
            
            ps.setInt(1, noteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error restoring note: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get all archived notes (trash) for a user
     * 
     * @param userId ID người dùng
     * @return Danh sách archived notes
     */
    public List<Note> getArchivedNotes(int userId) {
        String SQL_GET_ARCHIVED = "SELECT * FROM Notes WHERE UserID = ? AND IsArchived = 1 ORDER BY UpdatedAt DESC";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_ARCHIVED)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting archived notes: " + e.getMessage());
        }
        return notes;
    }
    
    // ==================== STATISTICS ====================
    
    /**
     * Đếm số ghi chú của user
     * 
     * @param userId ID người dùng
     * @return Số ghi chú
     */
    public int countByUser(int userId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_USER)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error counting notes: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Lấy thống kê ghi chú của user
     * 
     * @param userId ID người dùng
     * @return Mảng thống kê [total, regular, urgent, ideas, completed, favorite]
     */
    public int[] getStatistics(int userId) {
        int[] stats = new int[6];
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_STATISTICS)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats[0] = rs.getInt("TotalNotes");
                    stats[1] = rs.getInt("RegularCount");
                    stats[2] = rs.getInt("UrgentCount");
                    stats[3] = rs.getInt("IdeasCount");
                    stats[4] = rs.getInt("CompletedCount");
                    stats[5] = rs.getInt("FavoriteCount");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting statistics: " + e.getMessage());
        }
        return stats;
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Map ResultSet thành đối tượng Note
     * 
     * @param rs ResultSet từ query
     * @return Note object
     * @throws SQLException nếu có lỗi đọc dữ liệu
     */
    private Note mapResultSetToNote(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getInt("NoteID"));
        note.setUserId(rs.getInt("UserID"));
        
        int categoryId = rs.getInt("CategoryID");
        note.setCategoryId(rs.wasNull() ? null : categoryId);
        
        note.setTitle(rs.getString("Title"));
        note.setContent(rs.getString("Content"));
        note.setSummary(rs.getString("Summary"));
        note.setStatus(rs.getString("Status"));
        note.setFavorite(rs.getBoolean("IsFavorite"));
        note.setArchived(rs.getBoolean("IsArchived"));
        note.setColor(rs.getString("Color"));
        note.setViewCount(rs.getInt("ViewCount"));
        
        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            note.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
        if (updatedAt != null) {
            note.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return note;
    }
}
