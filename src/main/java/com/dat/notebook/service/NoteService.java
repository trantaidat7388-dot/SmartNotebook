package com.dat.notebook.service;

import com.dat.notebook.model.Note;
import com.dat.notebook.model.Tag;
import com.dat.notebook.repository.NoteRepository;
import com.dat.notebook.repository.TagRepository;
import com.dat.notebook.util.SmartTextUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class xử lý logic nghiệp vụ cho Ghi chú.
 * 
 * Chức năng:
 * - CRUD operations với validation
 * - Tự động tạo summary (Smart feature)
 * - Gợi ý tiêu đề (Smart feature)
 * - Tìm kiếm nâng cao
 * - Quản lý tags
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class NoteService {
    
    // ==================== DEPENDENCIES ====================
    
    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final AuthService authService;
    
    // ==================== CONSTRUCTOR ====================
    
    public NoteService() {
        this.noteRepository = new NoteRepository();
        this.tagRepository = new TagRepository();
        this.authService = AuthService.getInstance();
    }
    
    public NoteService(NoteRepository noteRepository, TagRepository tagRepository) {
        this.noteRepository = noteRepository;
        this.tagRepository = tagRepository;
        this.authService = AuthService.getInstance();
    }
    
    // ==================== CREATE ====================
    
    /**
     * Tạo ghi chú mới với các tính năng Smart
     * 
     * @param title Tiêu đề (có thể để trống - sẽ tự gợi ý)
     * @param content Nội dung
     * @return Note mới tạo hoặc null nếu thất bại
     */
    public Note createNote(String title, String content) {
        return createNote(title, content, Note.STATUS_REGULAR, null);
    }
    
    /**
     * Tạo ghi chú mới với đầy đủ options
     * 
     * @param title Tiêu đề
     * @param content Nội dung
     * @param status Trạng thái
     * @param categoryId ID danh mục (nullable)
     * @return Note mới tạo hoặc null
     */
    public Note createNote(String title, String content, String status, Integer categoryId) {
        int userId = authService.getCurrentUserId();
        if (userId <= 0) {
            System.err.println("Chưa đăng nhập, không thể tạo ghi chú");
            return null;
        }
        
        Note note = new Note();
        note.setUserId(userId);
        note.setCategoryId(categoryId);
        note.setContent(content);
        note.setStatus(status != null ? status : Note.STATUS_REGULAR);
        
        // ===== SMART FEATURE: Auto-suggest title =====
        if (title == null || title.trim().isEmpty()) {
            title = SmartTextUtil.suggestTitle(content);
        }
        note.setTitle(title);
        
        // ===== SMART FEATURE: Auto-generate summary =====
        String summary = SmartTextUtil.generateSummary(content);
        note.setSummary(summary);
        
        // Timestamps
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        
        if (noteRepository.insert(note)) {
            System.out.println("Tạo ghi chú thành công: " + note.getTitle());
            return note;
        }
        
        System.err.println("Không thể tạo ghi chú");
        return null;
    }
    
    /**
     * Tạo ghi chú nhanh chỉ với nội dung
     * 
     * @param content Nội dung
     * @return Note mới tạo
     */
    public Note quickNote(String content) {
        return createNote(null, content);
    }
    
    /**
     * Tạo ghi chú từ đối tượng Note
     * (Dùng cho trường hợp cần custom nhiều thuộc tính)
     * 
     * @param note Đối tượng Note cần tạo
     * @return Note đã tạo hoặc null nếu thất bại
     */
    public Note createNote(Note note) {
        if (note == null) {
            return null;
        }
        
        // Đảm bảo có userId
        if (note.getUserId() <= 0) {
            int userId = authService.getCurrentUserId();
            if (userId <= 0) {
                System.err.println("Chưa đăng nhập, không thể tạo ghi chú");
                return null;
            }
            note.setUserId(userId);
        }
        
        // Smart feature: Auto-suggest title nếu chưa có
        if (note.getTitle() == null || note.getTitle().trim().isEmpty()) {
            String title = SmartTextUtil.suggestTitle(note.getContent());
            note.setTitle(title);
        }
        
        // Smart feature: Auto-generate summary
        String summary = SmartTextUtil.generateSummary(note.getContent());
        note.setSummary(summary);
        
        // Timestamps
        if (note.getCreatedAt() == null) {
            note.setCreatedAt(LocalDateTime.now());
        }
        if (note.getUpdatedAt() == null) {
            note.setUpdatedAt(LocalDateTime.now());
        }
        
        if (noteRepository.insert(note)) {
            System.out.println("Tạo ghi chú thành công: " + note.getTitle());
            return note;
        }
        
        System.err.println("Không thể tạo ghi chú");
        return null;
    }
    
    // ==================== READ ====================
    
    /**
     * Lấy ghi chú theo ID
     * 
     * @param noteId ID ghi chú
     * @return Optional chứa Note
     */
    public Optional<Note> getNoteById(int noteId) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        
        if (noteOpt.isPresent()) {
            Note note = noteOpt.get();
            // Load tags
            note.setTags(tagRepository.findByNote(noteId));
            // Increment view count
            noteRepository.incrementViewCount(noteId);
        }
        
        return noteOpt;
    }
    
    /**
     * Lấy tất cả ghi chú của người dùng hiện tại
     * 
     * @return Danh sách ghi chú
     */
    public List<Note> getAllNotes() {
        int userId = authService.getCurrentUserId();
        if (userId <= 0) {
            return List.of();
        }
        
        List<Note> notes = noteRepository.findByUser(userId);
        
        // Load tags for each note
        for (Note note : notes) {
            note.setTags(tagRepository.findByNote(note.getId()));
        }
        
        return notes;
    }
    
    /**
     * Lấy tất cả ghi chú của user chỉ định
     * 
     * @param userId ID người dùng
     * @return Danh sách ghi chú
     */
    public List<Note> getNotesByUser(int userId) {
        if (userId <= 0) {
            return List.of();
        }
        
        List<Note> notes = noteRepository.findByUser(userId);
        
        // Load tags for each note
        for (Note note : notes) {
            note.setTags(tagRepository.findByNote(note.getId()));
        }
        
        return notes;
    }
    
    /**
     * Lấy ghi chú theo status
     * 
     * @param status Trạng thái
     * @return Danh sách ghi chú
     */
    public List<Note> getNotesByStatus(String status) {
        int userId = authService.getCurrentUserId();
        if (userId <= 0) {
            return List.of();
        }
        
        return noteRepository.findByUserAndStatus(userId, status);
    }
    
    /**
     * Lấy ghi chú yêu thích
     * 
     * @return Danh sách ghi chú yêu thích
     */
    public List<Note> getFavoriteNotes() {
        int userId = authService.getCurrentUserId();
        if (userId <= 0) {
            return List.of();
        }
        
        return noteRepository.findFavorites(userId);
    }
    
    /**
     * Lấy ghi chú theo danh mục
     * 
     * @param categoryId ID danh mục
     * @return Danh sách ghi chú
     */
    public List<Note> getNotesByCategory(int categoryId) {
        int userId = authService.getCurrentUserId();
        if (userId <= 0) {
            return List.of();
        }
        
        return noteRepository.findByUserAndCategory(userId, categoryId);
    }
    
    // ==================== UPDATE ====================
    
    /**
     * Cập nhật ghi chú
     * 
     * @param note Ghi chú cần cập nhật
     * @return true nếu thành công
     */
    public boolean updateNote(Note note) {
        if (note == null || note.getId() <= 0) {
            return false;
        }
        
        // ===== SMART FEATURE: Re-generate summary =====
        String newSummary = SmartTextUtil.generateSummary(note.getContent());
        note.setSummary(newSummary);
        
        note.setUpdatedAt(LocalDateTime.now());
        
        return noteRepository.update(note);
    }
    
    /**
     * Cập nhật nội dung ghi chú
     * 
     * @param noteId ID ghi chú
     * @param title Tiêu đề mới
     * @param content Nội dung mới
     * @return true nếu thành công
     */
    public boolean updateNoteContent(int noteId, String title, String content) {
        // Auto-generate summary
        String summary = SmartTextUtil.generateSummary(content);
        
        return noteRepository.updateContent(noteId, title, content, summary);
    }
    
    /**
     * Toggle yêu thích
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    public boolean toggleFavorite(int noteId) {
        return noteRepository.toggleFavorite(noteId);
    }
    
    /**
     * Cập nhật status
     * 
     * @param noteId ID ghi chú
     * @param status Status mới
     * @return true nếu thành công
     */
    public boolean updateStatus(int noteId, String status) {
        if (!Note.isValidStatus(status)) {
            return false;
        }
        return noteRepository.updateStatus(noteId, status);
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
     * Archive ghi chú (soft delete)
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    public boolean archiveNote(int noteId) {
        return noteRepository.archive(noteId);
    }
    
    /**
     * Xóa ghi chú vĩnh viễn
     * 
     * @param noteId ID ghi chú
     * @return true nếu thành công
     */
    public boolean deleteNote(int noteId) {
        // Soft delete - move to trash
        return noteRepository.delete(noteId);
    }
    
    /**
     * Restore archived note
     * 
     * @param noteId ID note
     * @return true nếu thành công
     */
    public boolean restoreNote(int noteId) {
        return noteRepository.restore(noteId);
    }
    
    /**
     * Get all archived notes for user (trash)
     * 
     * @param userId ID người dùng
     * @return Danh sách archived notes
     */
    public List<Note> getArchivedNotes(int userId) {
        return noteRepository.getArchivedNotes(userId);
    }
    
    /**
     * Permanently delete note (cannot be undone)
     * 
     * @param noteId ID note
     * @return true nếu thành công
     */
    public boolean deleteNotePermanently(int noteId) {
        // Remove tags first
        tagRepository.removeAllTagsFromNote(noteId);
        // Permanently delete
        return noteRepository.deletePermanently(noteId);
    }
    
    // ==================== SEARCH ====================
    
    /**
     * Tìm kiếm ghi chú theo từ khóa
     * 
     * @param keyword Từ khóa
     * @return Danh sách ghi chú khớp
     */
    public List<Note> searchNotes(String keyword) {
        int userId = authService.getCurrentUserId();
        if (userId <= 0 || keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        
        return noteRepository.search(userId, keyword.trim());
    }
    
    /**
     * Tìm kiếm nâng cao với nhiều filter
     * 
     * @param keyword Từ khóa
     * @param status Status filter
     * @param categoryId Category filter
     * @param favoriteOnly Chỉ lấy yêu thích
     * @return Danh sách ghi chú
     */
    public List<Note> searchAdvanced(String keyword, String status, 
                                      Integer categoryId, Boolean favoriteOnly) {
        int userId = authService.getCurrentUserId();
        if (userId <= 0) {
            return List.of();
        }
        
        return noteRepository.searchAdvanced(userId, keyword, status, categoryId, favoriteOnly);
    }
    
    /**
     * ===== SMART FEATURE: Highlight từ khóa trong text =====
     * 
     * @param text Text gốc
     * @param keyword Từ khóa cần highlight
     * @param highlightStart Tag mở (ví dụ: "<mark>")
     * @param highlightEnd Tag đóng (ví dụ: "</mark>")
     * @return Text với từ khóa đã được highlight
     */
    public String highlightKeyword(String text, String keyword, 
                                   String highlightStart, String highlightEnd) {
        return SmartTextUtil.highlightKeywords(text, keyword, highlightStart, highlightEnd);
    }
    
    // ==================== TAGS ====================
    
    /**
     * Thêm tag vào ghi chú
     * 
     * @param noteId ID ghi chú
     * @param tagName Tên tag
     * @return true nếu thành công
     */
    public boolean addTagToNote(int noteId, String tagName) {
        int userId = authService.getCurrentUserId();
        if (userId <= 0) {
            return false;
        }
        
        Tag tag = tagRepository.findOrCreate(userId, tagName);
        if (tag == null) {
            return false;
        }
        
        return tagRepository.addTagToNote(noteId, tag.getId());
    }
    
    /**
     * Xóa tag khỏi ghi chú
     * 
     * @param noteId ID ghi chú
     * @param tagId ID tag
     * @return true nếu thành công
     */
    public boolean removeTagFromNote(int noteId, int tagId) {
        return tagRepository.removeTagFromNote(noteId, tagId);
    }
    
    /**
     * Lấy tags phổ biến
     * 
     * @param limit Số lượng tối đa
     * @return Danh sách tags
     */
    public List<Tag> getPopularTags(int limit) {
        int userId = authService.getCurrentUserId();
        if (userId <= 0) {
            return List.of();
        }
        
        return tagRepository.findPopular(userId, limit);
    }
    
    /**
     * ===== SMART FEATURE: Gợi ý tags dựa trên nội dung =====
     * 
     * @param content Nội dung ghi chú
     * @return Danh sách tên tags gợi ý
     */
    public List<String> suggestTags(String content) {
        return SmartTextUtil.extractKeywords(content);
    }
    
    // ==================== STATISTICS ====================
    
    /**
     * Lấy thống kê ghi chú
     * 
     * @return Mảng [total, regular, urgent, ideas, completed, favorite]
     */
    public int[] getNoteStatistics() {
        int userId = authService.getCurrentUserId();
        if (userId <= 0) {
            return new int[6];
        }
        
        return noteRepository.getStatistics(userId);
    }
    
    /**
     * Đếm tổng số ghi chú
     * 
     * @return Số ghi chú
     */
    public int countNotes() {
        int userId = authService.getCurrentUserId();
        if (userId <= 0) {
            return 0;
        }
        
        return noteRepository.countByUser(userId);
    }
    
    // ==================== SMART UTILITIES ====================
    
    /**
     * ===== SMART FEATURE: Tạo summary cho nội dung =====
     * 
     * @param content Nội dung
     * @return Summary
     */
    public String generateSummary(String content) {
        return SmartTextUtil.generateSummary(content);
    }
    
    /**
     * ===== SMART FEATURE: Gợi ý tiêu đề =====
     * 
     * @param content Nội dung
     * @return Tiêu đề gợi ý
     */
    public String suggestTitle(String content) {
        return SmartTextUtil.suggestTitle(content);
    }
    
    /**
     * Lấy ghi chú gần đây
     * 
     * @param limit Số lượng
     * @return Danh sách ghi chú
     */
    public List<Note> getRecentNotes(int limit) {
        return getAllNotes().stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
}
