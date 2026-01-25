package com.dat.notebook.controller;

import com.dat.notebook.model.Note;
import com.dat.notebook.service.SummaryService;
import com.dat.notebook.service.TagSuggestionService;
import com.dat.notebook.service.TitleSuggestionService;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AiChatController - Controller cho màn hình AI Assistant với 3 chức năng NLP.
 * 
 * @author SmartNotebook Team
 */
public class AiChatController {

    @FXML
    private ScrollPane chatScrollPane;
    @FXML
    private VBox chatContainer;
    @FXML
    private TextField messageInput;
    @FXML
    private Button sendButton;
    @FXML
    private ProgressIndicator loadingIndicator;

    private SummaryService summaryService;
    private TitleSuggestionService titleService;
    private TagSuggestionService tagService;

    private Note currentNote;
    private String lastSummary;
    private String lastSuggestedTitle;
    private List<String> lastSuggestedTags;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        try {
            System.out.println("AiChatController: Initializing...");

            // Initialize NLP services with error handling
            try {
                summaryService = SummaryService.getInstance();
                titleService = TitleSuggestionService.getInstance();
                tagService = TagSuggestionService.getInstance();
                System.out.println("NLP Services initialized");
            } catch (Exception e) {
                System.err.println("Error initializing NLP services: " + e.getMessage());
                e.printStackTrace();
                addAiMessage("⚠️ Lỗi khởi động AI: " + e.getMessage());
            }

            // Welcome message
            addAiMessage("👋 Xin chào! Tôi là AI Assistant của Smart Notebook.\n\n" +
                    "Tôi có thể giúp bạn:\n" +
                    "📝 Tóm tắt ghi chú dài\n" +
                    "💡 Gợi ý tiêu đề phù hợp\n" +
                    "🏷️ Gợi ý tags tự động\n\n" +
                    "Hãy chọn một ghi chú và nhấn các nút bên dưới!");

        } catch (Exception e) {
            System.err.println("AiChatController initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Set note hiện tại để phân tích
     */
    public void setCurrentNote(Note note) {
        this.currentNote = note;
        if (note != null) {
            addAiMessage("📌 Đã chọn ghi chú: \"" + note.getTitle() + "\"\n" +
                    "Bạn muốn tôi làm gì với ghi chú này?");
        }
    }

    /**
     * Xử lý khi người dùng nhấn nút "Tóm tắt"
     */
    @FXML
    private void handleSummarize() {
        if (currentNote == null) {
            addAiMessage("⚠️ Vui lòng chọn một ghi chú trước.");
            return;
        }

        String content = currentNote.getContent();
        if (content == null || content.trim().isEmpty()) {
            addAiMessage("⚠️ Ghi chú này không có nội dung để tóm tắt.");
            return;
        }

        addUserMessage("📝 Tóm tắt ghi chú này");
        showLoading(true);

        CompletableFuture.runAsync(() -> {
            String summary = summaryService.summarize(content);
            lastSummary = summary;

            Platform.runLater(() -> {
                showLoading(false);
                addSummaryResult(summary);
            });
        });
    }

    /**
     * Hiển thị kết quả tóm tắt với nút Copy
     */
    private void addSummaryResult(String summary) {
        VBox resultBox = new VBox(10);
        resultBox.setAlignment(Pos.TOP_LEFT);
        resultBox.setPadding(new Insets(0, 50, 0, 0));
        resultBox.getStyleClass().add("ai-result-box");

        Label headerLabel = new Label("📝 Tóm tắt ghi chú:");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label summaryLabel = new Label(summary);
        summaryLabel.setWrapText(true);
        summaryLabel.getStyleClass().add("chat-message-ai");
        summaryLabel.setMaxWidth(380);

        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER_LEFT);

        Button copyBtn = new Button("📋 Copy");
        copyBtn.getStyleClass().add("ai-action-btn");
        copyBtn.setOnAction(e -> copyToClipboard(summary));

        Button applyBtn = new Button("✅ Áp dụng");
        applyBtn.getStyleClass().add("ai-action-btn-primary");
        applyBtn.setOnAction(e -> {
            addAiMessage("✅ Đã lưu tóm tắt! Bạn có thể paste vào ghi chú.");
            copyToClipboard(summary);
        });

        actionButtons.getChildren().addAll(copyBtn, applyBtn);

        Label timeLabel = new Label(TIME_FORMAT.format(LocalDateTime.now()));
        timeLabel.getStyleClass().add("chat-time");

        resultBox.getChildren().addAll(headerLabel, summaryLabel, actionButtons, timeLabel);
        chatContainer.getChildren().add(resultBox);
        scrollToBottom();
    }

    /**
     * Xử lý khi người dùng nhấn nút "Gợi ý tiêu đề"
     */
    @FXML
    private void handleSuggestTitle() {
        if (currentNote == null) {
            addAiMessage("⚠️ Vui lòng chọn một ghi chú trước.");
            return;
        }

        String content = currentNote.getContent();
        if (content == null || content.trim().isEmpty()) {
            addAiMessage("⚠️ Ghi chú này không có nội dung để phân tích.");
            return;
        }

        addUserMessage("💡 Gợi ý tiêu đề cho ghi chú");
        showLoading(true);

        CompletableFuture.runAsync(() -> {
            List<String> suggestions = titleService.suggestMultipleTitles(content, 3);
            lastSuggestedTitle = suggestions.isEmpty() ? null : suggestions.get(0);

            Platform.runLater(() -> {
                showLoading(false);
                addTitleSuggestions(suggestions);
            });
        });
    }

    /**
     * Hiển thị các gợi ý tiêu đề
     */
    private void addTitleSuggestions(List<String> suggestions) {
        VBox resultBox = new VBox(10);
        resultBox.setAlignment(Pos.TOP_LEFT);
        resultBox.setPadding(new Insets(0, 50, 0, 0));

        Label headerLabel = new Label("💡 Gợi ý tiêu đề:");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        resultBox.getChildren().add(headerLabel);

        for (int i = 0; i < suggestions.size(); i++) {
            String title = suggestions.get(i);
            HBox suggestionRow = new HBox(10);
            suggestionRow.setAlignment(Pos.CENTER_LEFT);

            Label numLabel = new Label((i + 1) + ".");
            numLabel.setStyle("-fx-font-weight: bold;");

            Label titleLabel = new Label(title);
            titleLabel.setWrapText(true);
            titleLabel.getStyleClass().add("chat-message-ai");
            titleLabel.setMaxWidth(280);

            Button useBtn = new Button("Dùng");
            useBtn.getStyleClass().add("ai-action-btn");
            final String selectedTitle = title;
            useBtn.setOnAction(e -> {
                copyToClipboard(selectedTitle);
                addAiMessage("✅ Đã copy tiêu đề: \"" + selectedTitle + "\"");
            });

            suggestionRow.getChildren().addAll(numLabel, titleLabel, useBtn);
            resultBox.getChildren().add(suggestionRow);
        }

        Label timeLabel = new Label(TIME_FORMAT.format(LocalDateTime.now()));
        timeLabel.getStyleClass().add("chat-time");
        resultBox.getChildren().add(timeLabel);

        chatContainer.getChildren().add(resultBox);
        scrollToBottom();
    }

    /**
     * Xử lý khi người dùng nhấn nút "Gợi ý tag"
     */
    @FXML
    private void handleSuggestTags() {
        if (currentNote == null) {
            addAiMessage("⚠️ Vui lòng chọn một ghi chú trước.");
            return;
        }

        String content = currentNote.getContent();
        if (content == null || content.trim().isEmpty()) {
            addAiMessage("⚠️ Ghi chú này không có nội dung để phân tích.");
            return;
        }

        addUserMessage("🏷️ Gợi ý tags cho ghi chú");
        showLoading(true);

        CompletableFuture.runAsync(() -> {
            List<String> tags = tagService.suggestTags(content);
            lastSuggestedTags = tags;

            Platform.runLater(() -> {
                showLoading(false);
                addTagSuggestions(tags);
            });
        });
    }

    /**
     * Hiển thị các gợi ý tag
     */
    private void addTagSuggestions(List<String> tags) {
        VBox resultBox = new VBox(10);
        resultBox.setAlignment(Pos.TOP_LEFT);
        resultBox.setPadding(new Insets(0, 50, 0, 0));

        Label headerLabel = new Label("🏷️ Gợi ý tags:");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Tags container (horizontal wrap)
        HBox tagsBox = new HBox(8);
        tagsBox.setAlignment(Pos.CENTER_LEFT);
        tagsBox.setStyle("-fx-wrap-text: true;");

        for (String tag : tags) {
            Label tagLabel = new Label("#" + tag);
            tagLabel.getStyleClass().add("tag-chip");
            tagLabel.setStyle("-fx-background-color: #e2e8f0; -fx-padding: 4 10; " +
                    "-fx-background-radius: 12; -fx-font-size: 12px; -fx-cursor: hand;");
            tagLabel.setOnMouseClicked(e -> {
                copyToClipboard(tag);
                addAiMessage("📋 Đã copy tag: #" + tag);
            });
            tagsBox.getChildren().add(tagLabel);
        }

        // Copy all button
        Button copyAllBtn = new Button("📋 Copy tất cả");
        copyAllBtn.getStyleClass().add("ai-action-btn");
        copyAllBtn.setOnAction(e -> {
            String allTags = String.join(", ", tags.stream().map(t -> "#" + t).toArray(String[]::new));
            copyToClipboard(allTags);
            addAiMessage("✅ Đã copy tất cả tags!");
        });

        Label timeLabel = new Label(TIME_FORMAT.format(LocalDateTime.now()));
        timeLabel.getStyleClass().add("chat-time");

        resultBox.getChildren().addAll(headerLabel, tagsBox, copyAllBtn, timeLabel);
        chatContainer.getChildren().add(resultBox);
        scrollToBottom();
    }

    @FXML
    private void handleSendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        addUserMessage(message);
        messageInput.clear();

        // Process message with NLP
        String response = processUserMessage(message);
        addAiMessage(response);
    }

    /**
     * Xử lý tin nhắn từ người dùng
     */
    private String processUserMessage(String message) {
        String lower = message.toLowerCase();

        if (lower.contains("tóm tắt") || lower.contains("summary") || lower.contains("summarize")) {
            if (currentNote != null && currentNote.getContent() != null) {
                lastSummary = summaryService.summarize(currentNote.getContent());
                return "📝 Đây là bản tóm tắt:\n\n" + lastSummary;
            }
            return "⚠️ Vui lòng chọn một ghi chú trước.";
        }

        if (lower.contains("tiêu đề") || lower.contains("title")) {
            if (currentNote != null && currentNote.getContent() != null) {
                lastSuggestedTitle = titleService.suggestTitle(currentNote.getContent());
                return "💡 Gợi ý tiêu đề: \"" + lastSuggestedTitle + "\"";
            }
            return "⚠️ Vui lòng chọn một ghi chú trước.";
        }

        if (lower.contains("tag") || lower.contains("phân loại")) {
            if (currentNote != null && currentNote.getContent() != null) {
                lastSuggestedTags = tagService.suggestTags(currentNote.getContent());
                return "🏷️ Gợi ý tags: " + String.join(", ",
                        lastSuggestedTags.stream().map(t -> "#" + t).toArray(String[]::new));
            }
            return "⚠️ Vui lòng chọn một ghi chú trước.";
        }

        return "🤔 Tôi có thể giúp bạn:\n\n" +
                "• Nhập \"tóm tắt\" để tóm tắt ghi chú\n" +
                "• Nhập \"tiêu đề\" để gợi ý tiêu đề\n" +
                "• Nhập \"tag\" để gợi ý tags\n\n" +
                "Hoặc nhấn các nút bên trên!";
    }

    private void addUserMessage(String message) {
        VBox messageBox = new VBox(5);
        messageBox.setAlignment(Pos.TOP_RIGHT);
        messageBox.setPadding(new Insets(0, 0, 0, 50));

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("chat-message-user");
        messageLabel.setMaxWidth(350);

        Label timeLabel = new Label(TIME_FORMAT.format(LocalDateTime.now()));
        timeLabel.getStyleClass().add("chat-time");

        HBox timeBox = new HBox(timeLabel);
        timeBox.setAlignment(Pos.CENTER_RIGHT);

        messageBox.getChildren().addAll(messageLabel, timeBox);
        chatContainer.getChildren().add(messageBox);
        scrollToBottom();
    }

    private void addAiMessage(String message) {
        VBox messageBox = new VBox(5);
        messageBox.setAlignment(Pos.TOP_LEFT);
        messageBox.setPadding(new Insets(0, 50, 0, 0));

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("chat-message-ai");
        messageLabel.setMaxWidth(380);

        Label timeLabel = new Label(TIME_FORMAT.format(LocalDateTime.now()));
        timeLabel.getStyleClass().add("chat-time");

        messageBox.getChildren().addAll(messageLabel, timeLabel);
        chatContainer.getChildren().add(messageBox);
        scrollToBottom();
    }

    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(show);
        }
        if (sendButton != null) {
            sendButton.setDisable(show);
        }
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatContainer.layout();
            if (chatScrollPane != null) {
                chatScrollPane.setVvalue(1.0);
            }
        });
    }

    private void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) chatContainer.getScene().getWindow();
        stage.close();
    }

    public String getLastSummary() {
        return lastSummary;
    }

    public String getLastSuggestedTitle() {
        return lastSuggestedTitle;
    }

    public List<String> getLastSuggestedTags() {
        return lastSuggestedTags;
    }
}
