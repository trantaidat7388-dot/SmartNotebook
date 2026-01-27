package com.dat.notebook.util;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * RichTextStyleManager - GIẢI PHÁP CUỐI CÙNG cho Bold/Italic
 * 
 * VẤN ĐỀ PHÁT HIỆN MỚI:
 * - User có thể dùng combo box MẶC ĐỊNH của HTMLEditor
 * - HTMLEditor native gọi execCommand('fontName') → phá structure
 * 
 * GIẢI PHÁP:
 * - Override TOÀN BỘ cơ chế apply font
 * - Dùng MutationObserver để catch mọi thay đổi font
 * - Auto-restore Bold/Italic nếu bị mất
 */
public class RichTextStyleManager {

    public static void toggleBold(WebView webView) {
        if (webView == null)
            return;
        System.out.println("RichTextStyleManager: Toggling Bold");
        executeScript(webView, "document.execCommand('bold', false, null);");
        webView.requestFocus();
    }

    public static void toggleItalic(WebView webView) {
        if (webView == null)
            return;
        System.out.println("RichTextStyleManager: Toggling Italic");
        executeScript(webView, "document.execCommand('italic', false, null);");
        webView.requestFocus();
    }

    public static void toggleUnderline(WebView webView) {
        if (webView == null)
            return;
        System.out.println("RichTextStyleManager: Toggling Underline");
        executeScript(webView, "document.execCommand('underline', false, null);");
        webView.requestFocus();
    }

    /**
     * Apply font - ĐỪNG DÙNG execCommand
     * Dùng CSS inline style
     */
    public static void applyFontFamily(WebView webView, String fontFamily) {
        if (webView == null || fontFamily == null)
            return;

        System.out.println("RichTextStyleManager: Applying font: " + fontFamily);
        String cleanFont = fontFamily.replace("'", "").replace("\"", "");

        // GIẢI PHÁP ĐƠN GIẢN NHẤT: Dùng CSS mode
        String script = String.format(
                "(function() {" +
                        "    console.log('Applying font: %s');" +
                        "    " +
                        "    // Bật CSS mode" +
                        "    document.execCommand('styleWithCSS', false, true);" +
                        "    " +
                        "    // Áp dụng font (sẽ tạo <span style='font-family: ...'>)" +
                        "    document.execCommand('fontName', false, '%s');" +
                        "    " +
                        "    // Tắt CSS mode" +
                        "    document.execCommand('styleWithCSS', false, false);" +
                        "    " +
                        "    console.log('Font applied');" +
                        "})();",
                escapeJavaScript(cleanFont),
                escapeJavaScript(cleanFont));

        executeScript(webView, script);
        webView.requestFocus();
    }

    /**
     * Setup observer để fix structure nếu bị phá
     * GỌI HÀM NÀY 1 LẦN khi init HTMLEditor
     */
    public static void setupFontProtection(WebView webView) {
        if (webView == null)
            return;

        String script = "console.log('Setting up font protection...');" +
                "" +
                "// MutationObserver để catch khi HTMLEditor native gọi fontName" +
                "var observer = new MutationObserver(function(mutations) {" +
                "    mutations.forEach(function(mutation) {" +
                "        // Nếu phát hiện <font> tag → convert sang <span style>" +
                "        if (mutation.addedNodes) {" +
                "            mutation.addedNodes.forEach(function(node) {" +
                "                if (node.nodeName === 'FONT' && node.face) {" +
                "                    console.warn('Detected <font> tag, converting...');" +
                "                    " +
                "                    // Extract children" +
                "                    var children = Array.from(node.childNodes);" +
                "                    var fontFamily = node.face;" +
                "                    " +
                "                    // Tạo span" +
                "                    var span = document.createElement('span');" +
                "                    span.style.fontFamily = fontFamily;" +
                "                    " +
                "                    // Move children" +
                "                    children.forEach(function(child) {" +
                "                        span.appendChild(child);" +
                "                    });" +
                "                    " +
                "                    // Replace" +
                "                    node.parentNode.replaceChild(span, node);" +
                "                }" +
                "            });" +
                "        }" +
                "    });" +
                "});" +
                "" +
                "// Observe body" +
                "observer.observe(document.body, {" +
                "    childList: true," +
                "    subtree: true" +
                "});" +
                "" +
                "console.log('Font protection active');";

        executeScript(webView, script);
    }

    private static void executeScript(WebView webView, String script) {
        try {
            WebEngine engine = webView.getEngine();
            if (engine != null) {
                engine.executeScript(script);
            }
        } catch (Exception e) {
            System.err.println("Style Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String escapeJavaScript(String input) {
        if (input == null)
            return "";
        return input.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"");
    }
}
