package com.dat.notebook.controller;

import com.dat.notebook.model.Note;
import com.dat.notebook.service.AIService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AIAssistantDialogController - Controller cho popup AI Assistant
 * 
 * THIẾT KẾ:
 * - Popup nhẹ, modal, hiển thị kết quả AI
 * - KHÔNG tạo tab mới
 * - KHÔNG tạo window mới
 * - CHỈ hiển thị kết quả và cho phép người dùng áp dụng
 * 
 * @author SmartNotebook Team
 */
public class AIAssistantDialogController {


    @FXML
    private VBox resultContainer;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Button btnClose;
    
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
     * Set ghi chú cần phân tích
     */
    public void setNote(Note note) {
        this.currentNote = note;
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
            String summary = aiService.summarizeNote(currentNote.getContent());
            
            Platform.runLater(() -> {
                showLoading(false);
                resultSummary = summary;
                summaryLabel.setText(summary);
                summaryBox.setVisible(true);
            });
        });
    }
    
    @FXML
    private void handleCopySummary() {
        if (resultSummary != null) {
            copyToClipboard(resultSummary);
            showSuccess("Đã copy tóm tắt!");
        }
    }
    

    
    @FXML
    private void handleSuggestTitle() {
        if (currentNote == null || currentNote.getContent() == null || currentNote.getContent().trim().isEmpty()) {
            showWarning("Ghi chú không có nội dung để phân tích.");
            return;
        }
        
        showLoading(true);
        titleBox.setVisible(false);
        
        CompletableFuture.runAsync(() -> {
            String title = aiService.suggestTitle(currentNote.getContent());
            
            Platform.runLater(() -> {
                showLoading(false);
                resultTitle = title;
                titleLabel.setText(title);
                titleBox.setVisible(true);
            });
        });
    }
    
    @FXML
    private void handleCopyTitle() {
        if (resultTitle != null) {
            copyToClipboard(resultTitle);
            showSuccess("Đã copy tiêu đề!");
        }
    }
    

    
    @FXML
    private void handleSuggestTags() {
        if (currentNote == null || currentNote.getContent() == null || currentNote.getContent().trim().isEmpty()) {
            showWarning("Ghi chú không có nội dung để phân tích.");
            return;
        }
        
        showLoading(true);
        tagsBox.setVisible(false);
        
        CompletableFuture.runAsync(() -> {
            List<String> tags = aiService.suggestTags(currentNote.getContent());
            String tagsText = aiService.formatTags(tags);
            
            Platform.runLater(() -> {
                showLoading(false);
                resultTags = tags;
                tagsLabel.setText(tagsText);
                tagsBox.setVisible(true);
            });
        });
    }
    
    @FXML
    private void handleCopyTags() {
        if (resultTags != null && !resultTags.isEmpty()) {
            String tagsText = aiService.formatTags(resultTags);
            copyToClipboard(tagsText);
            showSuccess("Đã copy tags!");
        }
    }
    

    
    /**
     * Phân tích tất cả cùng lúc
     */
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
            AIService.AIResult result = aiService.analyzeNote(currentNote.getContent());
            
            Platform.runLater(() -> {
                showLoading(false);
                
                // Hiển thị tất cả kết quả
                resultSummary = result.getSummary();
                summaryLabel.setText(resultSummary);
                summaryBox.setVisible(true);
                
                resultTitle = result.getSuggestedTitle();
                titleLabel.setText(resultTitle);
                titleBox.setVisible(true);
                
                resultTags = result.getSuggestedTags();
                tagsLabel.setText(aiService.formatTags(resultTags));
                tagsBox.setVisible(true);
            });
        });
    }
    

    
    private void showLoading(boolean show) {
        loadingIndicator.setVisible(show);
    }
    
    private void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }
    
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Cảnh báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String message) {
        // Simple notification - có thể thay bằng Notification UI đẹp hơn
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thành công");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
        
        // Auto close after 1 second
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Platform.runLater(() -> alert.close());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    @FXML
    private void handleClose() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
    

    
    public String getResultSummary() { return resultSummary; }
    public String getResultTitle() { return resultTitle; }
    public List<String> getResultTags() { return resultTags; }
}
