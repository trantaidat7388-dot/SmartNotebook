package com.dat.notebook.service.ai;

public class TextValidator {

    public static boolean isMeaningful(String text) {
        if (text == null || text.isBlank())
            return false;

        // Relaxed length check for debugging
        if (text.length() < 5)
            return false;

        // Simplified regex to match any letter (Unicode support)
        if (!text.matches(".*\\p{L}.*"))
            return false;

        // Prevent meaningless repetition
        if (text.matches(".*(xã|tính|giới){3,}.*"))
            return false;

        return true;
    }
}
