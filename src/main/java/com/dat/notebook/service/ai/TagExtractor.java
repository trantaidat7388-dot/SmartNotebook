package com.dat.notebook.service.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TagExtractor {

    public static List<String> extractTags(String text) {
        Map<String, Integer> freq = new HashMap<>();

        if (text == null)
            return List.of();

        String clean = text.toLowerCase()
                .replaceAll("[^a-zA-ZÀ-ỹ\\s]", "");

        for (String word : clean.split("\\s+")) {
            if (word.length() < 4)
                continue;
            freq.put(word, freq.getOrDefault(word, 0) + 1);
        }

        return freq.entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
