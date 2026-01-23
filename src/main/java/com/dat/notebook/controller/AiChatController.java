package com.dat.notebook.controller;

import com.dat.notebook.model.Note;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AiChatController {

    @FXML private Label lblCurrentNoteTitle;
    @FXML private Label lblAiSummary;
    @FXML private VBox vboxChatMessages;
    @FXML private TextField txtAiPrompt;

    private Note currentNote;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        // Add welcome message
        addAiMessage("👋 Hi! I'm your AI assistant. I can help you analyze, summarize, and improve your notes. What would you like to do?");
    }

    public void setCurrentNote(Note note) {
        this.currentNote = note;
        if (note != null) {
            lblCurrentNoteTitle.setText(note.getTitle());
            generateSummary();
        }
    }

    private void generateSummary() {
        if (currentNote == null || currentNote.getContent() == null || currentNote.getContent().trim().isEmpty()) {
            lblAiSummary.setText("No content to summarize.");
            return;
        }

        String content = currentNote.getContent().trim();
        String summary;
        
        int firstPeriod = content.indexOf('.');
        if (firstPeriod > 0 && firstPeriod < 150) {
            summary = content.substring(0, firstPeriod + 1);
        } else if (content.length() > 120) {
            summary = content.substring(0, 120) + "...";
        } else {
            summary = content;
        }

        lblAiSummary.setText(summary);
    }

    @FXML
    private void handleExtractTasks() {
        if (currentNote == null) {
            addAiMessage("⚠️ Please select a note first.");
            return;
        }
        
        addUserMessage("Extract tasks from this note");
        
        // Simulate AI response
        String response = "📋 I found the following tasks in your note:\n\n" +
                "1. Sync with design team about mobile assets\n" +
                "2. Review Q4 roadmap\n" +
                "3. Investigate AI latency issues\n" +
                "4. Implement dark mode for mobile app\n\n" +
                "Would you like me to create a task list from these?";
        
        addAiMessage(response);
    }

    @FXML
    private void handleChangeTone() {
        if (currentNote == null) {
            addAiMessage("⚠️ Please select a note first.");
            return;
        }
        
        addUserMessage("Change the tone of this note");
        
        String response = "🎨 I can change the tone to:\n\n" +
                "• Professional\n" +
                "• Casual\n" +
                "• Formal\n" +
                "• Friendly\n\n" +
                "Which tone would you prefer?";
        
        addAiMessage(response);
    }

    @FXML
    private void handleImproveClarity() {
        if (currentNote == null) {
            addAiMessage("⚠️ Please select a note first.");
            return;
        }
        
        addUserMessage("Improve the clarity of this note");
        
        String response = "✨ I can help improve clarity by:\n\n" +
                "• Simplifying complex sentences\n" +
                "• Adding structure with headings\n" +
                "• Breaking down long paragraphs\n" +
                "• Making action items more explicit\n\n" +
                "Shall I proceed with these improvements?";
        
        addAiMessage(response);
    }

    @FXML
    private void handleGenerateSummary() {
        if (currentNote == null) {
            addAiMessage("⚠️ Please select a note first.");
            return;
        }
        
        addUserMessage("Generate a detailed summary");
        
        String response = "📝 Here's a comprehensive summary:\n\n" +
                "This note covers the Q4 roadmap discussion with key focus areas:\n\n" +
                "Main Points:\n" +
                "• UI refinement needed based on user feedback\n" +
                "• AI performance optimization (targeting <3s response time)\n" +
                "• Dark mode is high priority for mobile release\n\n" +
                "Next Steps: Coordinate with design team and schedule follow-up meeting.";
        
        addAiMessage(response);
    }

    @FXML
    private void handleSendMessage() {
        String message = txtAiPrompt.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        addUserMessage(message);
        txtAiPrompt.clear();

        // Simulate AI response
        String response = processUserMessage(message);
        addAiMessage(response);
    }

    private String processUserMessage(String message) {
        String lower = message.toLowerCase();
        
        if (lower.contains("summarize") || lower.contains("summary")) {
            return "📝 Based on your note, here's a quick summary:\n\n" +
                   "The note discusses Q4 planning with emphasis on UX improvements and AI optimization. " +
                   "Key action items include UI refinement and performance enhancements.";
        } else if (lower.contains("task") || lower.contains("action")) {
            return "✅ I found these action items:\n" +
                   "1. Refine UI based on feedback\n" +
                   "2. Optimize AI response time\n" +
                   "3. Implement dark mode";
        } else if (lower.contains("improve") || lower.contains("better")) {
            return "✨ I can help improve your note by:\n" +
                   "• Adding clear section headings\n" +
                   "• Highlighting key takeaways\n" +
                   "• Organizing action items\n" +
                   "Would you like me to apply these improvements?";
        } else {
            return "🤔 I understand you're asking: \"" + message + "\"\n\n" +
                   "I can help with:\n" +
                   "• Summarizing your notes\n" +
                   "• Extracting tasks\n" +
                   "• Improving clarity\n" +
                   "• Changing tone\n\n" +
                   "Try asking me to do one of these!";
        }
    }

    private void addUserMessage(String message) {
        VBox messageBox = new VBox(5);
        messageBox.setAlignment(Pos.TOP_RIGHT);
        messageBox.setPadding(new Insets(0, 0, 0, 50));

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("chat-message-user");
        messageLabel.setMaxWidth(400);

        Label timeLabel = new Label(TIME_FORMAT.format(LocalDateTime.now()));
        timeLabel.getStyleClass().add("chat-time");

        HBox timeBox = new HBox(timeLabel);
        timeBox.setAlignment(Pos.CENTER_RIGHT);

        messageBox.getChildren().addAll(messageLabel, timeBox);
        vboxChatMessages.getChildren().add(messageBox);

        // Scroll to bottom
        vboxChatMessages.layout();
    }

    private void addAiMessage(String message) {
        VBox messageBox = new VBox(5);
        messageBox.setAlignment(Pos.TOP_LEFT);
        messageBox.setPadding(new Insets(0, 50, 0, 0));

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("chat-message-ai");
        messageLabel.setMaxWidth(400);

        Label timeLabel = new Label(TIME_FORMAT.format(LocalDateTime.now()));
        timeLabel.getStyleClass().add("chat-time");

        messageBox.getChildren().addAll(messageLabel, timeLabel);
        vboxChatMessages.getChildren().add(messageBox);

        // Scroll to bottom
        vboxChatMessages.layout();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) txtAiPrompt.getScene().getWindow();
        stage.close();
    }
}
