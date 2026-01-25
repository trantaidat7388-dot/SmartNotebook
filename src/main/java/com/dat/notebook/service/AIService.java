package com.dat.notebook.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AIService - Service tổng hợp các chức năng AI/NLP
 * 
 * NGUYÊN TẮC THIẾT KẾ:
 * - Service này HOÀN TOÀN KHÔNG BIẾT UI tồn tại
 * - Chỉ nhận text input, trả về text output
 * - Offline, không cần API bên ngoài
 * - Sử dụng các NLP service có sẵn
 * 
 * @author SmartNotebook Team
 */
public class AIService {
    
    private static AIService instance;
    
    private final SummaryService summaryService;
    private final TitleSuggestionService titleService;
    private final TagSuggestionService tagService;
    
    // Private constructor (Singleton)
    private AIService() {
        this.summaryService = SummaryService.getInstance();
        this.titleService = TitleSuggestionService.getInstance();
        this.tagService = TagSuggestionService.getInstance();
    }
    
    // Singleton instance
    public static synchronized AIService getInstance() {
        if (instance == null) {
            instance = new AIService();
        }
        return instance;
    }
    
    // ===== CHỨC NĂNG 1: TÓM TẮT GHI CHÚ =====
    
    /**
     * Tóm tắt nội dung ghi chú
     * 
     * @param content Nội dung ghi chú
     * @return Bản tóm tắt (3-5 điểm chính)
     */
    public String summarizeNote(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Không có nội dung để tóm tắt.";
        }
        return summaryService.summarize(content);
    }
    
    // ===== CHỨC NĂNG 2: GỢI Ý TIÊU ĐỀ =====
    
    /**
     * Gợi ý 1 tiêu đề tốt nhất cho ghi chú
     * 
     * @param content Nội dung ghi chú
     * @return Tiêu đề gợi ý
     */
    public String suggestTitle(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Ghi chú không có tiêu đề";
        }
        return titleService.suggestTitle(content);
    }
    
    /**
     * Gợi ý nhiều tiêu đề (cho người dùng lựa chọn)
     * 
     * @param content Nội dung ghi chú
     * @param count Số lượng tiêu đề cần gợi ý (mặc định 3)
     * @return Danh sách tiêu đề gợi ý
     */
    public List<String> suggestMultipleTitles(String content, int count) {
        if (content == null || content.trim().isEmpty()) {
            return Arrays.asList("Ghi chú không có tiêu đề");
        }
        return titleService.suggestMultipleTitles(content, count);
    }
    
    // ===== CHỨC NĂNG 3: GỢI Ý TAG =====
    
    /**
     * Gợi ý tags để phân loại ghi chú
     * 
     * @param content Nội dung ghi chú
     * @return Danh sách tags (3-7 tags)
     */
    public List<String> suggestTags(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return tagService.suggestTags(content);
    }
    
    // ===== HELPER METHOD: Format tags để hiển thị =====
    
    /**
     * Format danh sách tags thành chuỗi hiển thị
     * 
     * @param tags Danh sách tags
     * @return Chuỗi "#tag1 #tag2 #tag3"
     */
    public String formatTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return tags.stream()
                .map(tag -> "#" + tag)
                .collect(Collectors.joining(" "));
    }
    
    // ===== KẾT QUẢ TỔNG HỢP =====
    
    /**
     * Class chứa kết quả AI tổng hợp
     * Dùng để trả về tất cả kết quả cùng lúc
     */
    public static class AIResult {
        private final String summary;
        private final String suggestedTitle;
        private final List<String> suggestedTags;
        
        public AIResult(String summary, String suggestedTitle, List<String> suggestedTags) {
            this.summary = summary;
            this.suggestedTitle = suggestedTitle;
            this.suggestedTags = suggestedTags;
        }
        
        public String getSummary() { return summary; }
        public String getSuggestedTitle() { return suggestedTitle; }
        public List<String> getSuggestedTags() { return suggestedTags; }
    }
    
    /**
     * Phân tích toàn diện ghi chú
     * Trả về tất cả kết quả: tóm tắt, tiêu đề, tags
     * 
     * @param content Nội dung ghi chú
     * @return AIResult chứa đầy đủ kết quả
     */
    public AIResult analyzeNote(String content) {
        String summary = summarizeNote(content);
        String title = suggestTitle(content);
        List<String> tags = suggestTags(content);
        
        return new AIResult(summary, title, tags);
    }
}
