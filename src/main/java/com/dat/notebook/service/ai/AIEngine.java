package com.dat.notebook.service.ai;

import com.dat.notebook.service.TitleSuggestionService;
import java.util.List;

public class AIEngine {

    public static String generateTitle(String text) {
        System.out.println("AIEngine: Generating Title for text length " + (text != null ? text.length() : 0));
        String title = TitleSuggestionService.getInstance().suggestTitle(text);
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
        return KeywordExtractor.getInstance().extractMeaningfulKeywords(text, 8);
    }
}
