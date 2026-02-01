package com.dat.notebook.service.ai;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SentenceTokenizer {

    // Minimum sentence length - allow very short meaningful sentences
    private static final int MIN_SENTENCE_LENGTH = 3;
    
    // Filter out common noise patterns
    private static final List<String> NOISE_PATTERNS = List.of(
        "^[\\s\\d\\W]+$",  // Only whitespace/numbers/punctuation
        "^\\s*[.!?\\-_]*\\s*$"  // Only punctuation
    );

    public static List<String> splitSentences(String text) {
        if (text == null || text.isBlank())
            return List.of();

        // Step 1: Pre-process - preserve meaningful whitespace
        String cleanText = text
                .replaceAll("\\s+", " ")
                .replaceAll("[\\n\\r]", " ")
                .trim();

        // Step 2: Use BreakIterator for locale-aware splitting (VN)
        BreakIterator iterator = BreakIterator.getSentenceInstance(new Locale("vi", "VN"));
        iterator.setText(cleanText);

        List<String> sentences = new ArrayList<>();
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            String sentence = cleanText.substring(start, end).trim();
            
            // Filter out noise but keep meaningful short sentences
            if (isValidSentence(sentence)) {
                sentences.add(sentence);
            }
        }
        
        System.out.println("🟡 SentenceTokenizer: Found " + sentences.size() + " valid sentences from " + 
                           cleanText.length() + " characters.");
        return sentences;
    }
    
    /**
     * Check if sentence is meaningful and not noise
     * - Must be at least 3 characters
     * - Should not be pure punctuation/numbers
     * - Should contain at least one letter
     */
    private static boolean isValidSentence(String sentence) {
        if (sentence == null || sentence.length() < MIN_SENTENCE_LENGTH) {
            return false;
        }
        
        // Check if it's pure noise (only punctuation, numbers, whitespace)
        for (String pattern : NOISE_PATTERNS) {
            if (sentence.matches(pattern)) {
                return false;
            }
        }
        
        // Check if sentence contains at least one letter (meaningful)
        return sentence.matches(".*[\\p{L}\\p{N}].*");
    }
}
