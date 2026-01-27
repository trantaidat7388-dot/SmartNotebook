package com.dat.notebook.service.ai;

import java.util.List;

public class AIEngine {

    public static String generateTitle(String text) {
        System.out.println("AIEngine: Generating Title for text length " + (text != null ? text.length() : 0));
        String title = TitleExtractor.extractTitle(text);
        System.out.println("AIEngine: Generated Title: " + title);
        return title;
    }

    public static String generateSummary(String text) {
        System.out.println("AIEngine: Generating Summary...");
        String summary = SummaryExtractor.summarize(text);
        System.out.println("AIEngine: Generated Summary: " + summary);
        return summary;
    }

    public static List<String> generateTags(String text) {
        System.out.println("AIEngine: Generating Tags...");
        return TagExtractor.extractTags(text);
    }
}
