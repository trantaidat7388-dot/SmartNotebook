package com.dat.notebook.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * TextAnalysisService - Service cơ bản xử lý văn bản
 * 
 * Cung cấp các phương thức:
 * - Tokenize (tách từ)
 * - Split sentences (tách câu)
 * - Remove stopwords (loại bỏ từ dừng)
 * - Normalize text (chuẩn hóa văn bản)
 * 
 * @author SmartNotebook Team
 */
public class TextAnalysisService {

    // Singleton instance
    private static TextAnalysisService instance;

    // Vietnamese stopwords set
    private Set<String> stopwords;

    // Regex patterns
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?]+\\s*");
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\s\\p{Punct}]+");
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private TextAnalysisService() {
        loadStopwords();
    }

    public static synchronized TextAnalysisService getInstance() {
        if (instance == null) {
            instance = new TextAnalysisService();
        }
        return instance;
    }

    /**
     * Load Vietnamese stopwords từ file resources
     */
    private void loadStopwords() {
        stopwords = new HashSet<>();
        try {
            InputStream is = getClass().getResourceAsStream("/nlp/vietnamese-stopwords.txt");
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        stopwords.add(line);
                    }
                }
                reader.close();
                System.out.println("Loaded " + stopwords.size() + " Vietnamese stopwords");
            }
        } catch (Exception e) {
            System.err.println("Error loading stopwords: " + e.getMessage());
            // Load default stopwords nếu file không tồn tại
            loadDefaultStopwords();
        }

        // Thêm stopwords tiếng Anh phổ biến
        addEnglishStopwords();
    }

    private void loadDefaultStopwords() {
        String[] defaults = { "và", "của", "là", "có", "một", "này", "được", "trong",
                "cho", "để", "với", "không", "các", "những", "người", "trên", "từ" };
        stopwords.addAll(Arrays.asList(defaults));
    }

    private void addEnglishStopwords() {
        String[] english = { "the", "a", "an", "is", "are", "was", "were", "be", "been",
                "being", "have", "has", "had", "do", "does", "did", "will", "would", "could",
                "should", "may", "might", "must", "shall", "can", "need", "dare", "ought",
                "used", "to", "of", "in", "for", "on", "with", "at", "by", "from", "as",
                "into", "through", "during", "before", "after", "above", "below", "between",
                "under", "again", "further", "then", "once", "here", "there", "when", "where",
                "why", "how", "all", "each", "few", "more", "most", "other", "some", "such",
                "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very", "just",
                "and", "but", "if", "or", "because", "until", "while", "it", "this", "that" };
        stopwords.addAll(Arrays.asList(english));
    }

    /**
     * Tách văn bản thành các câu
     * 
     * @param text Văn bản đầu vào
     * @return Danh sách các câu
     */
    public List<String> splitSentences(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Tách theo dấu chấm, chấm hỏi, chấm than
        String[] sentences = SENTENCE_PATTERN.split(text.trim());

        List<String> result = new ArrayList<>();
        for (String sentence : sentences) {
            sentence = sentence.trim();
            // Chỉ lấy câu có độ dài hợp lý (ít nhất 10 ký tự)
            if (sentence.length() >= 10) {
                result.add(sentence);
            }
        }

        return result;
    }

    /**
     * Tách văn bản thành các từ (tokens)
     * 
     * @param text Văn bản đầu vào
     * @return Danh sách các từ
     */
    public List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Tách theo khoảng trắng và dấu câu
        String[] tokens = WORD_PATTERN.split(text.toLowerCase().trim());

        List<String> result = new ArrayList<>();
        for (String token : tokens) {
            token = token.trim();
            // Chỉ lấy từ có độ dài >= 2
            if (token.length() >= 2) {
                result.add(token);
            }
        }

        return result;
    }

    /**
     * Loại bỏ stopwords từ danh sách từ
     * 
     * @param words Danh sách từ đầu vào
     * @return Danh sách từ đã loại bỏ stopwords
     */
    public List<String> removeStopwords(List<String> words) {
        if (words == null || words.isEmpty()) {
            return Collections.emptyList();
        }

        return words.stream()
                .filter(word -> !stopwords.contains(word.toLowerCase()))
                .filter(word -> word.length() >= 2)
                .collect(Collectors.toList());
    }

    /**
     * Chuẩn hóa văn bản: lowercase, trim
     * 
     * @param text Văn bản đầu vào
     * @return Văn bản đã chuẩn hóa
     */
    public String normalize(String text) {
        if (text == null)
            return "";
        return text.toLowerCase().trim();
    }

    /**
     * Loại bỏ dấu tiếng Việt
     * 
     * @param text Văn bản đầu vào
     * @return Văn bản không dấu
     */
    public String removeDiacritics(String text) {
        if (text == null)
            return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
    }

    /**
     * Kiểm tra từ có phải stopword không
     * 
     * @param word Từ cần kiểm tra
     * @return true nếu là stopword
     */
    public boolean isStopword(String word) {
        return stopwords.contains(word.toLowerCase());
    }

    /**
     * Đếm số từ trong văn bản
     * 
     * @param text Văn bản đầu vào
     * @return Số từ
     */
    public int countWords(String text) {
        return tokenize(text).size();
    }

    /**
     * Đếm số câu trong văn bản
     * 
     * @param text Văn bản đầu vào
     * @return Số câu
     */
    public int countSentences(String text) {
        return splitSentences(text).size();
    }
}
