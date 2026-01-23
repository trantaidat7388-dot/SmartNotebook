package com.dat.notebook.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class cho các tính năng "Smart" xử lý văn bản.
 * 
 * Chức năng:
 * - Tự động tạo summary (tóm tắt)
 * - Gợi ý tiêu đề
 * - Trích xuất từ khóa
 * - Highlight keywords
 * - Phân tích văn bản
 * - Strip HTML tags
 * 
 * @author SmartNotebook Team
 * @version 2.0
 */
public class SmartTextUtil {
    
    // ==================== HTML UTILITY ====================
    
    /**
     * Strip HTML tags từ content
     * 
     * @param html HTML content
     * @return Plain text
     */
    public static String stripHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        
        return html
            .replaceAll("<script[^>]*>[\\s\\S]*?</script>", "") // Remove scripts
            .replaceAll("<style[^>]*>[\\s\\S]*?</style>", "")   // Remove styles
            .replaceAll("<br\\s*/?>", "\n")                      // Convert <br> to newline
            .replaceAll("</p>", "\n")                            // Convert </p> to newline
            .replaceAll("</div>", "\n")                          // Convert </div> to newline
            .replaceAll("</li>", "\n")                           // Convert </li> to newline
            .replaceAll("<[^>]+>", "")                           // Remove remaining tags
            .replaceAll("&nbsp;", " ")                           // Convert &nbsp;
            .replaceAll("&amp;", "&")                            // Convert &amp;
            .replaceAll("&lt;", "<")                             // Convert &lt;
            .replaceAll("&gt;", ">")                             // Convert &gt;
            .replaceAll("&quot;", "\"")                          // Convert &quot;
            .replaceAll("&#39;", "'")                            // Convert &#39;
            .replaceAll("\\s+", " ")                             // Collapse whitespace
            .trim();
    }
    
    /**
     * Convert plain text sang HTML đơn giản
     * 
     * @param text Plain text
     * @return HTML content
     */
    public static String textToHtml(String text) {
        if (text == null || text.isEmpty()) {
            return "<p></p>";
        }
        
        // Escape HTML characters
        String escaped = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
        
        // Convert line breaks to paragraphs
        String[] lines = escaped.split("\\r?\\n");
        StringBuilder html = new StringBuilder();
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                html.append("<p>&nbsp;</p>");
            } else {
                html.append("<p>").append(line).append("</p>");
            }
        }
        
        return html.toString();
    }
    
    // ==================== CONSTANTS ====================
    
    /** Số câu tối đa trong summary */
    private static final int MAX_SUMMARY_SENTENCES = 3;
    
    /** Độ dài tối đa của summary */
    private static final int MAX_SUMMARY_LENGTH = 200;
    
    /** Độ dài tối đa của suggested title */
    private static final int MAX_TITLE_LENGTH = 50;
    
    /** Số từ khóa tối đa khi trích xuất */
    private static final int MAX_KEYWORDS = 5;
    
    /** Danh sách stop words tiếng Việt */
    private static final Set<String> VIETNAMESE_STOP_WORDS = Set.of(
        "và", "hoặc", "nhưng", "mà", "là", "có", "được", "để", "cho", "của",
        "với", "trong", "ngoài", "trên", "dưới", "từ", "đến", "vào", "ra",
        "này", "đó", "kia", "ấy", "nào", "gì", "ai", "sao", "thế", "như",
        "thì", "cũng", "vẫn", "còn", "đã", "sẽ", "đang", "rất", "quá", "lắm",
        "một", "hai", "ba", "các", "những", "mọi", "tất", "cả", "không"
    );
    
    /** Danh sách stop words tiếng Anh */
    private static final Set<String> ENGLISH_STOP_WORDS = Set.of(
        "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "must", "can", "to", "of", "in", "for",
        "on", "with", "at", "by", "from", "as", "into", "through", "during",
        "before", "after", "above", "below", "between", "under", "again",
        "further", "then", "once", "here", "there", "when", "where", "why",
        "how", "all", "each", "few", "more", "most", "other", "some", "such",
        "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very",
        "and", "but", "if", "or", "because", "until", "while", "this", "that",
        "these", "those", "am", "it", "its", "he", "she", "they", "them", "his",
        "her", "their", "what", "which", "who", "whom", "i", "you", "we"
    );
    
    // ==================== SUMMARY GENERATION ====================
    
    /**
     * ===== SMART FEATURE: Tạo summary tự động cho văn bản =====
     * 
     * Thuật toán đơn giản:
     * 1. Tách văn bản thành câu
     * 2. Tính điểm quan trọng cho mỗi câu dựa trên:
     *    - Vị trí (câu đầu quan trọng hơn)
     *    - Chứa từ khóa quan trọng
     *    - Độ dài phù hợp
     * 3. Chọn các câu có điểm cao nhất
     * 
     * @param content Nội dung cần tóm tắt
     * @return Summary đã tóm tắt
     */
    public static String generateSummary(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        
        // Clean content
        String cleanedContent = cleanText(content);
        
        // Nếu nội dung ngắn, trả về nguyên
        if (cleanedContent.length() <= MAX_SUMMARY_LENGTH) {
            return cleanedContent;
        }
        
        // Tách câu
        List<String> sentences = splitIntoSentences(cleanedContent);
        
        if (sentences.isEmpty()) {
            return truncateText(cleanedContent, MAX_SUMMARY_LENGTH);
        }
        
        if (sentences.size() <= MAX_SUMMARY_SENTENCES) {
            return String.join(" ", sentences);
        }
        
        // Tính điểm cho mỗi câu
        Map<String, Double> scores = scoreSentences(sentences, cleanedContent);
        
        // Lấy các câu quan trọng nhất (giữ thứ tự xuất hiện)
        List<String> importantSentences = sentences.stream()
            .sorted((s1, s2) -> Double.compare(scores.getOrDefault(s2, 0.0), scores.getOrDefault(s1, 0.0)))
            .limit(MAX_SUMMARY_SENTENCES)
            .collect(Collectors.toList());
        
        // Sắp xếp lại theo thứ tự xuất hiện trong văn bản gốc
        importantSentences.sort(Comparator.comparingInt(sentences::indexOf));
        
        String summary = String.join(" ", importantSentences);
        
        // Truncate nếu vẫn quá dài
        return truncateText(summary, MAX_SUMMARY_LENGTH);
    }
    
    /**
     * Tính điểm quan trọng cho các câu
     */
    private static Map<String, Double> scoreSentences(List<String> sentences, String fullText) {
        Map<String, Double> scores = new HashMap<>();
        
        // Đếm tần suất từ
        Map<String, Integer> wordFreq = calculateWordFrequency(fullText);
        
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            double score = 0.0;
            
            // 1. Position score: câu đầu và cuối quan trọng hơn
            if (i == 0) {
                score += 2.0;
            } else if (i == sentences.size() - 1) {
                score += 1.0;
            } else if (i < 3) {
                score += 0.5;
            }
            
            // 2. Length score: câu không quá ngắn, không quá dài
            int wordCount = sentence.split("\\s+").length;
            if (wordCount >= 5 && wordCount <= 25) {
                score += 1.0;
            } else if (wordCount < 5) {
                score -= 0.5;
            }
            
            // 3. Keyword score: chứa nhiều từ quan trọng
            String[] words = sentence.toLowerCase().split("\\s+");
            for (String word : words) {
                word = word.replaceAll("[^\\p{L}\\p{N}]", "");
                if (wordFreq.containsKey(word) && !isStopWord(word)) {
                    score += wordFreq.get(word) * 0.1;
                }
            }
            
            // 4. Bonus cho câu có dấu hiệu quan trọng
            String lowerSentence = sentence.toLowerCase();
            if (lowerSentence.contains("quan trọng") || lowerSentence.contains("important") ||
                lowerSentence.contains("cần") || lowerSentence.contains("phải") ||
                lowerSentence.contains("chú ý") || lowerSentence.contains("note")) {
                score += 1.5;
            }
            
            scores.put(sentence, score);
        }
        
        return scores;
    }
    
    // ==================== TITLE SUGGESTION ====================
    
    /**
     * ===== SMART FEATURE: Gợi ý tiêu đề dựa trên nội dung =====
     * 
     * Chiến lược:
     * 1. Lấy câu đầu tiên
     * 2. Hoặc lấy heading đầu tiên (nếu có)
     * 3. Hoặc lấy cụm từ quan trọng nhất
     * 
     * @param content Nội dung
     * @return Tiêu đề gợi ý
     */
    public static String suggestTitle(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Ghi chú mới";
        }
        
        String cleanedContent = cleanText(content);
        
        // 1. Kiểm tra có heading không (dòng bắt đầu bằng # hoặc ##)
        Pattern headingPattern = Pattern.compile("^#{1,3}\\s*(.+)$", Pattern.MULTILINE);
        Matcher headingMatcher = headingPattern.matcher(content);
        if (headingMatcher.find()) {
            String heading = headingMatcher.group(1).trim();
            return truncateText(heading, MAX_TITLE_LENGTH);
        }
        
        // 2. Lấy dòng đầu tiên
        String[] lines = cleanedContent.split("\\n");
        if (lines.length > 0) {
            String firstLine = lines[0].trim();
            
            // Loại bỏ ký tự đặc biệt ở đầu (emoji, bullet points)
            firstLine = firstLine.replaceFirst("^[•\\-\\*📋📝💡🎯✅☐☑️]+\\s*", "");
            
            if (!firstLine.isEmpty() && firstLine.length() <= MAX_TITLE_LENGTH) {
                return firstLine;
            }
            
            // Nếu quá dài, cắt tại dấu câu hoặc word boundary
            if (firstLine.length() > MAX_TITLE_LENGTH) {
                return truncateText(firstLine, MAX_TITLE_LENGTH);
            }
        }
        
        // 3. Trích xuất cụm từ quan trọng
        List<String> keywords = extractKeywords(content);
        if (!keywords.isEmpty()) {
            String title = String.join(" ", keywords.subList(0, Math.min(3, keywords.size())));
            return capitalizeFirstLetter(title);
        }
        
        return "Ghi chú mới";
    }
    
    // ==================== KEYWORD EXTRACTION ====================
    
    /**
     * ===== SMART FEATURE: Trích xuất từ khóa quan trọng =====
     * 
     * @param content Nội dung
     * @return Danh sách từ khóa
     */
    public static List<String> extractKeywords(String content) {
        if (content == null || content.trim().isEmpty()) {
            return List.of();
        }
        
        String cleanedContent = cleanText(content).toLowerCase();
        
        // Đếm tần suất từ
        Map<String, Integer> wordFreq = calculateWordFrequency(cleanedContent);
        
        // Loại bỏ stop words và sắp xếp theo tần suất
        return wordFreq.entrySet().stream()
            .filter(e -> !isStopWord(e.getKey()))
            .filter(e -> e.getKey().length() >= 3)  // Từ có ít nhất 3 ký tự
            .filter(e -> e.getValue() >= 2)         // Xuất hiện ít nhất 2 lần
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(MAX_KEYWORDS)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Tính tần suất xuất hiện của các từ
     */
    private static Map<String, Integer> calculateWordFrequency(String text) {
        Map<String, Integer> freq = new HashMap<>();
        String[] words = text.toLowerCase().split("\\s+");
        
        for (String word : words) {
            word = word.replaceAll("[^\\p{L}\\p{N}]", "");
            if (!word.isEmpty()) {
                freq.merge(word, 1, Integer::sum);
            }
        }
        
        return freq;
    }
    
    // ==================== HIGHLIGHT ====================
    
    /**
     * ===== SMART FEATURE: Highlight từ khóa trong text =====
     * 
     * @param text Text gốc
     * @param keyword Từ khóa cần highlight
     * @param startTag Tag mở
     * @param endTag Tag đóng
     * @return Text với keyword đã highlight
     */
    public static String highlightKeywords(String text, String keyword, 
                                           String startTag, String endTag) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return text;
        }
        
        // Case-insensitive replace
        Pattern pattern = Pattern.compile("(" + Pattern.quote(keyword) + ")", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(text).replaceAll(startTag + "$1" + endTag);
    }
    
    /**
     * Highlight cho HTML
     */
    public static String highlightForHtml(String text, String keyword) {
        return highlightKeywords(text, keyword, "<mark>", "</mark>");
    }
    
    /**
     * Highlight cho JavaFX (dùng style)
     */
    public static String highlightForJavaFX(String text, String keyword) {
        return highlightKeywords(text, keyword, "«", "»");
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Tách văn bản thành các câu
     */
    public static List<String> splitIntoSentences(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        
        // Split by sentence-ending punctuation
        String[] sentences = text.split("(?<=[.!?])\\s+");
        
        return Arrays.stream(sentences)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
    
    /**
     * Làm sạch text (loại bỏ markdown, emoji thừa, etc.)
     */
    public static String cleanText(String text) {
        if (text == null) return "";
        
        return text
            // Loại bỏ markdown heading
            .replaceAll("^#{1,6}\\s*", "")
            // Loại bỏ markdown bold/italic
            .replaceAll("[*_]{1,3}", "")
            // Loại bỏ markdown links
            .replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1")
            // Chuẩn hóa whitespace
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    /**
     * Cắt text theo độ dài với word boundary
     */
    public static String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        
        // Tìm vị trí cắt tại word boundary
        int endIndex = text.lastIndexOf(' ', maxLength - 3);
        if (endIndex <= 0) {
            endIndex = maxLength - 3;
        }
        
        return text.substring(0, endIndex) + "...";
    }
    
    /**
     * Kiểm tra có phải stop word không
     */
    public static boolean isStopWord(String word) {
        if (word == null) return true;
        String lower = word.toLowerCase();
        return VIETNAMESE_STOP_WORDS.contains(lower) || ENGLISH_STOP_WORDS.contains(lower);
    }
    
    /**
     * Viết hoa chữ cái đầu
     */
    public static String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
    
    /**
     * Đếm số từ
     */
    public static int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
    
    /**
     * Đếm số ký tự (không tính khoảng trắng)
     */
    public static int countCharacters(String text) {
        if (text == null) return 0;
        return text.replaceAll("\\s+", "").length();
    }
    
    /**
     * Ước tính thời gian đọc (từ/phút)
     */
    public static int estimateReadingTime(String text) {
        int words = countWords(text);
        int wordsPerMinute = 200; // Tốc độ đọc trung bình
        return Math.max(1, (int) Math.ceil((double) words / wordsPerMinute));
    }
}
