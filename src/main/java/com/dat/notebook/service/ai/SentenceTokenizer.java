package com.dat.notebook.service.ai;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SentenceTokenizer {

    public static List<String> splitSentences(String text) {
        if (text == null || text.isBlank())
            return List.of();

        // Step 1: Pre-process
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
            // Filter short/noise sentences (Relaxed to 10 chars for debug)
            if (sentence.length() > 10) {
                sentences.add(sentence);
            }
        }
        System.out.println("SentenceTokenizer: Found " + sentences.size() + " valid sentences.");
        return sentences;
    }
}
