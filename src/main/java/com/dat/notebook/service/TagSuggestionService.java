package com.dat.notebook.service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TagSuggestionService - Gợi ý tags cho ghi chú học tập
 * 
 * ============================================================================
 * GỢI Ý TAGS HỌC TẬP - THUẬT NGỮ & TỪ KHÓA CHÍNH
 * ============================================================================
 * 
 * Yêu cầu tags:
 * - 3–7 tags
 * - Là thuật ngữ học tập hoặc từ khóa chính
 * - Không dùng từ chung chung
 * - Phản ánh nội dung cốt lõi
 * 
 * Thuật toán chi tiết:
 * 1. Tokenize nội dung thành các từ
 * 2. Loại bỏ stopwords (tiếng Việt + tiếng Anh)
 * 3. Loại bỏ từ ngắn (dưới 3 ký tự)
 * 4. Tính điểm TF (Term Frequency) cho mỗi từ
 * 5. Ưu tiên thuật ngữ chuyên ngành
 * 6. Chọn top từ khóa có tần suất cao làm tag
 * 7. Chuẩn hóa tag (lowercase, loại ký tự đặc biệt)
 * 8. Filter: chỉ giữ 3-7 tags, không trùng lặp
 * 
 * ============================================================================
 * MỨC ĐỘ AI: Level 1 - NLP truyền thống + Academic Focus
 * ============================================================================
 * 
 * @author SmartNotebook Team
 */
public class TagSuggestionService {

    private static TagSuggestionService instance;
    private TextAnalysisService textService;

    private static final int MIN_TAGS = 3;
    private static final int MAX_TAGS = 7;
    private static final int MIN_WORD_LENGTH = 3;

    // Category keywords để phân loại
    private static final Map<String, Set<String>> CATEGORY_KEYWORDS = new HashMap<>();

    static {
        // Programming
        CATEGORY_KEYWORDS.put("programming", new HashSet<>(Arrays.asList(
                "code", "java", "python", "javascript", "function", "class", "method",
                "variable", "api", "database", "server", "frontend", "backend", "bug",
                "debug", "compile", "runtime", "algorithm", "data", "structure")));

        // Work/Business
        CATEGORY_KEYWORDS.put("work", new HashSet<>(Arrays.asList(
                "meeting", "project", "deadline", "task", "team", "client", "report",
                "email", "schedule", "presentation", "budget", "goal", "strategy")));

        // Study/Education
        CATEGORY_KEYWORDS.put("study", new HashSet<>(Arrays.asList(
                "học", "bài", "làm", "thi", "điểm", "lớp", "giáo", "viên",
                "lesson", "exam", "homework", "study", "learn", "school", "university")));

        // Personal
        CATEGORY_KEYWORDS.put("personal", new HashSet<>(Arrays.asList(
                "diary", "thought", "feeling", "dream", "goal", "plan", "life",
                "nhật", "ký", "suy", "nghĩ", "cảm", "xúc")));

        // Ideas
        CATEGORY_KEYWORDS.put("idea", new HashSet<>(Arrays.asList(
                "idea", "concept", "brainstorm", "creative", "innovation", "design",
                "ý", "tưởng", "sáng", "tạo")));
    }

    private TagSuggestionService() {
        textService = TextAnalysisService.getInstance();
    }

    public static synchronized TagSuggestionService getInstance() {
        if (instance == null) {
            instance = new TagSuggestionService();
        }
        return instance;
    }

    /**
     * Gợi ý tags cho ghi chú
     * 
     * @param content Nội dung ghi chú
     * @return Danh sách tags được đề xuất (3-7 tags)
     */
    public List<String> suggestTags(String content) {
        return suggestTags(content, MAX_TAGS);
    }

    /**
     * Gợi ý tags với số lượng tùy chỉnh
     * 
     * @param content Nội dung ghi chú
     * @param maxTags Số tags tối đa
     * @return Danh sách tags
     */
    public List<String> suggestTags(String content, int maxTags) {
        maxTags = Math.max(MIN_TAGS, Math.min(MAX_TAGS, maxTags));

        if (content == null || content.trim().isEmpty()) {
            return Collections.singletonList("general");
        }

        Set<String> tags = new LinkedHashSet<>(); // Giữ thứ tự

        // 1. Thêm category tags
        String category = detectCategory(content);
        if (category != null) {
            tags.add(category);
        }

        // 2. Trích xuất keyword tags
        List<String> keywordTags = extractKeywordTags(content, maxTags);
        tags.addAll(keywordTags);

        // 3. Chuẩn hóa và filter
        List<String> normalizedTags = tags.stream()
                .map(this::normalizeTag)
                .filter(tag -> tag.length() >= MIN_WORD_LENGTH)
                .distinct()
                .limit(maxTags)
                .collect(Collectors.toList());

        // Đảm bảo có ít nhất MIN_TAGS
        if (normalizedTags.size() < MIN_TAGS) {
            normalizedTags.add("note");
            if (normalizedTags.size() < MIN_TAGS) {
                normalizedTags.add("general");
            }
        }

        return normalizedTags;
    }

    /**
     * Gợi ý tags kèm điểm số (confidence score)
     * 
     * @param content Nội dung ghi chú
     * @return Map từ tag đến điểm số (0.0 - 1.0)
     */
    public Map<String, Double> suggestTagsWithScores(String content) {
        Map<String, Double> tagScores = new LinkedHashMap<>();

        if (content == null || content.trim().isEmpty()) {
            tagScores.put("general", 1.0);
            return tagScores;
        }

        // Tính TF-IDF
        List<String> words = textService.tokenize(content);
        words = textService.removeStopwords(words);

        Map<String, Integer> frequency = new HashMap<>();
        for (String word : words) {
            if (word.length() >= MIN_WORD_LENGTH) {
                frequency.merge(word.toLowerCase(), 1, Integer::sum);
            }
        }

        if (frequency.isEmpty()) {
            tagScores.put("general", 1.0);
            return tagScores;
        }

        int maxFreq = Collections.max(frequency.values());

        // Normalize scores
        frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(MAX_TAGS)
                .forEach(entry -> {
                    double score = (double) entry.getValue() / maxFreq;
                    tagScores.put(entry.getKey(), Math.round(score * 100) / 100.0);
                });

        return tagScores;
    }

    /**
     * Detect category của nội dung dựa trên keywords
     */
    private String detectCategory(String content) {
        String lowerContent = content.toLowerCase();

        Map<String, Integer> categoryScores = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (lowerContent.contains(keyword)) {
                    score++;
                }
            }
            if (score > 0) {
                categoryScores.put(entry.getKey(), score);
            }
        }

        if (categoryScores.isEmpty()) {
            return null;
        }

        return categoryScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Trích xuất keyword tags từ content
     */
    private List<String> extractKeywordTags(String content, int maxTags) {
        List<String> words = textService.tokenize(content);
        words = textService.removeStopwords(words);

        // Filter: chỉ lấy từ đủ dài và không phải số
        words = words.stream()
                .filter(w -> w.length() >= MIN_WORD_LENGTH)
                .filter(w -> !w.matches("\\d+"))
                .collect(Collectors.toList());

        // Đếm tần suất
        Map<String, Integer> frequency = new HashMap<>();
        for (String word : words) {
            frequency.merge(word.toLowerCase(), 1, Integer::sum);
        }

        // Sắp xếp và lấy top
        return frequency.entrySet().stream()
                .filter(e -> e.getValue() >= 2) // Ít nhất xuất hiện 2 lần
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(maxTags)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Chuẩn hóa tag: lowercase, loại bỏ ký tự đặc biệt
     */
    private String normalizeTag(String tag) {
        if (tag == null)
            return "";

        // Lowercase và loại bỏ ký tự đặc biệt
        String normalized = tag.toLowerCase()
                .replaceAll("[^a-zA-Z0-9àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ]", "")
                .trim();

        return normalized;
    }

    /**
     * Kiểm tra tag có hợp lệ không
     */
    public boolean isValidTag(String tag) {
        if (tag == null || tag.trim().isEmpty())
            return false;
        if (tag.length() < MIN_WORD_LENGTH)
            return false;
        if (tag.length() > 30)
            return false;
        if (textService.isStopword(tag))
            return false;
        return true;
    }
}
