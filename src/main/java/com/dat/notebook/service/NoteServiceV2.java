package com.dat.notebook.service;

import com.dat.notebook.dao.NoteDAO;
import com.dat.notebook.model.Note;
import com.dat.notebook.util.SmartTextUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service class xử lý logic nghiệp vụ cho Ghi chú - CHUẨN MVC
 * 
 * Chức năng:
 * - CRUD operations với validation
 * - Tự động tạo summary (Smart feature) - sử dụng SummaryService
 * - Gợi ý tiêu đề (Smart feature) - sử dụng TitleSuggestionService
 * - Tìm kiếm nâng cao
 * 
 * QUAN TRỌNG: Service KHÔNG chứa SQL - chỉ gọi DAO
 * 
 * @author SmartNotebook Team
 * @version 2.0
 */
public class NoteServiceV2 {

    // ==================== DEPENDENCIES ====================

    private final NoteDAO noteDAO;
    private final AuthService authService;
    private final SummaryService summaryService;
    private final TitleSuggestionService titleSuggestionService;

    // ==================== CONSTRUCTOR ====================

    public NoteServiceV2() {
        this.noteDAO = new NoteDAO();
        this.authService = AuthService.getInstance();
        this.summaryService = SummaryService.getInstance();
        this.titleSuggestionService = TitleSuggestionService.getInstance();
    }

    public NoteServiceV2(NoteDAO noteDAO) {
        this.noteDAO = noteDAO;
        this.authService = AuthService.getInstance();
        this.summaryService = SummaryService.getInstance();
        this.titleSuggestionService = TitleSuggestionService.getInstance();
    }

    // ==================== CREATE ====================

    /**
     * Tạo ghi chú mới với các tính năng Smart
     * 
     * @param title       Tiêu đề (có thể để trống - sẽ tự gợi ý)
     * @param content     Nội dung plain text
     * @param htmlContent Nội dung HTML (Rich Text)
     * @return Note mới tạo hoặc null nếu thất bại
     */
    public Note createNote(String title, String content, String htmlContent) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            System.err.println("NoteService: Chưa đăng nhập, không thể tạo ghi chú");
            return null;
        }

        Note note = new Note();
        note.setUserId(userId);
        note.setContent(content);
        note.setHtmlContent(htmlContent);
        note.setStatus(Note.STATUS_REGULAR);

        // ===== SMART FEATURE: Auto-suggest title =====
        if (title == null || title.trim().isEmpty()) {
            // Extract text từ HTML nếu có
            String textContent = htmlContent != null ? SmartTextUtil.stripHtml(htmlContent) : content;
            title = titleSuggestionService.suggestTitle(textContent);
        }
        note.setTitle(title);

        // ===== SMART FEATURE: Auto-generate summary =====
        String textContent = htmlContent != null ? SmartTextUtil.stripHtml(htmlContent) : content;
        note.setSummary(summaryService.summarize(textContent));

        // Timestamps
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());

        if (noteDAO.insert(note)) {
            System.out.println("NoteService: Tạo ghi chú thành công - " + note.getTitle());
            return note;
        }

        System.err.println("NoteService: Không thể tạo ghi chú");
        return null;
    }

    /**
     * Tạo ghi chú từ đối tượng Note đầy đủ
     * 
     * @param note Note object
     * @return Note đã tạo với ID hoặc null
     */
    public Note createNote(Note note) {
        if (note == null) {
            return null;
        }

        int userId = getCurrentUserId();
        if (userId <= 0) {
            System.err.println("NoteService: Chưa đăng nhập");
            return null;
        }

        note.setUserId(userId);

        // Smart feature: Auto-suggest title
        if (note.getTitle() == null || note.getTitle().trim().isEmpty()) {
            String content = note.getHtmlContent() != null ? SmartTextUtil.stripHtml(note.getHtmlContent())
                    : note.getContent();
            note.setTitle(titleSuggestionService.suggestTitle(content));
        }

        // Smart feature: Auto-generate summary
        String content = note.getHtmlContent() != null ? SmartTextUtil.stripHtml(note.getHtmlContent())
                : note.getContent();
        note.setSummary(summaryService.summarize(content));

        // Timestamps
        if (note.getCreatedAt() == null) {
            note.setCreatedAt(LocalDateTime.now());
        }
        note.setUpdatedAt(LocalDateTime.now());

        if (noteDAO.insert(note)) {
            return note;
        }

        return null;
    }

    /**
     * Tạo ghi chú nhanh chỉ với nội dung
     * 
     * @param content Nội dung
     * @return Note mới tạo
     */
    public Note quickNote(String content) {
        return createNote(null, content, null);
    }

    // ==================== READ ====================

    /**
     * Lấy ghi chú theo ID (kiểm tra ownership)
     * 
     * @param noteId ID ghi chú
     * @return Optional<Note>
     */
    public Optional<Note> getNoteById(int noteId) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return Optional.empty();
        }

        return noteDAO.findById(noteId, userId);
    }

    /**
     * Lấy tất cả ghi chú của user hiện tại
     * 
     * @return List<Note>
     */
    public List<Note> getAllNotes() {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return List.of();
        }

        return noteDAO.findAllByUser(userId);
    }

    /**
     * Lấy tất cả ghi chú của user chỉ định
     * 
     * @param userId ID người dùng
     * @return List<Note>
     */
    public List<Note> getNotesByUser(int userId) {
        if (userId <= 0) {
            return List.of();
        }

        return noteDAO.findAllByUser(userId);
    }

    /**
     * Lấy ghi chú theo status
     * 
     * @param status Trạng thái
     * @return List<Note>
     */
    public List<Note> getNotesByStatus(String status) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return List.of();
        }

        return noteDAO.findByStatus(userId, status);
    }

    /**
     * Lấy ghi chú yêu thích
     * 
     * @return List<Note>
     */
    public List<Note> getFavoriteNotes() {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return List.of();
        }

        return noteDAO.findFavorites(userId);
    }

    /**
     * Lấy ghi chú đã xóa (trong thùng rác)
     * 
     * @return List<Note>
     */
    public List<Note> getArchivedNotes() {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return List.of();
        }

        return noteDAO.findArchived(userId);
    }

    /**
     * Lấy ghi chú đã xóa của user chỉ định
     * 
     * @param userId ID người dùng
     * @return List<Note>
     */
    public List<Note> getArchivedNotes(int userId) {
        if (userId <= 0) {
            return List.of();
        }

        return noteDAO.findArchived(userId);
    }

    /**
     * Tìm kiếm ghi chú
     * 
     * @param keyword Từ khóa
     * @return List<Note>
     */
    public List<Note> searchNotes(String keyword) {
        int userId = getCurrentUserId();
        if (userId <= 0 || keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        return noteDAO.search(userId, keyword.trim());
    }

    // ==================== UPDATE ====================

    /**
     * Cập nhật ghi chú
     * 
     * @param note Note cần cập nhật
     * @return true nếu thành công
     */
    public boolean updateNote(Note note) {
        if (note == null || note.getId() <= 0) {
            return false;
        }

        int userId = getCurrentUserId();
        if (userId <= 0) {
            return false;
        }

        // Re-generate summary từ content mới
        String content = note.getHtmlContent() != null ? SmartTextUtil.stripHtml(note.getHtmlContent())
                : note.getContent();
        note.setSummary(summaryService.summarize(content));

        note.setUpdatedAt(LocalDateTime.now());

        return noteDAO.update(note, userId);
    }

    /**
     * Cập nhật nội dung ghi chú (auto-save)
     * 
     * @param noteId      ID ghi chú
     * @param title       Tiêu đề
     * @param content     Nội dung plain text
     * @param htmlContent Nội dung HTML
     * @return true nếu thành công
     */
    public boolean updateNoteContent(int noteId, String title, String content, String htmlContent) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return false;
        }

        // Generate summary từ content mới
        String textContent = htmlContent != null ? SmartTextUtil.stripHtml(htmlContent) : content;
        String summary = summaryService.summarize(textContent);

        return noteDAO.updateContent(noteId, userId, title, content, htmlContent, summary);
    }

    /**
     * Toggle trạng thái yêu thích
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    public boolean toggleFavorite(int noteId) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return false;
        }

        return noteDAO.toggleFavorite(noteId, userId);
    }

    /**
     * Cập nhật status
     * 
     * @param noteId ID ghi chú
     * @param status Status mới
     * @return true nếu thành công
     */
    public boolean updateStatus(int noteId, String status) {
        int userId = getCurrentUserId();
        if (userId <= 0 || !Note.isValidStatus(status)) {
            return false;
        }

        return noteDAO.updateStatus(noteId, userId, status);
    }

    /**
     * Đánh dấu hoàn thành
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    public boolean markAsCompleted(int noteId) {
        return updateStatus(noteId, Note.STATUS_COMPLETED);
    }

    // ==================== DELETE ====================

    /**
     * Xóa ghi chú (soft delete - vào thùng rác)
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    public boolean deleteNote(int noteId) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return false;
        }

        return noteDAO.delete(noteId, userId);
    }

    /**
     * Khôi phục ghi chú từ thùng rác
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    public boolean restoreNote(int noteId) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return false;
        }

        return noteDAO.restore(noteId, userId);
    }

    /**
     * Xóa vĩnh viễn (không thể khôi phục)
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    public boolean deleteNotePermanently(int noteId) {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return false;
        }

        return noteDAO.deletePermanently(noteId, userId);
    }

    // ==================== UTILITY ====================

    /**
     * Đếm số ghi chú của user hiện tại
     * 
     * @return Số lượng
     */
    public int countNotes() {
        int userId = getCurrentUserId();
        if (userId <= 0) {
            return 0;
        }

        return noteDAO.countByUser(userId);
    }

    /**
     * Lấy ID user hiện tại từ AuthService
     */
    private int getCurrentUserId() {
        return authService.getCurrentUserId();
    }
}
