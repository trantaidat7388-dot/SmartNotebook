package com.dat.notebook.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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

    // Store prompts: Key = Type (SUMMARY, TITLE, TAGS, GENERAL), Value = Prompt
    // Content
    private final Map<String, String> offlinePrompts = new HashMap<>(); // Standard Map import required if not present

    // Private constructor (Singleton)
    private AIService() {
        loadOfflinePrompts();
    }

    // Singleton instance
    public static synchronized AIService getInstance() {
        if (instance == null) {
            instance = new AIService();
        }
        return instance;
    }

    /**
     * Load prompts from ai_prompts_offline.txt
     */
    private void loadOfflinePrompts() {
        try (java.io.InputStream is = getClass().getResourceAsStream("/ai_prompts_offline.txt")) {
            if (is == null) {
                System.err.println("AI Prompts file not found!");
                return;
            }

            String content = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String[] sections = content.split("---");

            for (String section : sections) {
                section = section.trim();
                if (section.isEmpty())
                    continue;

                if (section.startsWith("# PROMPT:")) {
                    // Specific prompt
                    String[] lines = section.split("\n", 2);
                    if (lines.length >= 2) {
                        String header = lines[0].trim(); // e.g. "# PROMPT: SUMMARIZE"
                        String body = lines[1].trim();

                        String type = header.substring("# PROMPT:".length()).trim().toUpperCase();
                        offlinePrompts.put(type, body);
                    }
                } else if (section.startsWith("# SYSTEM PROMPT")) {
                    // General system prompt
                    offlinePrompts.put("GENERAL", section);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading AI prompts: " + e.getMessage());
        }
    }

    /**
     * Get prompt by type
     */
    public String getPrompt(String type) {
        return offlinePrompts.getOrDefault(type.toUpperCase(), offlinePrompts.get("GENERAL"));
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
        // Use new AIEngine
        return com.dat.notebook.service.ai.AIEngine.generateSummary(content);
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
        // Use new AIEngine
        return com.dat.notebook.service.ai.AIEngine.generateTitle(content);
    }

    /**
     * Gợi ý nhiều tiêu đề (cho người dùng lựa chọn)
     * 
     * @param content Nội dung ghi chú
     * @param count   Số lượng tiêu đề cần gợi ý (mặc định 3)
     * @return Danh sách tiêu đề gợi ý
     */
    public List<String> suggestMultipleTitles(String content, int count) {
        if (content == null || content.trim().isEmpty()) {
            return Arrays.asList("Ghi chú không có tiêu đề");
        }
        // AIEngine provides one best title only
        return Arrays.asList(com.dat.notebook.service.ai.AIEngine.generateTitle(content));
    }

    // ===== CHỨC NĂNG 3: GỢI Ý TAG =====

    /**
     * Gợi ý tags để phân loại ghi chú
     * 
     * @param content Nội dung ghi chú
     * @return Danh sách tags (3-6 tags)
     */
    public List<String> suggestTags(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // Use new AIEngine
        return com.dat.notebook.service.ai.AIEngine.generateTags(content);
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

        public String getSummary() {
            return summary;
        }

        public String getSuggestedTitle() {
            return suggestedTitle;
        }

        public List<String> getSuggestedTags() {
            return suggestedTags;
        }
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
