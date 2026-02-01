package com.dat.notebook.util;

import javafx.application.Platform;
import javafx.scene.web.WebView;

/**
 * SIMPLIFIED RichTextStyleManager - Fixed JavaScript syntax errors
 * 
 * @author Dat
 * @since 2026
 */
public class RichTextStyleManager {
    
    /**
     * Toggle Bold - Simple and reliable
     */
    public static void toggleBold(WebView webView) {
        if (webView == null) return;
        
        System.out.println("🟡 RichTextStyleManager: Toggle Bold");
        executeScript(webView, "document.execCommand('bold', false, null);");
        Platform.runLater(() -> webView.requestFocus());
    }
    
    /**
     * Toggle Italic - Simple and reliable
     */
    public static void toggleItalic(WebView webView) {
        if (webView == null) return;
        
        System.out.println("🟡 RichTextStyleManager: Toggle Italic");
        executeScript(webView, "document.execCommand('italic', false, null);");
        Platform.runLater(() -> webView.requestFocus());
    }
    
    /**
     * Toggle Underline - Simple and reliable
     */
    public static void toggleUnderline(WebView webView) {
        if (webView == null) return;
        
        System.out.println("🟡 RichTextStyleManager: Toggle Underline");
        executeScript(webView, "document.execCommand('underline', false, null);");
        Platform.runLater(() -> webView.requestFocus());
    }
    
    /**
     * Apply Font Family - Simple and reliable
     */
    public static void applyFontFamily(WebView webView, String fontFamily) {
        if (webView == null || fontFamily == null) return;
        
        System.out.println("🟡 RichTextStyleManager: Apply Font: " + fontFamily);
        executeScript(webView, "document.execCommand('fontName', false, '" + fontFamily + "');");
        Platform.runLater(() -> webView.requestFocus());
    }
    
    /**
     * Execute JavaScript safely
     */
    private static void executeScript(WebView webView, String script) {
        try {
            if (webView != null && webView.getEngine() != null) {
                webView.getEngine().executeScript(script);
            }
        } catch (Exception e) {
            System.err.println("❌ JavaScript Error: " + e.getMessage());
        }
    }
}