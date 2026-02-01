package com.dat.notebook.service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TitleSuggestionService - Gợi ý tiêu đề cho ghi chú học tập
 * 
 * ============================================================================
 * GỢI Ý TIÊU ĐỀ HỌC TẬP - PHẢN ÁNH TRỌNG TÂM KIẾN THỨC
 * ============================================================================
 * 
 * Yêu cầu tiêu đề:
 * - Ngắn gọn (3-12 từ)
 * - Phản ánh đúng trọng tâm kiến thức
 * - Sử dụng thuật ngữ học tập
 * - Không chung chung
 * 
 * Thuật toán chi tiết:
 * 1. Phân tích toàn bộ nội dung ghi chú
 * 2. Trích xuất top từ khóa quan trọng (TF - Term Frequency)
 * 3. Chiến lược chọn tiêu đề (theo thứ tự ưu tiên):
 *    a) Câu đầu tiên (nếu đủ ngắn và chứa keyword)
 *    b) Câu chứa nhiều keyword quan trọng nhất
 *    c) Ghép các keyword thành tiêu đề
 * 4. Giới hạn độ dài: 3-12 từ
 * 5. Chuẩn hóa: viết hoa chữ cái đầu các từ quan trọng
 * 
 * ============================================================================
 * MỨC ĐỘ AI: Level 1 - NLP truyền thống + Academic Focus
 * ============================================================================
 * 
 * @author SmartNotebook Team
 */
public class TitleSuggestionService {

    private static TitleSuggestionService instance;
    private TextAnalysisService textService;

    private static final int MAX_TITLE_WORDS = 12;
    private static final int MIN_TITLE_WORDS = 3;
    private static final int TOP_KEYWORDS = 5;

    private TitleSuggestionService() {
        textService = TextAnalysisService.getInstance();
    }

    public static synchronized TitleSuggestionService getInstance() {
        if (instance == null) {
            instance = new TitleSuggestionService();
        }
        return instance;
    }

    /**
     * Gợi ý tiêu đề cho ghi chú
     * 
     * @param content Nội dung ghi chú
     * @return Tiêu đề được đề xuất
     */
    public String suggestTitle(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Ghi chú mới";
        }

        content = content.trim();

        // Phương án 1: Lấy câu đầu nếu đủ ngắn
        String firstSentence = getFirstSentence(content);
        if (isGoodTitle(firstSentence)) {
            return capitalizeFirstLetter(truncateToWords(firstSentence, MAX_TITLE_WORDS));
        }

        // Phương án 2: Tìm câu chứa nhiều keyword nhất
        String keywordRichSentence = findKeywordRichSentence(content);
        if (isGoodTitle(keywordRichSentence)) {
            return capitalizeFirstLetter(truncateToWords(keywordRichSentence, MAX_TITLE_WORDS));
        }

        // Phương án 3: Ghép các keyword thành tiêu đề
        List<String> keywords = extractTopKeywords(content, TOP_KEYWORDS);
        if (!keywords.isEmpty()) {
            String keywordTitle = String.join(" ", keywords);
            return capitalizeFirstLetter(keywordTitle);
        }

        // Fallback: Lấy phần đầu của content
        return capitalizeFirstLetter(truncateToWords(content, MAX_TITLE_WORDS));
    }

    /**
     * Gợi ý nhiều tiêu đề để người dùng chọn
     * 
     * @param content Nội dung ghi chú
     * @param count   Số lượng gợi ý (1-5)
     * @return Danh sách các tiêu đề được đề xuất
     */
    public List<String> suggestMultipleTitles(String content, int count) {
        count = Math.max(1, Math.min(5, count));
        List<String> suggestions = new ArrayList<>();

        if (content == null || content.trim().isEmpty()) {
            suggestions.add("Ghi chú mới");
            return suggestions;
        }

        // Gợi ý 1: Câu đầu tiên
        String firstSentence = getFirstSentence(content);
        if (isGoodTitle(firstSentence)) {
            suggestions.add(capitalizeFirstLetter(truncateToWords(firstSentence, MAX_TITLE_WORDS)));
        }

        // Gợi ý 2: Câu chứa nhiều keyword
        String keywordRich = findKeywordRichSentence(content);
        if (isGoodTitle(keywordRich) && !suggestions.contains(capitalizeFirstLetter(keywordRich))) {
            suggestions.add(capitalizeFirstLetter(truncateToWords(keywordRich, MAX_TITLE_WORDS)));
        }

        // Gợi ý 3: Keywords ghép lại
        List<String> keywords = extractTopKeywords(content, TOP_KEYWORDS);
        if (!keywords.isEmpty()) {
            String keywordTitle = capitalizeFirstLetter(String.join(" ", keywords));
            if (!suggestions.contains(keywordTitle)) {
                suggestions.add(keywordTitle);
            }
        }

        // Gợi ý 4+: Các câu quan trọng khác
        List<String> sentences = textService.splitSentences(content);
        for (String sentence : sentences) {
            if (suggestions.size() >= count)
                break;
            String title = capitalizeFirstLetter(truncateToWords(sentence, MAX_TITLE_WORDS));
            if (isGoodTitle(sentence) && !suggestions.contains(title)) {
                suggestions.add(title);
            }
        }

        // Đảm bảo có ít nhất 1 suggestion
        if (suggestions.isEmpty()) {
            suggestions.add(capitalizeFirstLetter(truncateToWords(content, MAX_TITLE_WORDS)));
        }

        return suggestions.stream().limit(count).collect(Collectors.toList());
    }

    /**
     * Lấy câu đầu tiên từ content
     */
    private String getFirstSentence(String content) {
        List<String> sentences = textService.splitSentences(content);
        if (!sentences.isEmpty()) {
            return sentences.get(0);
        }
        return content.split("\n")[0].trim();
    }

    /**
     * Tìm câu chứa nhiều keyword quan trọng nhất
     */
    private String findKeywordRichSentence(String content) {
        List<String> sentences = textService.splitSentences(content);
        if (sentences.isEmpty())
            return content;

        List<String> keywords = extractTopKeywords(content, 10);
        Set<String> keywordSet = new HashSet<>(keywords);

        String bestSentence = sentences.get(0);
        int maxKeywordCount = 0;

        for (String sentence : sentences) {
            List<String> words = textService.tokenize(sentence);
            int keywordCount = 0;
            for (String word : words) {
                if (keywordSet.contains(word.toLowerCase())) {
                    keywordCount++;
                }
            }

            // Ưu tiên câu ngắn hơn nếu cùng số keyword
            if (keywordCount > maxKeywordCount ||
                    (keywordCount == maxKeywordCount && sentence.length() < bestSentence.length())) {
                maxKeywordCount = keywordCount;
                bestSentence = sentence;
            }
        }

        return bestSentence;
    }

    /**
     * Trích xuất top keywords từ content sử dụng TF đơn giản
     */
    private List<String> extractTopKeywords(String content, int topN) {
        List<String> words = textService.tokenize(content);
        words = textService.removeStopwords(words);

        // Đếm tần suất
        Map<String, Integer> frequency = new HashMap<>();
        for (String word : words) {
            frequency.merge(word.toLowerCase(), 1, Integer::sum);
        }

        // Sắp xếp theo tần suất
        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra xem một chuỗi có phù hợp làm tiêu đề không
     */
    private boolean isGoodTitle(String text) {
        if (text == null || text.trim().isEmpty())
            return false;

        int wordCount = textService.tokenize(text).size();
        return wordCount >= MIN_TITLE_WORDS && wordCount <= MAX_TITLE_WORDS * 2;
    }

    /**
     * Cắt ngắn chuỗi theo số từ
     */
    private String truncateToWords(String text, int maxWords) {
        if (text == null)
            return "";

        String[] words = text.split("\\s+");
        if (words.length <= maxWords) {
            return text.trim();
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0)
                result.append(" ");
            result.append(words[i]);
        }

        String truncated = result.toString().trim();
        
        // Add ellipsis only if text was actually cut
        if (words.length > maxWords && !truncated.endsWith(".") && 
            !truncated.endsWith("!") && !truncated.endsWith("?")) {
            truncated += "...";
        }
        
        return truncated;
    }

    /**
     * Viết hoa chữ cái đầu tiên của mỗi từ quan trọng
     */
    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty())
            return text;

        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0)
                result.append(" ");

            String word = words[i];
            if (word.length() > 0) {
                // Viết hoa từ đầu tiên và các từ quan trọng
                if (i == 0 || word.length() > 3) {
                    result.append(Character.toUpperCase(word.charAt(0)));
                    if (word.length() > 1) {
                        result.append(word.substring(1).toLowerCase());
                    }
                } else {
                    result.append(word.toLowerCase());
                }
            }
        }

        return result.toString();
    }
}
