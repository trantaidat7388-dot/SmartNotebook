package com.dat.notebook.dao;

import com.dat.notebook.config.DatabaseConfig;
import com.dat.notebook.model.Note;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object cho Note - CHUẨN MVC
 * 
 * QUAN TRỌNG: Tất cả query đều phải có WHERE user_id = ? để đảm bảo:
 * - Mỗi user chỉ thấy ghi chú của chính mình
 * - Bảo mật dữ liệu đa người dùng
 * 
 * @author SmartNotebook Team
 * @version 2.0
 */
public class NoteDAO {
    

    // Tất cả query đều có UserID filter
    
    private static final String SQL_INSERT = 
        "INSERT INTO Notes (UserID, CategoryID, Title, Content, HtmlContent, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, 0, GETDATE(), GETDATE())";
    
    private static final String SQL_UPDATE = 
        "UPDATE Notes SET CategoryID = ?, Title = ?, Content = ?, HtmlContent = ?, Summary = ?, " +
        "Status = ?, IsFavorite = ?, IsArchived = ?, Color = ?, UpdatedAt = GETDATE() " +
        "WHERE NoteID = ? AND UserID = ?";
    
    private static final String SQL_UPDATE_CONTENT = 
        "UPDATE Notes SET Title = ?, Content = ?, HtmlContent = ?, Summary = ?, UpdatedAt = GETDATE() " +
        "WHERE NoteID = ? AND UserID = ?";
    
    private static final String SQL_DELETE = 
        "UPDATE Notes SET IsArchived = 1, UpdatedAt = GETDATE() WHERE NoteID = ? AND UserID = ?";
    
    private static final String SQL_DELETE_PERMANENT = 
        "DELETE FROM Notes WHERE NoteID = ? AND UserID = ?";
    
    private static final String SQL_RESTORE = 
        "UPDATE Notes SET IsArchived = 0, UpdatedAt = GETDATE() WHERE NoteID = ? AND UserID = ?";
    
    private static final String SQL_FIND_BY_ID = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, HtmlContent, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes WHERE NoteID = ? AND UserID = ?";
    
    private static final String SQL_FIND_ALL_BY_USER = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, HtmlContent, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes WHERE UserID = ? AND IsArchived = 0 " +
        "ORDER BY UpdatedAt DESC";
    
    private static final String SQL_FIND_BY_STATUS = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, HtmlContent, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes WHERE UserID = ? AND Status = ? AND IsArchived = 0 " +
        "ORDER BY UpdatedAt DESC";
    
    private static final String SQL_FIND_FAVORITES = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, HtmlContent, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes WHERE UserID = ? AND IsFavorite = 1 AND IsArchived = 0 " +
        "ORDER BY UpdatedAt DESC";
    
    private static final String SQL_FIND_ARCHIVED = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, HtmlContent, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes WHERE UserID = ? AND IsArchived = 1 " +
        "ORDER BY UpdatedAt DESC";
    
    private static final String SQL_SEARCH = 
        "SELECT NoteID, UserID, CategoryID, Title, Content, HtmlContent, Summary, Status, " +
        "IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt " +
        "FROM Notes " +
        "WHERE UserID = ? AND IsArchived = 0 " +
        "AND (Title LIKE ? OR Content LIKE ? OR Summary LIKE ?) " +
        "ORDER BY UpdatedAt DESC";
    
    private static final String SQL_TOGGLE_FAVORITE = 
        "UPDATE Notes SET IsFavorite = CASE WHEN IsFavorite = 1 THEN 0 ELSE 1 END, " +
        "UpdatedAt = GETDATE() WHERE NoteID = ? AND UserID = ?";
    
    private static final String SQL_UPDATE_STATUS = 
        "UPDATE Notes SET Status = ?, UpdatedAt = GETDATE() WHERE NoteID = ? AND UserID = ?";
    
    private static final String SQL_COUNT_BY_USER = 
        "SELECT COUNT(*) FROM Notes WHERE UserID = ? AND IsArchived = 0";
    

    
    /**
     * Tạo ghi chú mới
     * 
     * @param note Note object (phải có userId)
     * @return true nếu thành công, note.id sẽ được set
     */
    public boolean insert(Note note) {
        if (note == null || note.getUserId() <= 0) {
            System.err.println("NoteDAO.insert: Invalid note or missing userId");
            return false;
        }
        
        // Trim title and summary to prevent truncation
        String title = note.getTitle();
        if (title != null && title.length() > 1000) {
            title = title.substring(0, 997) + "...";
        }
        String summary = note.getSummary();
        if (summary != null && summary.length() > 2000) {
            summary = summary.substring(0, 1997) + "...";
        }
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, note.getUserId());
            ps.setObject(2, note.getCategoryId(), Types.INTEGER);
            ps.setString(3, title);
            ps.setString(4, note.getContent());
            ps.setString(5, note.getHtmlContent());
            ps.setString(6, summary);
            ps.setString(7, note.getStatus() != null ? note.getStatus() : Note.STATUS_REGULAR);
            ps.setBoolean(8, note.isFavorite());
            ps.setString(9, note.getColor() != null ? note.getColor() : Note.DEFAULT_COLOR);
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        note.setId(rs.getInt(1));
                        note.setCreatedAt(LocalDateTime.now());
                        note.setUpdatedAt(LocalDateTime.now());
                        System.out.println("NoteDAO: Created note ID=" + note.getId() + " for user=" + note.getUserId());
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("NoteDAO.insert ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    

    
    /**
     * Lấy ghi chú theo ID (BẮT BUỘC kiểm tra userId)
     * 
     * @param noteId ID ghi chú
     * @param userId ID người dùng
     * @return Optional<Note>
     */
    public Optional<Note> findById(int noteId, int userId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            
            ps.setInt(1, noteId);
            ps.setInt(2, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("NoteDAO.findById ERROR: " + e.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Lấy tất cả ghi chú của user (không archived)
     * 
     * @param userId ID người dùng
     * @return List<Note>
     */
    public List<Note> findAllByUser(int userId) {
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL_BY_USER)) {
            
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("NoteDAO.findAllByUser ERROR: " + e.getMessage());
        }
        
        System.out.println("NoteDAO: Found " + notes.size() + " notes for user=" + userId);
        return notes;
    }
    
    /**
     * Lấy ghi chú theo status
     * 
     * @param userId ID người dùng
     * @param status Trạng thái
     * @return List<Note>
     */
    public List<Note> findByStatus(int userId, String status) {
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STATUS)) {
            
            ps.setInt(1, userId);
            ps.setString(2, status);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("NoteDAO.findByStatus ERROR: " + e.getMessage());
        }
        return notes;
    }
    
    /**
     * Lấy ghi chú yêu thích
     * 
     * @param userId ID người dùng
     * @return List<Note>
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
            System.err.println("NoteDAO.findFavorites ERROR: " + e.getMessage());
        }
        return notes;
    }
    
    /**
     * Lấy ghi chú đã archived (thùng rác)
     * 
     * @param userId ID người dùng
     * @return List<Note>
     */
    public List<Note> findArchived(int userId) {
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ARCHIVED)) {
            
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("NoteDAO.findArchived ERROR: " + e.getMessage());
        }
        return notes;
    }
    
    /**
     * Tìm kiếm ghi chú
     * 
     * @param userId ID người dùng
     * @param keyword Từ khóa tìm kiếm
     * @return List<Note>
     */
    public List<Note> search(int userId, String keyword) {
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SEARCH)) {
            
            String searchPattern = "%" + keyword + "%";
            ps.setInt(1, userId);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);
            ps.setString(4, searchPattern);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("NoteDAO.search ERROR: " + e.getMessage());
        }
        return notes;
    }
    

    
    /**
     * Cập nhật ghi chú
     * 
     * @param note Note object
     * @param userId ID người dùng (để verify ownership)
     * @return true nếu thành công
     */
    public boolean update(Note note, int userId) {
        if (note == null || note.getId() <= 0 || userId <= 0) {
            return false;
        }
        
        // Trim title and summary to prevent truncation
        String title = note.getTitle();
        if (title != null && title.length() > 1000) {
            title = title.substring(0, 997) + "...";
        }
        String summary = note.getSummary();
        if (summary != null && summary.length() > 2000) {
            summary = summary.substring(0, 1997) + "...";
        }
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            
            ps.setObject(1, note.getCategoryId(), Types.INTEGER);
            ps.setString(2, title);
            ps.setString(3, note.getContent());
            ps.setString(4, note.getHtmlContent());
            ps.setString(5, summary);
            ps.setString(6, note.getStatus());
            ps.setBoolean(7, note.isFavorite());
            ps.setBoolean(8, note.isArchived());
            ps.setString(9, note.getColor());
            ps.setInt(10, note.getId());
            ps.setInt(11, userId);
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                note.setUpdatedAt(LocalDateTime.now());
                System.out.println("NoteDAO: Updated note ID=" + note.getId());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("NoteDAO.update ERROR: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Cập nhật nội dung ghi chú (title, content, htmlContent)
     * 
     * @param noteId ID ghi chú
     * @param userId ID người dùng
     * @param title Tiêu đề mới
     * @param content Nội dung plain text
     * @param htmlContent Nội dung HTML
     * @param summary Tóm tắt
     * @return true nếu thành công
     */
    public boolean updateContent(int noteId, int userId, String title, String content, 
                                  String htmlContent, String summary) {
        // Trim to prevent truncation
        if (title != null && title.length() > 1000) {
            title = title.substring(0, 997) + "...";
        }
        if (summary != null && summary.length() > 2000) {
            summary = summary.substring(0, 1997) + "...";
        }
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_CONTENT)) {
            
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setString(3, htmlContent);
            ps.setString(4, summary);
            ps.setInt(5, noteId);
            ps.setInt(6, userId);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("NoteDAO.updateContent ERROR: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Toggle trạng thái yêu thích
     * 
     * @param noteId ID ghi chú
     * @param userId ID người dùng
     * @return true nếu thành công
     */
    public boolean toggleFavorite(int noteId, int userId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_TOGGLE_FAVORITE)) {
            
            ps.setInt(1, noteId);
            ps.setInt(2, userId);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("NoteDAO.toggleFavorite ERROR: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Cập nhật status
     * 
     * @param noteId ID ghi chú
     * @param userId ID người dùng
     * @param status Status mới
     * @return true nếu thành công
     */
    public boolean updateStatus(int noteId, int userId, String status) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            
            ps.setString(1, status);
            ps.setInt(2, noteId);
            ps.setInt(3, userId);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("NoteDAO.updateStatus ERROR: " + e.getMessage());
        }
        return false;
    }
    

    
    /**
     * Soft delete - chuyển vào thùng rác
     * 
     * @param noteId ID ghi chú
     * @param userId ID người dùng
     * @return true nếu thành công
     */
    public boolean delete(int noteId, int userId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            
            ps.setInt(1, noteId);
            ps.setInt(2, userId);
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                System.out.println("NoteDAO: Soft deleted note ID=" + noteId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("NoteDAO.delete ERROR: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Khôi phục ghi chú từ thùng rác
     * 
     * @param noteId ID ghi chú
     * @param userId ID người dùng
     * @return true nếu thành công
     */
    public boolean restore(int noteId, int userId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_RESTORE)) {
            
            ps.setInt(1, noteId);
            ps.setInt(2, userId);
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                System.out.println("NoteDAO: Restored note ID=" + noteId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("NoteDAO.restore ERROR: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Xóa vĩnh viễn (không thể khôi phục)
     * 
     * @param noteId ID ghi chú
     * @param userId ID người dùng
     * @return true nếu thành công
     */
    public boolean deletePermanently(int noteId, int userId) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE_PERMANENT)) {
            
            ps.setInt(1, noteId);
            ps.setInt(2, userId);
            
            int affected = ps.executeUpdate();
            if (affected > 0) {
                System.out.println("NoteDAO: Permanently deleted note ID=" + noteId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("NoteDAO.deletePermanently ERROR: " + e.getMessage());
        }
        return false;
    }
    

    
    /**
     * Đếm số ghi chú của user
     * 
     * @param userId ID người dùng
     * @return Số lượng ghi chú
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
            System.err.println("NoteDAO.countByUser ERROR: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Map ResultSet to Note object
     */
    private Note mapResultSetToNote(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getInt("NoteID"));
        note.setUserId(rs.getInt("UserID"));
        
        Integer categoryId = rs.getInt("CategoryID");
        if (rs.wasNull()) {
            note.setCategoryId(null);
        } else {
            note.setCategoryId(categoryId);
        }
        
        note.setTitle(rs.getString("Title"));
        note.setContent(rs.getString("Content"));
        
        // HTML Content - có thể null
        String htmlContent = rs.getString("HtmlContent");
        if (htmlContent != null && !htmlContent.trim().isEmpty()) {
            note.setHtmlContent(htmlContent);
        }
        
        note.setSummary(rs.getString("Summary"));
        note.setStatus(rs.getString("Status"));
        note.setFavorite(rs.getBoolean("IsFavorite"));
        note.setArchived(rs.getBoolean("IsArchived"));
        note.setColor(rs.getString("Color"));
        note.setViewCount(rs.getInt("ViewCount"));
        
        // Timestamps
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
