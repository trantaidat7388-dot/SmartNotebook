package com.dat.notebook.service.ai;

import java.util.Arrays;
import java.util.List;

public class TitleExtractor {

    public static String extractTitle(String text) {
        List<String> sentences = SentenceTokenizer.splitSentences(text);

        for (String sentence : sentences) {
            String title = extractPhrase(sentence);
            if (TextValidator.isMeaningful(title)) {
                return title;
            }
        }
        return "Ghi chú không có tiêu đề phù hợp";
    }

    private static String extractPhrase(String sentence) {
        String[] words = sentence.split("\\s+");
        if (words.length < 4)
            return "";

        int max = Math.min(words.length, 10);
        return String.join(" ", Arrays.copyOfRange(words, 0, max));
    }
}
