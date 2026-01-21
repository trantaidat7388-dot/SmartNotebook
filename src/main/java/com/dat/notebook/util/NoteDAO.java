package com.dat.notebook.util;

import com.dat.notebook.model.Note;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho Note
 * Phiên bản tạm: lưu trong memory (chưa kết nối database)
 */
public class NoteDAO {

    // Lưu tạm trong memory
    private static List<Note> notes = new ArrayList<>();
    private static int nextId = 1;

    static {
        // Dữ liệu mẫu
        Note note1 = new Note();
        note1.setId(nextId++);
        note1.setTitle("Ghi chú đầu tiên");
        note1.setContent("Đây là nội dung ghi chú mẫu.");
        note1.setCreatedAt(LocalDateTime.now());
        notes.add(note1);

        Note note2 = new Note();
        note2.setId(nextId++);
        note2.setTitle("Danh sách việc cần làm");
        note2.setContent("1. Học Java\n2. Làm project\n3. Review code");
        note2.setCreatedAt(LocalDateTime.now());
        notes.add(note2);
    }

    /**
     * Lấy tất cả ghi chú
     */
    public List<Note> getAllNotes() {
        try (Connection conn = DBConnection.tryGetConnection()) {
            if (conn == null) {
                return new ArrayList<>(notes);
            }

            String sql = "SELECT NoteID, Title, Content, CategoryID, CreatedAt, UpdatedAt " +
                    "FROM Notes ORDER BY UpdatedAt DESC, NoteID DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<Note> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            return new ArrayList<>(notes);
        }
    }

    /**
     * Lấy ghi chú theo danh mục (categoryId). categoryId=0 sẽ lấy ghi chú không có danh mục.
     */
    public List<Note> getNotesByCategory(int categoryId) {
        try (Connection conn = DBConnection.tryGetConnection()) {
            if (conn == null) {
                List<Note> result = new ArrayList<>();
                for (Note note : notes) {
                    if (note.getCategoryId() == categoryId) {
                        result.add(note);
                    }
                }
                return result;
            }

            String sql;
            if (categoryId == 0) {
                sql = "SELECT NoteID, Title, Content, CategoryID, CreatedAt, UpdatedAt " +
                        "FROM Notes WHERE CategoryID IS NULL ORDER BY UpdatedAt DESC, NoteID DESC";
            } else {
                sql = "SELECT NoteID, Title, Content, CategoryID, CreatedAt, UpdatedAt " +
                        "FROM Notes WHERE CategoryID = ? ORDER BY UpdatedAt DESC, NoteID DESC";
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (categoryId != 0) {
                    ps.setInt(1, categoryId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<Note> result = new ArrayList<>();
                    while (rs.next()) {
                        result.add(mapRow(rs));
                    }
                    return result;
                }
            }
        } catch (SQLException e) {
            List<Note> result = new ArrayList<>();
            for (Note note : notes) {
                if (note.getCategoryId() == categoryId) {
                    result.add(note);
                }
            }
            return result;
        }
    }

    /**
     * Lấy ghi chú theo ID
     */
    public Note getNoteById(int id) {
        try (Connection conn = DBConnection.tryGetConnection()) {
            if (conn == null) {
                for (Note note : notes) {
                    if (note.getId() == id) {
                        return note;
                    }
                }
                return null;
            }

            String sql = "SELECT NoteID, Title, Content, CategoryID, CreatedAt, UpdatedAt FROM Notes WHERE NoteID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            for (Note note : notes) {
                if (note.getId() == id) {
                    return note;
                }
            }
            return null;
        }
    }

    /**
     * Thêm mới ghi chú
     */
    public boolean insertNote(Note note) {
        try (Connection conn = DBConnection.tryGetConnection()) {
            if (conn == null) {
                note.setId(nextId++);
                note.setCreatedAt(LocalDateTime.now());
                note.setUpdatedAt(LocalDateTime.now());
                return notes.add(note);
            }

            String sql = "INSERT INTO Notes (Title, Content, CategoryID, CreatedAt, UpdatedAt) VALUES (?, ?, ?, GETDATE(), GETDATE())";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, note.getTitle());
                ps.setString(2, note.getContent());
                if (note.getCategoryId() <= 0) {
                    ps.setNull(3, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(3, note.getCategoryId());
                }

                int affected = ps.executeUpdate();
                if (affected <= 0) {
                    return false;
                }
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
            note.setId(nextId++);
            note.setCreatedAt(LocalDateTime.now());
            note.setUpdatedAt(LocalDateTime.now());
            return notes.add(note);
        }
    }

    /**
     * Cập nhật ghi chú
     */
    public boolean updateNote(Note updatedNote) {
        try (Connection conn = DBConnection.tryGetConnection()) {
            if (conn == null) {
                for (int i = 0; i < notes.size(); i++) {
                    if (notes.get(i).getId() == updatedNote.getId()) {
                        updatedNote.setUpdatedAt(LocalDateTime.now());
                        notes.set(i, updatedNote);
                        return true;
                    }
                }
                return false;
            }

            String sql = "UPDATE Notes SET Title = ?, Content = ?, CategoryID = ?, UpdatedAt = GETDATE() WHERE NoteID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, updatedNote.getTitle());
                ps.setString(2, updatedNote.getContent());
                if (updatedNote.getCategoryId() <= 0) {
                    ps.setNull(3, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(3, updatedNote.getCategoryId());
                }
                ps.setInt(4, updatedNote.getId());
                int affected = ps.executeUpdate();
                updatedNote.setUpdatedAt(LocalDateTime.now());
                return affected > 0;
            }
        } catch (SQLException e) {
            for (int i = 0; i < notes.size(); i++) {
                if (notes.get(i).getId() == updatedNote.getId()) {
                    updatedNote.setUpdatedAt(LocalDateTime.now());
                    notes.set(i, updatedNote);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Xóa ghi chú
     */
    public boolean deleteNote(int id) {
        try (Connection conn = DBConnection.tryGetConnection()) {
            if (conn == null) {
                return notes.removeIf(note -> note.getId() == id);
            }
            String sql = "DELETE FROM Notes WHERE NoteID = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            return notes.removeIf(note -> note.getId() == id);
        }
    }

    /**
     * Tìm kiếm ghi chú theo từ khóa
     */
    public List<Note> searchNotes(String keyword) {
        try (Connection conn = DBConnection.tryGetConnection()) {
            if (conn == null) {
                List<Note> results = new ArrayList<>();
                String lowerKeyword = keyword.toLowerCase();
                for (Note note : notes) {
                    if (note.getTitle().toLowerCase().contains(lowerKeyword) ||
                            (note.getContent() != null && note.getContent().toLowerCase().contains(lowerKeyword))) {
                        results.add(note);
                    }
                }
                return results;
            }

            String sql = "SELECT NoteID, Title, Content, CategoryID, CreatedAt, UpdatedAt " +
                    "FROM Notes " +
                    "WHERE Title LIKE ? OR Content LIKE ? " +
                    "ORDER BY UpdatedAt DESC, NoteID DESC";
            String pattern = "%" + keyword + "%";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, pattern);
                ps.setString(2, pattern);
                try (ResultSet rs = ps.executeQuery()) {
                    List<Note> results = new ArrayList<>();
                    while (rs.next()) {
                        results.add(mapRow(rs));
                    }
                    return results;
                }
            }
        } catch (SQLException e) {
            List<Note> results = new ArrayList<>();
            String lowerKeyword = keyword.toLowerCase();
            for (Note note : notes) {
                if (note.getTitle().toLowerCase().contains(lowerKeyword) ||
                        (note.getContent() != null && note.getContent().toLowerCase().contains(lowerKeyword))) {
                    results.add(note);
                }
            }
            return results;
        }
    }

    private static Note mapRow(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getInt("NoteID"));
        note.setTitle(rs.getString("Title"));
        note.setContent(rs.getString("Content"));

        int catId = rs.getInt("CategoryID");
        if (rs.wasNull()) {
            catId = 0;
        }
        note.setCategoryId(catId);

        Timestamp created = rs.getTimestamp("CreatedAt");
        if (created != null) {
            note.setCreatedAt(created.toLocalDateTime());
        }
        Timestamp updated = rs.getTimestamp("UpdatedAt");
        if (updated != null) {
            note.setUpdatedAt(updated.toLocalDateTime());
        }
        return note;
    }
}
