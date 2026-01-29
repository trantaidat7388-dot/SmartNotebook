package com.dat.notebook.util;

import com.dat.notebook.model.NoteVersion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho NoteVersion
 * Quản lý version history của ghi chú
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class NoteVersionDAO {
    

    
    /**
     * Tạo version mới cho ghi chú
     * Sử dụng stored procedure sp_CreateNoteVersion
     * 
     * @param version NoteVersion cần tạo
     * @return true nếu thành công
     */
    public boolean createVersion(NoteVersion version) {
        String sql = "{CALL sp_CreateNoteVersion(?, ?, ?, ?, ?)}";
        
        try (Connection conn = DBConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            
            cs.setInt(1, version.getNoteId());
            cs.setString(2, version.getTitle());
            cs.setString(3, version.getHtmlContent());
            
            if (version.getCreatedBy() != null) {
                cs.setInt(4, version.getCreatedBy());
            } else {
                cs.setNull(4, Types.INTEGER);
            }
            
            if (version.getChangeDescription() != null) {
                cs.setString(5, version.getChangeDescription());
            } else {
                cs.setNull(5, Types.NVARCHAR);
            }
            
            // Execute và lấy kết quả
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    version.setVersionId(rs.getInt("NewVersionID"));
                    version.setVersionNumber(rs.getInt("VersionNumber"));
                    version.setCreatedAt(LocalDateTime.now());
                    return true;
                }
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error creating note version: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Insert version thủ công (không dùng stored procedure)
     * Dùng khi cần control chi tiết hơn
     */
    public boolean insertVersion(NoteVersion version) {
        String sql = "INSERT INTO NoteVersions " +
                     "(NoteID, Title, HtmlContent, PlainTextContent, VersionNumber, " +
                     "CreatedAt, CreatedBy, ChangeDescription) " +
                     "VALUES (?, ?, ?, ?, ?, GETDATE(), ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, version.getNoteId());
            ps.setString(2, version.getTitle());
            ps.setString(3, version.getHtmlContent());
            
            // Generate plain text nếu chưa có
            String plainText = version.getPlainTextContent();
            if (plainText == null || plainText.isEmpty()) {
                version.generatePlainText();
                plainText = version.getPlainTextContent();
            }
            ps.setString(4, plainText);
            
            // Get next version number
            int nextVersion = getNextVersionNumber(version.getNoteId());
            ps.setInt(5, nextVersion);
            
            if (version.getCreatedBy() != null) {
                ps.setInt(6, version.getCreatedBy());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            
            if (version.getChangeDescription() != null) {
                ps.setString(7, version.getChangeDescription());
            } else {
                ps.setNull(7, Types.NVARCHAR);
            }
            
            int affected = ps.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        version.setVersionId(keys.getInt(1));
                        version.setVersionNumber(nextVersion);
                        version.setCreatedAt(LocalDateTime.now());
                        return true;
                    }
                }
            }
            
            return false;
            
        } catch (SQLException e) {
            System.err.println("Error inserting version: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    

    
    /**
     * Lấy tất cả versions của một ghi chú
     * 
     * @param noteId ID của ghi chú
     * @return Danh sách versions, sắp xếp mới nhất trước
     */
    public List<NoteVersion> getVersionsByNoteId(int noteId) {
        return getVersionsByNoteId(noteId, 50); // Default 50 versions
    }
    
    /**
     * Lấy versions với giới hạn số lượng
     * 
     * @param noteId ID của ghi chú
     * @param maxVersions Số lượng tối đa
     * @return Danh sách versions
     */
    public List<NoteVersion> getVersionsByNoteId(int noteId, int maxVersions) {
        String sql = "{CALL sp_GetNoteVersionHistory(?, ?)}";
        List<NoteVersion> versions = new ArrayList<>();
        
        try (Connection conn = DBConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            
            cs.setInt(1, noteId);
            cs.setInt(2, maxVersions);
            
            try (ResultSet rs = cs.executeQuery()) {
                while (rs.next()) {
                    NoteVersion version = mapRowSummary(rs);
                    versions.add(version);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting version history: " + e.getMessage());
            e.printStackTrace();
        }
        
        return versions;
    }
    
    /**
     * Lấy một version cụ thể theo ID
     * Bao gồm cả HTML content đầy đủ
     * 
     * @param versionId ID của version
     * @return NoteVersion hoặc null
     */
    public NoteVersion getVersionById(int versionId) {
        String sql = "SELECT v.VersionID, v.NoteID, v.Title, v.HtmlContent, " +
                     "v.PlainTextContent, v.VersionNumber, v.CreatedAt, " +
                     "v.CreatedBy, v.ChangeDescription, u.Username AS CreatedByUsername " +
                     "FROM NoteVersions v " +
                     "LEFT JOIN Users u ON v.CreatedBy = u.UserID " +
                     "WHERE v.VersionID = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, versionId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowFull(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting version by ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Lấy version mới nhất của một ghi chú
     * 
     * @param noteId ID của ghi chú
     * @return NoteVersion mới nhất hoặc null
     */
    public NoteVersion getLatestVersion(int noteId) {
        String sql = "SELECT TOP 1 v.VersionID, v.NoteID, v.Title, v.HtmlContent, " +
                     "v.PlainTextContent, v.VersionNumber, v.CreatedAt, " +
                     "v.CreatedBy, v.ChangeDescription, u.Username AS CreatedByUsername " +
                     "FROM NoteVersions v " +
                     "LEFT JOIN Users u ON v.CreatedBy = u.UserID " +
                     "WHERE v.NoteID = ? " +
                     "ORDER BY v.VersionNumber DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, noteId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowFull(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting latest version: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Đếm số lượng versions của một ghi chú
     * 
     * @param noteId ID của ghi chú
     * @return Số lượng versions
     */
    public int getVersionCount(int noteId) {
        String sql = "SELECT COUNT(*) FROM NoteVersions WHERE NoteID = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, noteId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error counting versions: " + e.getMessage());
        }
        
        return 0;
    }
    

    
    /**
     * Xóa một version cụ thể
     * CẢNH BÁO: Không nên xóa version history trừ khi thực sự cần
     * 
     * @param versionId ID của version
     * @return true nếu xóa thành công
     */
    public boolean deleteVersion(int versionId) {
        String sql = "DELETE FROM NoteVersions WHERE VersionID = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, versionId);
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting version: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Xóa tất cả versions của một ghi chú
     * Được gọi tự động khi xóa note (CASCADE)
     * 
     * @param noteId ID của ghi chú
     * @return Số lượng versions đã xóa
     */
    public int deleteVersionsByNoteId(int noteId) {
        String sql = "DELETE FROM NoteVersions WHERE NoteID = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, noteId);
            return ps.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error deleting versions: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Giữ lại N versions mới nhất, xóa các versions cũ
     * Cleanup để tránh lưu trữ quá nhiều versions
     * 
     * @param noteId ID của ghi chú
     * @param keepCount Số lượng versions cần giữ lại
     * @return Số lượng versions đã xóa
     */
    public int keepLatestVersions(int noteId, int keepCount) {
        String sql = "DELETE FROM NoteVersions " +
                     "WHERE NoteID = ? AND VersionID NOT IN (" +
                     "  SELECT TOP (?) VersionID " +
                     "  FROM NoteVersions " +
                     "  WHERE NoteID = ? " +
                     "  ORDER BY VersionNumber DESC" +
                     ")";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, noteId);
            ps.setInt(2, keepCount);
            ps.setInt(3, noteId);
            
            return ps.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error cleanup versions: " + e.getMessage());
            return 0;
        }
    }
    

    
    /**
     * Rollback note về một version cụ thể
     * Sử dụng stored procedure sp_RollbackToVersion
     * 
     * @param noteId ID của ghi chú
     * @param versionId ID của version muốn rollback
     * @param userId ID người thực hiện rollback
     * @return true nếu thành công
     */
    public boolean rollbackToVersion(int noteId, int versionId, int userId) {
        String sql = "{CALL sp_RollbackToVersion(?, ?, ?)}";
        
        try (Connection conn = DBConnection.getConnection();
             CallableStatement cs = conn.prepareCall(sql)) {
            
            cs.setInt(1, noteId);
            cs.setInt(2, versionId);
            cs.setInt(3, userId);
            
            cs.execute();
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error rolling back to version: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    

    
    /**
     * Lấy version number tiếp theo cho note
     */
    private int getNextVersionNumber(int noteId) {
        String sql = "SELECT ISNULL(MAX(VersionNumber), 0) + 1 FROM NoteVersions WHERE NoteID = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, noteId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting next version number: " + e.getMessage());
        }
        
        return 1; // Default to version 1
    }
    
    /**
     * Map ResultSet sang NoteVersion (Summary - không bao gồm content đầy đủ)
     */
    private NoteVersion mapRowSummary(ResultSet rs) throws SQLException {
        NoteVersion version = new NoteVersion();
        version.setVersionId(rs.getInt("VersionID"));
        version.setNoteId(rs.getInt("NoteID"));
        version.setTitle(rs.getString("Title"));
        version.setVersionNumber(rs.getInt("VersionNumber"));
        
        Timestamp createdAtTs = rs.getTimestamp("CreatedAt");
        if (createdAtTs != null) {
            version.setCreatedAt(createdAtTs.toLocalDateTime());
        }
        
        int createdBy = rs.getInt("CreatedBy");
        if (!rs.wasNull()) {
            version.setCreatedBy(createdBy);
        }
        
        version.setChangeDescription(rs.getString("ChangeDescription"));
        version.setCreatedByUsername(rs.getString("CreatedByUsername"));
        
        return version;
    }
    
    /**
     * Map ResultSet sang NoteVersion (Full - bao gồm content đầy đủ)
     */
    private NoteVersion mapRowFull(ResultSet rs) throws SQLException {
        NoteVersion version = mapRowSummary(rs);
        version.setHtmlContent(rs.getString("HtmlContent"));
        version.setPlainTextContent(rs.getString("PlainTextContent"));
        return version;
    }
}
