package com.dat.notebook.service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SummaryService - Tóm tắt ghi chú bằng thuật toán TF-IDF (offline).
 * 
 * @author SmartNotebook Team
 */
public class SummaryService {

    private static SummaryService instance;
    private TextAnalysisService textService;

    private static final int MAX_SUMMARY_SENTENCES = 5;
    private static final int MIN_SUMMARY_SENTENCES = 3;

    private SummaryService() {
        textService = TextAnalysisService.getInstance();
    }

    public static synchronized SummaryService getInstance() {
        if (instance == null) {
            instance = new SummaryService();
        }
        return instance;
    }

    /**
     * Tóm tắt nội dung ghi chú.
     * 
     * @param content Nội dung ghi chú
     * @return Bản tóm tắt (3-5 câu quan trọng nhất)
     */
    public String summarize(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "Không có nội dung để tóm tắt.";
        }

        List<String> sentences = textService.splitSentences(content);

        if (sentences.isEmpty()) {
            return content.length() > 200 ? content.substring(0, 200) + "..." : content;
        }

        if (sentences.size() <= MIN_SUMMARY_SENTENCES) {
            return String.join(". ", sentences) + ".";
        }

        Map<String, Double> tfidfScores = calculateTFIDF(sentences);

        Map<Integer, Double> sentenceScores = new HashMap<>();
        for (int i = 0; i < sentences.size(); i++) {
            double score = calculateSentenceScore(sentences.get(i), tfidfScores);
            if (i == 0)
                score *= 1.2;
            if (i == sentences.size() - 1)
                score *= 1.1;
            sentenceScores.put(i, score);
        }

        int numSentences = Math.min(MAX_SUMMARY_SENTENCES, Math.max(MIN_SUMMARY_SENTENCES, sentences.size() / 3));

        List<Integer> topIndices = sentenceScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(numSentences)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < topIndices.size(); i++) {
            if (i > 0)
                summary.append(" ");
            String sentence = sentences.get(topIndices.get(i));
            summary.append("• ").append(sentence);
            if (!sentence.endsWith(".") && !sentence.endsWith("!") && !sentence.endsWith("?")) {
                summary.append(".");
            }
            summary.append("\n");
        }

        return summary.toString().trim();
    }

    /**
     * Tóm tắt dạng bullet points
     * 
     * @param content   Nội dung ghi chú
     * @param numPoints Số điểm tóm tắt (3-5)
     * @return Danh sách các điểm tóm tắt
     */
    public List<String> summarizeToBulletPoints(String content, int numPoints) {
        numPoints = Math.max(MIN_SUMMARY_SENTENCES, Math.min(MAX_SUMMARY_SENTENCES, numPoints));

        if (content == null || content.trim().isEmpty()) {
            return Collections.singletonList("Không có nội dung để tóm tắt.");
        }

        List<String> sentences = textService.splitSentences(content);

        if (sentences.size() <= numPoints) {
            return sentences;
        }

        Map<String, Double> tfidfScores = calculateTFIDF(sentences);
        Map<Integer, Double> sentenceScores = new HashMap<>();

        for (int i = 0; i < sentences.size(); i++) {
            double score = calculateSentenceScore(sentences.get(i), tfidfScores);
            if (i == 0)
                score *= 1.2;
            sentenceScores.put(i, score);
        }

        return sentenceScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(numPoints)
                .map(Map.Entry::getKey)
                .sorted()
                .map(sentences::get)
                .collect(Collectors.toList());
    }

    /**
     * Tính TF-IDF scores cho document. TF-IDF = TF * IDF
     */
    private Map<String, Double> calculateTFIDF(List<String> sentences) {
        Map<String, Double> tfidfScores = new HashMap<>();
        Map<String, Integer> documentFrequency = new HashMap<>();
        Map<String, Integer> termFrequency = new HashMap<>();
        int totalWords = 0;

        for (String sentence : sentences) {
            List<String> words = textService.tokenize(sentence);
            words = textService.removeStopwords(words);

            Set<String> uniqueWordsInSentence = new HashSet<>();

            for (String word : words) {
                termFrequency.merge(word, 1, Integer::sum);
                totalWords++;
                uniqueWordsInSentence.add(word);
            }

            for (String word : uniqueWordsInSentence) {
                documentFrequency.merge(word, 1, Integer::sum);
            }
        }

        int numSentences = sentences.size();
        int finalTotalWords = totalWords;

        for (String word : termFrequency.keySet()) {
            double tf = (double) termFrequency.get(word) / finalTotalWords;
            double idf = Math.log((double) numSentences / (documentFrequency.get(word) + 1)) + 1;
            tfidfScores.put(word, tf * idf);
        }

        return tfidfScores;
    }

    /**
     * Tính điểm của một câu dựa trên TF-IDF của các từ trong câu
     */
    private double calculateSentenceScore(String sentence, Map<String, Double> tfidfScores) {
        List<String> words = textService.tokenize(sentence);
        words = textService.removeStopwords(words);

        if (words.isEmpty())
            return 0.0;

        double totalScore = 0.0;
        for (String word : words) {
            totalScore += tfidfScores.getOrDefault(word, 0.0);
        }

        // Normalize by sentence length để tránh bias cho câu dài
        return totalScore / Math.sqrt(words.size());
    }

    /**
     * Lấy tỷ lệ nén (compression ratio)
     */
    public double getCompressionRatio(String original, String summary) {
        if (original == null || original.isEmpty())
            return 0;
        return 1.0 - ((double) summary.length() / original.length());
    }
}
