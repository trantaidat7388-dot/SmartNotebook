package com.dat.notebook.controller;

import com.dat.notebook.model.Note;
import com.dat.notebook.service.AIService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AIAssistantViewController - Controller cho AI Assistant Tab (Full Screen)
 * 
 * THIẾT KẾ:
 * - Tab toàn màn hình trong MainView
 * - Hiển thị kết quả AI với giao diện phóng to
 * - Tích hợp với note hiện tại từ MainController
 * 
 * @author SmartNotebook Team
 */
public class AIAssistantViewController {


    @FXML
    private VBox resultContainer;
    @FXML
    private ProgressIndicator loadingIndicator;
    
    // Result display areas
    @FXML
    private VBox summaryBox;
    @FXML
    private TextArea summaryLabel;
    @FXML
    private Button btnCopySummary;
    
    @FXML
    private VBox titleBox;
    @FXML
    private TextArea titleLabel;
    @FXML
    private Button btnCopyTitle;
    
    @FXML
    private VBox tagsBox;
    @FXML
    private TextArea tagsLabel;
    @FXML
    private Button btnCopyTags;


    private Note currentNote;
    private AIService aiService;
    private Runnable onBackCallback;
    
    // Kết quả AI
    private String resultSummary;
    private String resultTitle;
    private List<String> resultTags;
    
    @FXML
    public void initialize() {
        aiService = AIService.getInstance();
        
        // Hide all result boxes initially
        summaryBox.setVisible(false);
        titleBox.setVisible(false);
        tagsBox.setVisible(false);
        loadingIndicator.setVisible(false);
    }
    
    /**
     * Set callback for back button
     */
    public void setOnBack(Runnable callback) {
        this.onBackCallback = callback;
    }
    
    /**
     * Handle back button
     */
    @FXML
    private void handleBack() {
        if (onBackCallback != null) {
            onBackCallback.run();
        }
    }
    
    /**
     * Set ghi chú cần phân tích
     */
    public void setNote(Note note) {
        this.currentNote = note;
        
        // Clear previous results
        clearResults();
    }
    
    /**
     * Clear all results
     */
    private void clearResults() {
        summaryBox.setVisible(false);
        titleBox.setVisible(false);
        tagsBox.setVisible(false);
        summaryLabel.clear();
        titleLabel.clear();
        tagsLabel.clear();
    }
    

    
    @FXML
    private void handleSummarize() {
        if (currentNote == null || currentNote.getContent() == null || currentNote.getContent().trim().isEmpty()) {
            showWarning("Ghi chú không có nội dung để tóm tắt.");
            return;
        }
        
        showLoading(true);
        summaryBox.setVisible(false);
        
        CompletableFuture.runAsync(() -> {
            try {
                String content = currentNote.getContent();
                String summary = aiService.summarizeNote(content);
                resultSummary = summary;
                
                Platform.runLater(() -> {
                    summaryLabel.setText(summary);
                    summaryBox.setVisible(true);
                    showLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showLoading(false);
                    showError("Lỗi khi tóm tắt: " + e.getMessage());
                });
            }
        });
    }
    

    
    @FXML
    private void handleSuggestTitle() {
        if (currentNote == null || currentNote.getContent() == null || currentNote.getContent().trim().isEmpty()) {
            showWarning("Ghi chú không có nội dung để gợi ý tiêu đề.");
            return;
        }
        
        showLoading(true);
        titleBox.setVisible(false);
        
        CompletableFuture.runAsync(() -> {
            try {
                String content = currentNote.getContent();
                String title = aiService.suggestTitle(content);
                resultTitle = title;
                
                Platform.runLater(() -> {
                    titleLabel.setText(title);
                    titleBox.setVisible(true);
                    showLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showLoading(false);
                    showError("Lỗi khi gợi ý tiêu đề: " + e.getMessage());
                });
            }
        });
    }
    

    
    @FXML
    private void handleSuggestTags() {
        if (currentNote == null || currentNote.getContent() == null || currentNote.getContent().trim().isEmpty()) {
            showWarning("Ghi chú không có nội dung để gợi ý tags.");
            return;
        }
        
        showLoading(true);
        tagsBox.setVisible(false);
        
        CompletableFuture.runAsync(() -> {
            try {
                String content = currentNote.getContent();
                List<String> tags = aiService.suggestTags(content);
                resultTags = tags;
                
                String tagsStr = String.join(", ", tags);
                
                Platform.runLater(() -> {
                    tagsLabel.setText(tagsStr);
                    tagsBox.setVisible(true);
                    showLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showLoading(false);
                    showError("Lỗi khi gợi ý tags: " + e.getMessage());
                });
            }
        });
    }
    

    
    @FXML
    private void handleAnalyzeAll() {
        if (currentNote == null || currentNote.getContent() == null || currentNote.getContent().trim().isEmpty()) {
            showWarning("Ghi chú không có nội dung để phân tích.");
            return;
        }
        
        showLoading(true);
        summaryBox.setVisible(false);
        titleBox.setVisible(false);
        tagsBox.setVisible(false);
        
        CompletableFuture.runAsync(() -> {
            try {
                String content = currentNote.getContent();
                
                // Run all analyses
                String summary = aiService.summarizeNote(content);
                String title = aiService.suggestTitle(content);
                List<String> tags = aiService.suggestTags(content);
                
                resultSummary = summary;
                resultTitle = title;
                resultTags = tags;
                
                String tagsStr = String.join(", ", tags);
                
                Platform.runLater(() -> {
                    summaryLabel.setText(summary);
                    titleLabel.setText(title);
                    tagsLabel.setText(tagsStr);
                    
                    summaryBox.setVisible(true);
                    titleBox.setVisible(true);
                    tagsBox.setVisible(true);
                    
                    showLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showLoading(false);
                    showError("Lỗi khi phân tích: " + e.getMessage());
                });
            }
        });
    }
    

    
    @FXML
    private void handleCopySummary() {
        if (resultSummary != null && !resultSummary.isEmpty()) {
            copyToClipboard(resultSummary);
            showInfo("Đã copy tóm tắt vào clipboard!");
        }
    }
    
    @FXML
    private void handleCopyTitle() {
        if (resultTitle != null && !resultTitle.isEmpty()) {
            copyToClipboard(resultTitle);
            showInfo("Đã copy tiêu đề vào clipboard!");
        }
    }
    
    @FXML
    private void handleCopyTags() {
        if (resultTags != null && !resultTags.isEmpty()) {
            String tagsStr = String.join(", ", resultTags);
            copyToClipboard(tagsStr);
            showInfo("Đã copy tags vào clipboard!");
        }
    }
    

    
    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }
    
    private void showLoading(boolean show) {
        loadingIndicator.setVisible(show);
    }
    
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Cảnh báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
