package com.dat.notebook.service.ai;

import java.util.List;
import java.util.stream.Collectors;

public class SummaryExtractor {

    public static String summarize(String text) {
        List<String> sentences = SentenceTokenizer.splitSentences(text);

        List<String> selected = sentences.stream()
                .filter(TextValidator::isMeaningful)
                .limit(3)
                .collect(Collectors.toList());

        if (selected.size() < 2) {
            return "Không thể tóm tắt nội dung này";
        }
        return String.join(" ", selected);
    }
}
