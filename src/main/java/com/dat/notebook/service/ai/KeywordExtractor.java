package com.dat.notebook.service.ai;

import com.dat.notebook.service.TextAnalysisService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * KeywordExtractor - Module trích xuất từ khóa sử dụng thuật toán TF-IDF
 * 
 * ============================================================================
 * THUẬT TOÁN TF-IDF (Term Frequency - Inverse Document Frequency)
 * ============================================================================
 * 
 * TF-IDF là phương pháp thống kê đánh giá mức độ quan trọng của một từ
 * trong một văn bản, dựa trên tần suất xuất hiện của từ đó.
 * 
 * Công thức:
 * - TF (Term Frequency) = Số lần từ xuất hiện / Tổng số từ trong văn bản
 * - IDF (Inverse Document Frequency) = log(Tổng số câu / Số câu chứa từ)
 * - TF-IDF = TF × IDF
 * 
 * Ý nghĩa:
 * - TF cao: Từ xuất hiện nhiều trong văn bản → quan trọng với văn bản này
 * - IDF cao: Từ xuất hiện ở ít câu → mang tính đặc trưng, không phải từ chung
 * - TF-IDF cao: Từ vừa xuất hiện nhiều, vừa mang tính đặc trưng → từ khóa tốt
 * 
 * ============================================================================
 * MỨC ĐỘ AI: Level 1 - NLP truyền thống
 * ============================================================================
 * 
 * Ứng dụng này sử dụng AI ở Level 1:
 * - Xử lý ngôn ngữ tự nhiên bằng phương pháp thống kê (TF-IDF)
 * - KHÔNG sử dụng Machine Learning hoặc Deep Learning
 * - KHÔNG gọi API AI bên ngoài
 * - Chạy hoàn toàn offline trên máy tính người dùng
 * 
 * Hạn chế:
 * - Không hiểu ngữ nghĩa sâu của văn bản
 * - Không nhận diện được từ đồng nghĩa
 * - Hiệu quả phụ thuộc vào chất lượng stopwords list
 * 
 * Hướng mở rộng tương lai:
 * - Tích hợp Word Embeddings (Word2Vec, fastText)
 * - Sử dụng pre-trained language models
 * - Kết nối với OpenAI/Gemini API
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class KeywordExtractor {

    private static KeywordExtractor instance;
    private TextAnalysisService textService;

    // Cấu hình mặc định
    private static final int DEFAULT_TOP_KEYWORDS = 10;
    private static final int MIN_WORD_LENGTH = 4; // Giảm xuống 4 để cho phép từ có nghĩa
    private static final int MIN_WORD_FREQUENCY = 1; // Giảm xuống 1

    /**
     * Constructor private - Singleton pattern
     * Đảm bảo chỉ có một instance của KeywordExtractor trong ứng dụng
     */
    private KeywordExtractor() {
        textService = TextAnalysisService.getInstance();
    }

    /**
     * Lấy instance duy nhất của KeywordExtractor
     * 
     * @return KeywordExtractor instance
     */
    public static synchronized KeywordExtractor getInstance() {
        if (instance == null) {
            instance = new KeywordExtractor();
        }
        return instance;
    }

    /**
     * Trích xuất từ khóa quan trọng nhất từ văn bản
     * 
     * Flow xử lý:
     * 1. Tách văn bản thành các câu
     * 2. Tokenize và loại bỏ stopwords
     * 3. Tính TF-IDF cho mỗi từ
     * 4. Sắp xếp và chọn top N từ khóa
     * 
     * @param content Nội dung văn bản cần phân tích
     * @param topN    Số lượng từ khóa muốn trích xuất
     * @return Danh sách từ khóa quan trọng nhất
     */
    public List<String> extractKeywords(String content, int topN) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }

        topN = Math.max(1, Math.min(topN, 20)); // Giới hạn 1-20 keywords

        // Tính TF-IDF scores
        Map<String, Double> tfidfScores = calculateTFIDF(content);

        // Sắp xếp theo điểm và lấy top N
        return tfidfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Trích xuất từ khóa với số lượng mặc định (10)
     * Ưu tiên cụm từ 2 chữ có nghĩa
     * 
     * @param content Nội dung văn bản
     * @return Danh sách từ khóa quan trọng nhất
     */
    public List<String> extractKeywords(String content) {
        return extractMeaningfulKeywords(content, DEFAULT_TOP_KEYWORDS);
    }

    /**
     * Trích xuất từ khóa ưu tiên cụm từ 2 chữ có nghĩa
     */
    public List<String> extractMeaningfulKeywords(String content, int topN) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Tách thành các câu
        List<String> sentences = textService.splitSentences(content);
        if (sentences.isEmpty()) {
            sentences = Collections.singletonList(content);
        }

        // Thu thập cụm từ 2 chữ có nghĩa
        Set<String> meaningfulPhrases = new HashSet<>();
        
        for (String sentence : sentences) {
            List<String> words = textService.tokenize(sentence);
            words = textService.removeStopwords(words);
            
            // Tạo cụm từ 2 chữ
            for (int i = 0; i < words.size() - 1; i++) {
                String word1 = words.get(i).toLowerCase().trim();
                String word2 = words.get(i + 1).toLowerCase().trim();
                
                // Lọc từ có nghĩa
                if (word1.length() >= 3 && word2.length() >= 3 
                    && !isCommonStopword(word1) && !isCommonStopword(word2)
                    && !word1.matches("\\d+") && !word2.matches("\\d+")) {
                    
                    String phrase = word1 + " " + word2;
                    meaningfulPhrases.add(phrase);
                }
            }
            
            // Thêm từ đơn có nghĩa nếu không đủ cụm từ
            for (String word : words) {
                String clean = word.toLowerCase().trim();
                if (clean.length() >= 5 && !isCommonStopword(clean) && !clean.matches("\\d+")) {
                    meaningfulPhrases.add(clean);
                }
            }
        }

        // Sắp xếp theo độ dài (ưu tiên cụm từ dài)
        return meaningfulPhrases.stream()
                .sorted((a, b) -> {
                    int wordsA = a.split("\\s+").length;
                    int wordsB = b.split("\\s+").length;
                    if (wordsA != wordsB) {
                        return Integer.compare(wordsB, wordsA); // Ưu tiên nhiều từ hơn
                    }
                    return Integer.compare(b.length(), a.length()); // Rồi ưu tiên dài hơn
                })
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * Trích xuất từ khóa kèm điểm TF-IDF
     * Hữu ích khi cần biết mức độ quan trọng tương đối của mỗi từ khóa
     * 
     * @param content Nội dung văn bản
     * @return Map từ keyword đến điểm TF-IDF (đã normalize về 0-1)
     */
    public Map<String, Double> getKeywordsWithScores(String content) {
        return getKeywordsWithScores(content, DEFAULT_TOP_KEYWORDS);
    }

    /**
     * Trích xuất từ khóa kèm điểm TF-IDF với số lượng tùy chỉnh
     * 
     * @param content Nội dung văn bản
     * @param topN    Số lượng từ khóa
     * @return Map từ keyword đến điểm TF-IDF (đã normalize về 0-1)
     */
    public Map<String, Double> getKeywordsWithScores(String content, int topN) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Double> tfidfScores = calculateTFIDF(content);

        if (tfidfScores.isEmpty()) {
            return Collections.emptyMap();
        }

        // Normalize scores về 0-1
        double maxScore = Collections.max(tfidfScores.values());

        Map<String, Double> normalizedScores = new LinkedHashMap<>();
        tfidfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .forEach(entry -> {
                    double normalizedScore = entry.getValue() / maxScore;
                    normalizedScores.put(entry.getKey(), Math.round(normalizedScore * 100) / 100.0);
                });

        return normalizedScores;
    }

    /**
     * ============================================================================
     * CORE ALGORITHM: Tính TF-IDF scores
     * ============================================================================
     * 
     * Bước 1: Tách văn bản thành các câu (coi mỗi câu là một "document")
     * Bước 2: Tokenize và loại bỏ stopwords cho mỗi câu
     * Bước 3: Tính Term Frequency (TF) cho mỗi từ
     * Bước 4: Tính Document Frequency (DF) - số câu chứa từ
     * Bước 5: Tính IDF = log(tổng số câu / DF)
     * Bước 6: Tính TF-IDF = TF × IDF
     * 
     * @param content Nội dung văn bản
     * @return Map từ word đến điểm TF-IDF
     */
    public Map<String, Double> calculateTFIDF(String content) {
        Map<String, Double> tfidfScores = new HashMap<>();

        // Bước 1: Tách thành các câu
        List<String> sentences = textService.splitSentences(content);

        if (sentences.isEmpty()) {
            // Fallback: coi toàn bộ content là 1 câu
            sentences = Collections.singletonList(content);
        }

        // Bước 2 & 3: Tính Term Frequency và Document Frequency
        Map<String, Integer> termFrequency = new HashMap<>(); // Tổng số lần xuất hiện
        Map<String, Integer> documentFrequency = new HashMap<>(); // Số câu chứa từ
        int totalWords = 0;

        for (String sentence : sentences) {
            // Tokenize và loại bỏ stopwords
            List<String> words = textService.tokenize(sentence);
            words = textService.removeStopwords(words);

            // Filter: chỉ lấy từ đủ dài và có nghĩa
            words = words.stream()
                    .filter(w -> w.length() >= MIN_WORD_LENGTH)
                    .filter(w -> !w.matches("\\d+")) // Loại bỏ số thuần
                    .filter(w -> !isCommonStopword(w)) // Loại bỏ stopword phổ biến
                    .collect(Collectors.toList());

            Set<String> uniqueWordsInSentence = new HashSet<>();

            for (String word : words) {
                String lowerWord = word.toLowerCase();

                // Term Frequency: đếm tổng số lần xuất hiện
                termFrequency.merge(lowerWord, 1, Integer::sum);
                totalWords++;

                // Document Frequency: chỉ đếm 1 lần mỗi câu
                uniqueWordsInSentence.add(lowerWord);
            }

            // Cập nhật Document Frequency
            for (String word : uniqueWordsInSentence) {
                documentFrequency.merge(word, 1, Integer::sum);
            }
        }

        // Bước 4, 5, 6: Tính TF-IDF cho mỗi từ
        int numSentences = sentences.size();
        int finalTotalWords = totalWords;

        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            String word = entry.getKey();
            int tf = entry.getValue();
            int df = documentFrequency.getOrDefault(word, 1);

            // Chỉ xét từ xuất hiện ít nhất MIN_WORD_FREQUENCY lần
            if (tf < MIN_WORD_FREQUENCY) {
                continue;
            }

            // TF = số lần xuất hiện / tổng số từ
            double termFreq = (double) tf / finalTotalWords;

            // IDF = log(tổng số câu / số câu chứa từ) + 1
            // +1 để tránh IDF = 0 khi từ xuất hiện ở tất cả các câu
            double inverseDocFreq = Math.log((double) numSentences / df) + 1;

            // TF-IDF = TF × IDF
            double tfidf = termFreq * inverseDocFreq;

            tfidfScores.put(word, tfidf);
        }

        return tfidfScores;
    }

    /**
     * Tính điểm TF-IDF cho một câu dựa trên tổng điểm các từ trong câu
     * Sử dụng trong SummaryService để xác định câu quan trọng
     * 
     * @param sentence    Câu cần tính điểm
     * @param tfidfScores Map điểm TF-IDF của các từ (từ calculateTFIDF)
     * @return Điểm của câu (đã normalize theo độ dài câu)
     */
    public double calculateSentenceScore(String sentence, Map<String, Double> tfidfScores) {
        List<String> words = textService.tokenize(sentence);
        words = textService.removeStopwords(words);

        if (words.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        for (String word : words) {
            totalScore += tfidfScores.getOrDefault(word.toLowerCase(), 0.0);
        }

        // Normalize bằng căn bậc hai của số từ
        // Tránh bias cho câu dài (nhiều từ → điểm cao hơn)
        return totalScore / Math.sqrt(words.size());
    }

    /**
     * Kiểm tra một từ có phải là từ khóa tiềm năng không
     * 
     * @param word Từ cần kiểm tra
     * @return true nếu từ có thể là keyword
     */
    public boolean isPotentialKeyword(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }

        String cleanWord = word.toLowerCase().trim();

        // Kiểm tra độ dài
        if (cleanWord.length() < MIN_WORD_LENGTH) {
            return false;
        }

        // Kiểm tra không phải số thuần
        if (cleanWord.matches("\\d+")) {
            return false;
        }

        // Kiểm tra không phải stopword
        if (textService.isStopword(cleanWord)) {
            return false;
        }

        return true;
    }

    /**
     * Kiểm tra từ phổ biến cần loại bỏ (đơn giản hóa)
     */
    private boolean isCommonStopword(String word) {
        if (word == null || word.trim().isEmpty()) {
            return true;
        }
        
        String clean = word.toLowerCase().trim();
        
        // Các từ cực kỳ phổ biến cần loại bỏ
        String[] veryCommonWords = {
            "và", "của", "có", "là", "để", "cho", "với", "từ", "tại", 
            "về", "được", "các", "một", "những", "này", "đó", "như", "sẽ"
        };
        
        for (String stopword : veryCommonWords) {
            if (clean.equals(stopword)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Kiểm tra từ có phải mảnh từ bị cắt không
     */
    private boolean isWordFragment(String word) {
        if (word == null || word.trim().isEmpty()) {
            return true;
        }
        
        String clean = word.toLowerCase().trim();
        
        // Những từ rõ ràng là mảnh từ (cần cập nhật dựa trên dữ liệu thực tế)
        String[] fragments = {
            "dụng", "thông", "trong", "người", "việc", "công", "nghi", 
            "phát", "triển", "quan", "trọng", "sinh", "viên", "làm",
            "cần", "nơi", "lưu", "trữ", "dung", "còn", "các", "cụ"
        };
        
        for (String fragment : fragments) {
            if (clean.equals(fragment)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Kiểm tra từ có phải từ tiếng Việt hợp lệ không
     */
    private boolean isValidVietnameseWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }
        
        String clean = word.toLowerCase().trim();
        
        // Từ quá ngắn
        if (clean.length() < 5) {
            return false;
        }
        
        // Từ hợp lệ phải chứa ít nhất 1 nguyên âm tiếng Việt
        if (!clean.matches(".*[aeiouăâêôơưàáảãạằắẳẵặầấẩẫậèéẻẽẹềếểễệìíỉĩịòóỏõọồốổỗộờớởỡợùúủũụừứửữựỳýỷỹỵ].*")) {
            return false;
        }
        
        // Phải chứa ít nhất 1 phụ âm
        if (!clean.matches(".*[bcdfghjklmnpqrstvwxyz].*")) {
            return false;
        }
        
        return true;
    }
}
