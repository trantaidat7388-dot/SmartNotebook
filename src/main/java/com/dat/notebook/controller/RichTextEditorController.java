package com.dat.notebook.controller;

import com.dat.notebook.model.Note;
import com.dat.notebook.model.NoteVersion;
import com.dat.notebook.service.AuthService;
import com.dat.notebook.service.NoteService;
import com.dat.notebook.util.NoteVersionDAO;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Controller cho Rich Text Editor View
 * Quản lý multiple tabs, auto-save, version history, image insertion
 * 
 * Features:
 * - Tab-based note editing
 * - HTMLEditor với full formatting
 * - Auto-save sau 3 giây
 * - Version history
 * - Keyboard shortcuts
 * - Image insertion (Base64 embedded)
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class RichTextEditorController {
    
    // ==================== FXML CONTROLS ====================
    
    @FXML private TabPane noteTabPane;
    @FXML private Tab emptyTab;
    
    // Toolbar buttons
    @FXML private Button btnNewNote;
    @FXML private Button btnSaveNote;
    @FXML private Button btnVersionHistory;
    @FXML private Button btnInsertImage;
    
    @FXML private ColorPicker colorPickerText;
    @FXML private ColorPicker colorPickerBackground;
    
    // Status labels
    @FXML private Label lblAutoSaveStatus;
    @FXML private Label lblWordCount;
    @FXML private Label lblStatus;
    @FXML private Label lblCurrentNote;
    @FXML private Label lblLastSaved;
    @FXML private Label lblTabCount;
    
    // ==================== SERVICES & DATA ====================
    
    private NoteService noteService;
    private NoteVersionDAO versionDAO;
    private AuthService authService;
    
    // Tab management
    private Map<Tab, NoteEditorTab> tabMap = new HashMap<>();
    private int untitledCounter = 1;
    
    // Auto-save
    private ScheduledExecutorService autoSaveExecutor;
    private static final long AUTO_SAVE_DELAY_SECONDS = 3;
    
    // ==================== INITIALIZATION ====================
    
    public void initialize() {
        noteService = new NoteService();
        versionDAO = new NoteVersionDAO();
        authService = AuthService.getInstance();
        autoSaveExecutor = Executors.newSingleThreadScheduledExecutor();
        
        setupTabPane();
        setupKeyboardShortcuts();
        updateTabCount();
        
        System.out.println("RichTextEditorController initialized");
    }
    
    /**
     * Setup TabPane listeners
     */
    private void setupTabPane() {
        // Listen to tab selection changes
        noteTabPane.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldTab, newTab) -> onTabChanged(oldTab, newTab)
        );
        
        // Listen to tab list changes
        noteTabPane.getTabs().addListener((javafx.collections.ListChangeListener.Change<? extends Tab> change) -> {
            updateTabCount();
            
            // Hide empty tab if there are other tabs
            if (noteTabPane.getTabs().size() > 1 && noteTabPane.getTabs().contains(emptyTab)) {
                noteTabPane.getTabs().remove(emptyTab);
            } else if (noteTabPane.getTabs().isEmpty()) {
                noteTabPane.getTabs().add(emptyTab);
                noteTabPane.getSelectionModel().select(emptyTab);
            }
        });
    }
    
    /**
     * Setup keyboard shortcuts
     */
    private void setupKeyboardShortcuts() {
        noteTabPane.setOnKeyPressed(event -> {
            // Ctrl + N: New note
            if (new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN).match(event)) {
                handleNewNote();
                event.consume();
            }
            // Ctrl + S: Save
            else if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(event)) {
                handleSaveNote();
                event.consume();
            }
            // Ctrl + W: Close tab
            else if (new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN).match(event)) {
                Tab selectedTab = noteTabPane.getSelectionModel().getSelectedItem();
                if (selectedTab != null && selectedTab != emptyTab) {
                    closeTab(selectedTab);
                }
                event.consume();
            }
            // Ctrl + B, I, U: Formatting (handled by HTMLEditor)
        });
    }
    
    // ==================== TAB MANAGEMENT ====================
    
    /**
     * Tạo ghi chú mới trong tab mới
     */
    @FXML
    private void handleNewNote() {
        // Tạo Note object mới
        Note newNote = new Note();
        newNote.setTitle("Ghi chú mới " + untitledCounter++);
        newNote.setHtmlContent("<p>Bắt đầu viết ghi chú của bạn...</p>");
        newNote.setUserId(authService.getCurrentUserId());
        
        // Tạo tab mới
        Tab noteTab = createNoteTab(newNote, true);
        noteTabPane.getTabs().add(noteTab);
        noteTabPane.getSelectionModel().select(noteTab);
        
        updateStatus("Đã tạo ghi chú mới");
    }
    
    /**
     * Mở note đã có trong tab mới
     */
    public void openNote(Note note) {
        if (note == null) return;
        
        // Kiểm tra note đã mở chưa
        for (Map.Entry<Tab, NoteEditorTab> entry : tabMap.entrySet()) {
            if (entry.getValue().note.getId() == note.getId()) {
                noteTabPane.getSelectionModel().select(entry.getKey());
                updateStatus("Chuyển đến tab: " + note.getTitle());
                return;
            }
        }
        
        // Tạo tab mới cho note này
        Tab noteTab = createNoteTab(note, false);
        noteTabPane.getTabs().add(noteTab);
        noteTabPane.getSelectionModel().select(noteTab);
        
        updateStatus("Đã mở: " + note.getTitle());
    }
    
    /**
     * Tạo tab editor cho note
     */
    private Tab createNoteTab(Note note, boolean isNew) {
        Tab tab = new Tab();
        tab.setText(note.getTitle());
        
        // Create editor container
        VBox editorContainer = new VBox(10);
        editorContainer.setPadding(new Insets(10));
        editorContainer.setStyle("-fx-background-color: white;");
        
        // Title editor
        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        Label lblTitle = new Label("Tiêu đề:");
        lblTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        
        TextField txtTitle = new TextField(note.getTitle());
        txtTitle.setPromptText("Nhập tiêu đề ghi chú...");
        txtTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        HBox.setHgrow(txtTitle, Priority.ALWAYS);
        
        // Update tab text when title changes
        txtTitle.textProperty().addListener((obs, old, newVal) -> {
            tab.setText(newVal.isEmpty() ? "Chưa có tiêu đề" : newVal);
            note.setTitle(newVal);
            scheduleAutoSave(note);
        });
        
        titleBox.getChildren().addAll(lblTitle, txtTitle);
        
        // HTMLEditor
        HTMLEditor htmlEditor = new HTMLEditor();
        htmlEditor.setHtmlText(note.getHtmlContent());
        htmlEditor.setPrefHeight(500);
        VBox.setVgrow(htmlEditor, Priority.ALWAYS);
        
        // Listen to content changes for auto-save
        WebView webView = (WebView) htmlEditor.lookup(".web-view");
        if (webView != null) {
            WebEngine webEngine = webView.getEngine();
            webEngine.documentProperty().addListener((obs, old, newDoc) -> {
                if (newDoc != null) {
                    String htmlContent = htmlEditor.getHtmlText();
                    note.setHtmlContent(htmlContent);
                    scheduleAutoSave(note);
                    updateWordCount(note);
                }
            });
        }
        
        editorContainer.getChildren().addAll(titleBox, htmlEditor);
        tab.setContent(editorContainer);
        
        // Create NoteEditorTab wrapper
        NoteEditorTab editorTab = new NoteEditorTab(note, htmlEditor, txtTitle, isNew);
        tabMap.put(tab, editorTab);
        
        // Handle tab close request
        tab.setOnCloseRequest(event -> {
            if (!handleTabClose(tab)) {
                event.consume(); // Cancel close if user cancels
            }
        });
        
        return tab;
    }
    
    /**
     * Xử lý khi tab thay đổi
     */
    private void onTabChanged(Tab oldTab, Tab newTab) {
        if (newTab == null || newTab == emptyTab) {
            lblCurrentNote.setText("Chưa mở ghi chú nào");
            lblWordCount.setText("0 từ");
            return;
        }
        
        NoteEditorTab editorTab = tabMap.get(newTab);
        if (editorTab != null) {
            lblCurrentNote.setText(editorTab.note.getTitle());
            updateWordCount(editorTab.note);
        }
    }
    
    /**
     * Xử lý khi đóng tab
     */
    private boolean handleTabClose(Tab tab) {
        NoteEditorTab editorTab = tabMap.get(tab);
        if (editorTab == null) return true;
        
        // Nếu có thay đổi chưa lưu, hỏi user
        if (editorTab.hasUnsavedChanges) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Lưu thay đổi");
            alert.setHeaderText("Bạn có muốn lưu thay đổi trước khi đóng?");
            alert.setContentText(editorTab.note.getTitle());
            
            ButtonType btnSave = new ButtonType("Lưu");
            ButtonType btnDiscard = new ButtonType("Không lưu");
            ButtonType btnCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(btnSave, btnDiscard, btnCancel);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == btnSave) {
                    saveNote(editorTab.note, editorTab.htmlEditor);
                } else if (result.get() == btnCancel) {
                    return false; // Cancel close
                }
            }
        }
        
        // Cancel auto-save task if any
        if (editorTab.autoSaveTask != null) {
            editorTab.autoSaveTask.cancel(false);
        }
        
        tabMap.remove(tab);
        return true;
    }
    
    /**
     * Đóng tab
     */
    private void closeTab(Tab tab) {
        if (handleTabClose(tab)) {
            noteTabPane.getTabs().remove(tab);
        }
    }
    
    // ==================== AUTO-SAVE ====================
    
    /**
     * Schedule auto-save sau một khoảng thời gian
     */
    private void scheduleAutoSave(Note note) {
        Tab currentTab = noteTabPane.getSelectionModel().getSelectedItem();
        if (currentTab == null || currentTab == emptyTab) return;
        
        NoteEditorTab editorTab = tabMap.get(currentTab);
        if (editorTab == null || editorTab.note != note) return;
        
        // Cancel previous auto-save task
        if (editorTab.autoSaveTask != null) {
            editorTab.autoSaveTask.cancel(false);
        }
        
        // Mark as having unsaved changes
        editorTab.hasUnsavedChanges = true;
        updateAutoSaveStatus("Đang chỉnh sửa...", false);
        
        // Schedule new auto-save
        editorTab.autoSaveTask = autoSaveExecutor.schedule(() -> {
            Platform.runLater(() -> {
                if (autoSaveNote(note, editorTab.htmlEditor)) {
                    editorTab.hasUnsavedChanges = false;
                }
            });
        }, AUTO_SAVE_DELAY_SECONDS, TimeUnit.SECONDS);
    }
    
    /**
     * Auto-save note (silent save)
     */
    private boolean autoSaveNote(Note note, HTMLEditor editor) {
        try {
            // Get latest content
            String htmlContent = editor.getHtmlText();
            note.setHtmlContent(htmlContent);
            note.setUpdatedAt(LocalDateTime.now());
            
            boolean success;
            if (note.getId() <= 0) {
                // New note - insert
                Note savedNote = noteService.createNote(
                    note.getTitle(),
                    htmlContent,
                    note.getStatus(),
                    note.getCategoryId()
                );
                if (savedNote != null) {
                    note.setId(savedNote.getId());
                    success = true;
                } else {
                    success = false;
                }
            } else {
                // Existing note - update
                success = noteService.updateNote(note);
            }
            
            if (success) {
                updateAutoSaveStatus("✓ Đã tự động lưu", true);
                updateLastSaved();
                return true;
            } else {
                updateAutoSaveStatus("✗ Lỗi tự động lưu", false);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Auto-save error: " + e.getMessage());
            updateAutoSaveStatus("✗ Lỗi tự động lưu", false);
            return false;
        }
    }
    
    // ==================== MANUAL SAVE ====================
    
    /**
     * Lưu note hiện tại (manual save with version)
     */
    @FXML
    private void handleSaveNote() {
        Tab currentTab = noteTabPane.getSelectionModel().getSelectedItem();
        if (currentTab == null || currentTab == emptyTab) {
            showAlert("Không có ghi chú nào để lưu", Alert.AlertType.INFORMATION);
            return;
        }
        
        NoteEditorTab editorTab = tabMap.get(currentTab);
        if (editorTab == null) return;
        
        if (saveNote(editorTab.note, editorTab.htmlEditor)) {
            editorTab.hasUnsavedChanges = false;
            updateStatus("Đã lưu: " + editorTab.note.getTitle());
        }
    }
    
    /**
     * Save note và tạo version history
     */
    private boolean saveNote(Note note, HTMLEditor editor) {
        try {
            String htmlContent = editor.getHtmlText();
            note.setHtmlContent(htmlContent);
            note.setUpdatedAt(LocalDateTime.now());
            
            boolean success;
            if (note.getId() <= 0) {
                // New note
                Note savedNote = noteService.createNote(
                    note.getTitle(),
                    htmlContent,
                    note.getStatus(),
                    note.getCategoryId()
                );
                if (savedNote != null) {
                    note.setId(savedNote.getId());
                    success = true;
                } else {
                    success = false;
                }
            } else {
                // Update existing
                success = noteService.updateNote(note);
            }
            
            if (success) {
                // Tạo version history
                createVersionHistory(note, "Manual save");
                
                updateAutoSaveStatus("✓ Đã lưu", true);
                updateLastSaved();
                showAlert("Đã lưu thành công: " + note.getTitle(), Alert.AlertType.INFORMATION);
                return true;
            } else {
                showAlert("Lỗi khi lưu ghi chú", Alert.AlertType.ERROR);
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("Save error: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi: " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }
    }
    
    /**
     * Tạo version history
     */
    private void createVersionHistory(Note note, String description) {
        if (note.getId() <= 0) return;
        
        try {
            NoteVersion version = new NoteVersion();
            version.setNoteId(note.getId());
            version.setTitle(note.getTitle());
            version.setHtmlContent(note.getHtmlContent());
            version.setCreatedBy(authService.getCurrentUserId());
            version.setChangeDescription(description);
            
            versionDAO.createVersion(version);
            System.out.println("Created version " + version.getVersionNumber() + " for note " + note.getId());
            
        } catch (Exception e) {
            System.err.println("Error creating version: " + e.getMessage());
        }
    }
    
    // ==================== VERSION HISTORY ====================
    
    /**
     * Hiển thị lịch sử phiên bản
     */
    @FXML
    private void handleShowVersionHistory() {
        Tab currentTab = noteTabPane.getSelectionModel().getSelectedItem();
        if (currentTab == null || currentTab == emptyTab) {
            showAlert("Chọn một ghi chú để xem lịch sử", Alert.AlertType.INFORMATION);
            return;
        }
        
        NoteEditorTab editorTab = tabMap.get(currentTab);
        if (editorTab == null || editorTab.note.getId() <= 0) {
            showAlert("Lưu ghi chú trước khi xem lịch sử", Alert.AlertType.INFORMATION);
            return;
        }
        
        showVersionHistoryDialog(editorTab.note);
    }
    
    /**
     * Hiển thị dialog version history
     */
    private void showVersionHistoryDialog(Note note) {
        Stage dialog = new Stage();
        dialog.setTitle("Lịch sử phiên bản - " + note.getTitle());
        dialog.initModality(Modality.APPLICATION_MODAL);
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        Label title = new Label("Lịch sử chỉnh sửa");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        // ListView for versions
        ListView<NoteVersion> versionList = new ListView<>();
        versionList.setPrefHeight(300);
        
        // Load versions
        List<NoteVersion> versions = versionDAO.getVersionsByNoteId(note.getId());
        versionList.getItems().addAll(versions);
        
        // Custom cell factory
        versionList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(NoteVersion version, boolean empty) {
                super.updateItem(version, empty);
                if (empty || version == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox box = new VBox(3);
                    Label lbl1 = new Label("Version " + version.getVersionNumber() + " - " + 
                                          version.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    lbl1.setStyle("-fx-font-weight: bold;");
                    
                    Label lbl2 = new Label(version.getTitle());
                    Label lbl3 = new Label(version.hasChangeDescription() ? version.getChangeDescription() : "Tự động lưu");
                    lbl3.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                    
                    box.getChildren().addAll(lbl1, lbl2, lbl3);
                    setGraphic(box);
                }
            }
        });
        
        // Buttons
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnRestore = new Button("Khôi phục phiên bản này");
        btnRestore.setOnAction(e -> {
            NoteVersion selected = versionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                restoreVersion(note, selected);
                dialog.close();
            }
        });
        
        Button btnClose = new Button("Đóng");
        btnClose.setOnAction(e -> dialog.close());
        
        buttons.getChildren().addAll(btnRestore, btnClose);
        
        root.getChildren().addAll(title, versionList, buttons);
        
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 500, 400);
        dialog.setScene(scene);
        dialog.show();
    }
    
    /**
     * Khôi phục version cũ
     */
    private void restoreVersion(Note note, NoteVersion version) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Khôi phục phiên bản");
        confirm.setHeaderText("Bạn có chắc muốn khôi phục phiên bản này?");
        confirm.setContentText("Nội dung hiện tại sẽ bị thay thế bởi Version " + version.getVersionNumber());
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (versionDAO.rollbackToVersion(note.getId(), version.getVersionId(), authService.getCurrentUserId())) {
                // Reload note
                Optional<Note> reloadedNoteOpt = noteService.getNoteById(note.getId());
                if (reloadedNoteOpt.isPresent()) {
                    Note reloadedNote = reloadedNoteOpt.get();
                    note.setHtmlContent(reloadedNote.getHtmlContent());
                    note.setTitle(reloadedNote.getTitle());
                    
                    // Update UI
                    Tab currentTab = noteTabPane.getSelectionModel().getSelectedItem();
                    NoteEditorTab editorTab = tabMap.get(currentTab);
                    if (editorTab != null) {
                        editorTab.htmlEditor.setHtmlText(note.getHtmlContent());
                        editorTab.titleField.setText(note.getTitle());
                    }
                    
                    showAlert("Đã khôi phục Version " + version.getVersionNumber(), Alert.AlertType.INFORMATION);
                }
            } else {
                showAlert("Lỗi khi khôi phục phiên bản", Alert.AlertType.ERROR);
            }
        }
    }
    
    // ==================== FORMATTING ACTIONS ====================
    
    /**
     * Chèn ảnh từ file (Base64 embedded)
     */
    @FXML
    private void handleInsertImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Hình ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        File file = fileChooser.showOpenDialog(noteTabPane.getScene().getWindow());
        if (file != null) {
            try {
                // Read file to Base64
                byte[] fileContent = Files.readAllBytes(file.toPath());
                String base64 = Base64.getEncoder().encodeToString(fileContent);
                
                // Determine mime type
                String mimeType = Files.probeContentType(file.toPath());
                if (mimeType == null) {
                    mimeType = "image/png";
                }
                
                // Create data URL
                String dataUrl = "data:" + mimeType + ";base64," + base64;
                
                // Insert image
                String imgHtml = "<img src='" + dataUrl + "' style='max-width: 100%; height: auto;' />";
                executeJavaScript("document.execCommand('insertHTML', false, '" + imgHtml.replace("'", "\\'") + "');");
                
                updateStatus("Đã chèn ảnh: " + file.getName());
                
            } catch (IOException e) {
                System.err.println("Error inserting image: " + e.getMessage());
                showAlert("Lỗi khi chèn ảnh: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
    
    /**
     * Execute JavaScript trong HTMLEditor
     */
    private void executeJavaScript(String script) {
        Tab currentTab = noteTabPane.getSelectionModel().getSelectedItem();
        if (currentTab == null || currentTab == emptyTab) return;
        
        NoteEditorTab editorTab = tabMap.get(currentTab);
        if (editorTab == null) return;
        
        WebView webView = (WebView) editorTab.htmlEditor.lookup(".web-view");
        if (webView != null) {
            webView.getEngine().executeScript(script);
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private void updateTabCount() {
        int count = noteTabPane.getTabs().size();
        if (noteTabPane.getTabs().contains(emptyTab)) {
            count--;
        }
        lblTabCount.setText(count + " tab" + (count != 1 ? "s" : ""));
    }
    
    private void updateWordCount(Note note) {
        int wordCount = note.getWordCount();
        lblWordCount.setText(wordCount + " từ");
    }
    
    private void updateStatus(String message) {
        lblStatus.setText(message);
    }
    
    private void updateAutoSaveStatus(String message, boolean success) {
        lblAutoSaveStatus.setText(message);
        lblAutoSaveStatus.setStyle("-fx-text-fill: " + (success ? "#10b981" : "#ef4444") + "; -fx-font-size: 12px; -fx-padding: 8 0;");
    }
    
    private void updateLastSaved() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        lblLastSaved.setText("Lần lưu cuối: " + time);
    }
    
    private String toHexString(javafx.scene.paint.Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
    
    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.show();
    }
    
    /**
     * Cleanup when controller is destroyed
     */
    public void shutdown() {
        if (autoSaveExecutor != null) {
            autoSaveExecutor.shutdownNow();
        }
    }
    
    // ==================== INNER CLASS ====================
    
    /**
     * Wrapper class cho một note tab
     */
    private static class NoteEditorTab {
        Note note;
        HTMLEditor htmlEditor;
        TextField titleField;
        boolean isNew;
        boolean hasUnsavedChanges;
        ScheduledFuture<?> autoSaveTask;
        
        NoteEditorTab(Note note, HTMLEditor editor, TextField titleField, boolean isNew) {
            this.note = note;
            this.htmlEditor = editor;
            this.titleField = titleField;
            this.isNew = isNew;
            this.hasUnsavedChanges = isNew;
        }
    }
}
