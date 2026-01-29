package com.dat.notebook.controller;

import com.dat.notebook.model.Note;
import com.dat.notebook.model.User;
import com.dat.notebook.service.AuthService;
import com.dat.notebook.service.NoteServiceV2;
import com.dat.notebook.util.RichTextStyleManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import netscape.javascript.JSObject;
import javafx.concurrent.Worker;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * MainController V2 - Controller chính cho SmartNotebook
 * 
 * KIẾN TRÚC MVC:
 * - Controller CHỈ xử lý UI logic
 * - KHÔNG chứa SQL queries
 * - Gọi Service để xử lý business logic
 * 
 * TÍNH NĂNG:
 * - HTMLEditor cho Rich Text editing
 * - Auto-save với debounce 1.5 giây
 * - Keyboard shortcuts (Ctrl+S, Ctrl+B, Ctrl+I)
 * - Real-time save status indicator
 * 
 * @author SmartNotebook Team
 * @version 2.0
 */
public class MainControllerV2 {

    // ==================== FXML COMPONENTS ====================

    // Sidebar
    @FXML
    private TextField txtSearch;
    @FXML
    private Button btnAllNotes;
    @FXML
    private Button btnFavorites;
    @FXML
    private Button btnAIAssistant;
    @FXML
    private Button btnTrash;
    @FXML
    private Button btnFilterRegular;
    @FXML
    private Button btnFilterUrgent;
    @FXML
    private Button btnFilterIdeas;
    @FXML
    private Button btnFilterCompleted;
    @FXML
    private Label lblUsername;

    // Center Panel - Notes List
    @FXML
    private Label lblContentTitle;
    @FXML
    private Label lblNotesCount;
    @FXML
    private Button btnCreateNote;
    @FXML
    private ComboBox<String> cmbSort;
    @FXML
    private VBox vboxNotesList;

    // Right Panel - Editor
    @FXML
    private VBox editorPanel;
    @FXML
    private Label lblEditorTitle;
    @FXML
    private Label lblNoteDate;
    @FXML
    private Label lblSaveStatus;
    @FXML
    private TextField txtNoteTitle;
    @FXML
    private Label lblStatus;
    @FXML
    private HBox colorPicker;
    @FXML
    private HTMLEditor htmlEditor; // QUAN TRỌNG: HTMLEditor thay cho TextArea
    @FXML
    private Button btnFavorite;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnAiAssistant; // Nút robot để mở AI Assistant

    // ==================== SERVICES (MVC) ====================

    private final NoteServiceV2 noteService = new NoteServiceV2();
    private final AuthService authService = AuthService.getInstance();

    // ==================== STATE ====================

    private User currentUser;
    private ObservableList<Note> allNotes = FXCollections.observableArrayList();
    private Note selectedNote = null;
    private AIAssistantViewController currentAIController = null;
    private boolean isCreateMode = false;
    private String currentFilter = "ALL";
    private boolean showFavoritesOnly = false;
    private String currentSort = "NEWEST";
    private String selectedColor = "#ffffff";

    // ==================== EDITOR TOOLBAR ====================

    // ==================== AUTO-SAVE ====================

    private final ScheduledExecutorService autoSaveExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> autoSaveTask = null;
    private static final long AUTO_SAVE_DELAY_MS = 1500; // 1.5 giây
    private boolean hasUnsavedChanges = false;
    private String lastSavedContent = "";

    // ==================== FORMATTERS ====================

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ==================== COLORS ====================

    private static final String[] NOTE_COLORS = {
            "#ffffff", "#fef3c7", "#fed7e2", "#c3b1e1", "#bae6fd", "#d9f99d"
    };

    // ==================== INITIALIZATION ====================

    @FXML
    public void initialize() {
        System.out.println("MainControllerV2: Initializing...");

        // Get current user
        currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy thông tin người dùng!");
            return;
        }

        // Update UI với username
        if (lblUsername != null) {
            lblUsername.setText(currentUser.getUsername());
        }

        // Setup components
        setupSortComboBox();
        setupColorPicker();
        setupSearchListener();
        setupAutoSave();
        setupKeyboardShortcuts();
        setupButtonAnimations();

        // Load notes
        loadAllNotes();
        displayNotesList();

        // Set default active button
        setActiveNavButton(btnAllNotes);

        // Show empty editor state
        showEmptyEditorState();

        // Patch default toolbar buttons
        Platform.runLater(() -> {
            applyDefaultToolbarPatch();

            // Setup font protection observer
            if (htmlEditor != null) {
                WebView webView = (WebView) htmlEditor.lookup(".web-view");
                if (webView != null) {
                    // Wait for WebView to load
                    webView.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
                        if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                            RichTextStyleManager.setupFontProtection(webView);
                        }
                    });
                }
            }
        });

        System.out.println("MainControllerV2: Initialization complete");
    }

    // ==================== SETUP METHODS ====================

    /**
     * Setup sort combo box
     */
    private void setupSortComboBox() {
        if (cmbSort != null) {
            cmbSort.setItems(FXCollections.observableArrayList("Mới nhất", "Cũ nhất", "A-Z", "Z-A"));
            cmbSort.setValue("Mới nhất");
            cmbSort.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    switch (newVal) {
                        case "Mới nhất":
                            currentSort = "NEWEST";
                            break;
                        case "Cũ nhất":
                            currentSort = "OLDEST";
                            break;
                        case "A-Z":
                            currentSort = "A_Z";
                            break;
                        case "Z-A":
                            currentSort = "Z_A";
                            break;
                    }
                    displayNotesList();
                }
            });
        }
    }

    /**
     * Setup color picker buttons
     */
    private void setupColorPicker() {
        if (colorPicker != null) {
            colorPicker.getChildren().clear();
            for (String color : NOTE_COLORS) {
                Button btn = new Button();
                btn.setPrefSize(28, 28);
                btn.setStyle("-fx-background-color: " + color + "; " +
                        "-fx-background-radius: 14; " +
                        "-fx-border-color: " + (color.equals("#ffffff") ? "#d1d5db" : "transparent") + "; " +
                        "-fx-border-radius: 14; " +
                        "-fx-cursor: hand;");
                btn.setOnAction(e -> {
                    selectedColor = color;
                    updateColorPickerSelection();
                    if (selectedNote != null) {
                        selectedNote.setColor(color);
                        hasUnsavedChanges = true;
                        triggerAutoSave();
                    }
                });
                colorPicker.getChildren().add(btn);
            }
        }
    }

    /**
     * Update color picker visual selection
     */
    private void updateColorPickerSelection() {
        if (colorPicker == null)
            return;

        for (int i = 0; i < colorPicker.getChildren().size(); i++) {
            Button btn = (Button) colorPicker.getChildren().get(i);
            String color = NOTE_COLORS[i];
            String borderColor = color.equals(selectedColor) ? "#3b82f6"
                    : (color.equals("#ffffff") ? "#d1d5db" : "transparent");
            String borderWidth = color.equals(selectedColor) ? "3" : "1";
            btn.setStyle("-fx-background-color: " + color + "; " +
                    "-fx-background-radius: 14; " +
                    "-fx-border-color: " + borderColor + "; " +
                    "-fx-border-width: " + borderWidth + "; " +
                    "-fx-border-radius: 14; " +
                    "-fx-cursor: hand;");
        }
    }

    /**
     * Setup search listener với realtime filtering
     */
    private void setupSearchListener() {
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                displayNotesList();
            });
        }
    }

    /**
     * Setup auto-save với HTMLEditor listener
     * QUAN TRỌNG: HTMLEditor không có trực tiếp textProperty(),
     * nên phải dùng workaround với WebView
     */
    private void setupAutoSave() {
        if (htmlEditor != null) {
            // Lắng nghe thay đổi focus để trigger check
            htmlEditor.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused && selectedNote != null) {
                    checkForChangesAndSave();
                }
            });

            // Periodic check cho content changes (vì HTMLEditor không có event)
            ScheduledExecutorService contentChecker = Executors.newSingleThreadScheduledExecutor();
            contentChecker.scheduleAtFixedRate(() -> {
                Platform.runLater(this::checkForChangesAndSave);
            }, 2000, 1500, TimeUnit.MILLISECONDS);
        }

        // Title field listener
        if (txtNoteTitle != null) {
            txtNoteTitle.textProperty().addListener((obs, oldVal, newVal) -> {
                if (selectedNote != null && !isCreateMode) {
                    hasUnsavedChanges = true;
                    updateSaveStatus("Đang chỉnh sửa...", "#f59e0b");
                    triggerAutoSave();
                }
            });
        }
    }

    /**
     * Check if content changed and trigger auto-save
     */
    private void checkForChangesAndSave() {
        if (selectedNote == null || htmlEditor == null || isCreateMode)
            return;

        String currentContent = htmlEditor.getHtmlText();
        if (currentContent != null && !currentContent.equals(lastSavedContent)) {
            hasUnsavedChanges = true;
            updateSaveStatus("Đang chỉnh sửa...", "#f59e0b");
            triggerAutoSave();
        }
    }

    /**
     * Trigger auto-save với debounce
     */
    private void triggerAutoSave() {
        if (autoSaveTask != null && !autoSaveTask.isDone()) {
            autoSaveTask.cancel(false);
        }

        autoSaveTask = autoSaveExecutor.schedule(() -> {
            Platform.runLater(this::performAutoSave);
        }, AUTO_SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Perform actual auto-save
     */
    private void performAutoSave() {
        if (selectedNote == null || !hasUnsavedChanges)
            return;

        updateSaveStatus("Đang lưu...", "#3b82f6");

        try {
            // Get content from HTMLEditor
            String htmlContent = htmlEditor.getHtmlText();
            String title = txtNoteTitle.getText();

            // Update note object
            selectedNote.setTitle(title);
            selectedNote.setHtmlContent(htmlContent);
            selectedNote.setUpdatedAt(LocalDateTime.now());

            // Save via service
            boolean success = noteService.updateNote(selectedNote);

            if (success) {
                lastSavedContent = htmlContent;
                hasUnsavedChanges = false;
                updateSaveStatus("✔ Đã lưu", "#10b981");

                // Refresh notes list để update preview
                loadAllNotes();
                displayNotesList();
            } else {
                updateSaveStatus("⚠ Lỗi lưu", "#ef4444");
            }
        } catch (Exception e) {
            System.err.println("Auto-save error: " + e.getMessage());
            updateSaveStatus("⚠ Lỗi: " + e.getMessage(), "#ef4444");
        }
    }

    /**
     * Update save status label
     */
    private void updateSaveStatus(String text, String color) {
        Platform.runLater(() -> {
            if (lblSaveStatus != null) {
                lblSaveStatus.setText(text);
                lblSaveStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
        });
    }

    /**
     * Setup keyboard shortcuts
     */
    private void setupKeyboardShortcuts() {
        Platform.runLater(() -> {
            if (htmlEditor != null && htmlEditor.getScene() != null) {
                Scene scene = htmlEditor.getScene();

                // Ctrl+N = New Note
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
                        () -> handleCreateNote());

                // Ctrl+B = Bold (Safe)
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN),
                        () -> {
                            RichTextStyleManager.toggleBold((WebView) htmlEditor.lookup(".web-view"));
                        });

                // Ctrl+I = Italic (Safe)
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN),
                        () -> {
                            RichTextStyleManager.toggleItalic((WebView) htmlEditor.lookup(".web-view"));
                        });

                // Ctrl+U = Underline (Safe)
                scene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN),
                        () -> {
                            RichTextStyleManager.toggleUnderline((WebView) htmlEditor.lookup(".web-view"));
                        });

            }
        });
    }

    /**
     * Setup animations for buttons
     */
    private void setupButtonAnimations() {
        setupHoverAnimation(btnAllNotes);
        setupHoverAnimation(btnFavorites);
        setupHoverAnimation(btnTrash);
        setupHoverAnimation(btnCreateNote);
        setupHoverAnimation(btnAiAssistant); // Note: check variable name matching FXML
    }

    private void setupHoverAnimation(Button btn) {
        if (btn == null)
            return;

        // Scale Transition
        javafx.animation.ScaleTransition scaleIn = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(200), btn);
        scaleIn.setToX(1.05);
        scaleIn.setToY(1.05);

        javafx.animation.ScaleTransition scaleOut = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(200), btn);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);

        btn.setOnMouseEntered(e -> {
            scaleOut.stop();
            scaleIn.playFromStart();
        });

        btn.setOnMouseExited(e -> {
            scaleIn.stop();
            scaleOut.playFromStart();
        });
    }

    /**
     * Patch default toolbar buttons to fix Bold/Italic bugs
     */
    private void applyDefaultToolbarPatch() {
        if (htmlEditor == null)
            return;

        WebView webView = (WebView) htmlEditor.lookup(".web-view");
        if (webView == null)
            return;

        // Capture buttons references for the bridge
        ToggleButton boldBtn = null;
        ToggleButton italicBtn = null;
        ToggleButton underlineBtn = null;

        // 1. Override Toggle Buttons (Bold, Italic, Underline)
        Node boldNode = htmlEditor.lookup(".html-editor-bold");
        if (boldNode instanceof ToggleButton) {
            boldBtn = (ToggleButton) boldNode;
            boldBtn.setOnAction(e -> RichTextStyleManager.toggleBold(webView));
            boldBtn.setFocusTraversable(false);
        }

        Node italicNode = htmlEditor.lookup(".html-editor-italic");
        if (italicNode instanceof ToggleButton) {
            italicBtn = (ToggleButton) italicNode;
            italicBtn.setOnAction(e -> RichTextStyleManager.toggleItalic(webView));
            italicBtn.setFocusTraversable(false);
        }

        Node underlineNode = htmlEditor.lookup(".html-editor-underline");
        if (underlineNode instanceof ToggleButton) {
            underlineBtn = (ToggleButton) underlineNode;
            underlineBtn.setOnAction(e -> RichTextStyleManager.toggleUnderline(webView));
            underlineBtn.setFocusTraversable(false);
        }

        // 2. Override Font Family & Size ComboBoxes
        for (Node node : htmlEditor.lookupAll(".combo-box")) {
            if (node instanceof ComboBox) {
                ComboBox<?> combo = (ComboBox<?>) node;
                combo.setFocusTraversable(false);

                // Identify Font Family Picker vs Size Picker
                // Font picker usually has String items (Arial, etc.)
                // This is a heuristic, but typically effective for default HTMLEditor
                if (!combo.getItems().isEmpty() && combo.getItems().get(0) instanceof String) {

                    @SuppressWarnings("unchecked")
                    ComboBox<String> fontCombo = (ComboBox<String>) combo;

                    // HIJACK: Override action (This replaces default HTMLEditor listener)
                    fontCombo.setOnAction(e -> {
                        String selectedFont = fontCombo.getValue();
                        if (selectedFont != null) {
                            // Use SAFE method to preserve bold/italic
                            RichTextStyleManager.applyFontFamily(webView, selectedFont);

                            // Important: Force focus back
                            Platform.runLater(webView::requestFocus);
                        }
                    });
                }
            }
        }

        // 3. Setup JS Bridge to sync button states (Bold/Italic/Underline)
        if (boldBtn != null || italicBtn != null || underlineBtn != null) {
            final EditorBridge bridge = new EditorBridge(boldBtn, italicBtn, underlineBtn);

            // Listen for load completion to inject JS
            webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    try {
                        JSObject window = (JSObject) webView.getEngine().executeScript("window");
                        window.setMember("javaBridge", bridge);

                        // Listen to selection changes in JS
                        webView.getEngine().executeScript(
                                "document.addEventListener('selectionchange', function() {" +
                                        "    var b = document.queryCommandState('bold');" +
                                        "    var i = document.queryCommandState('italic');" +
                                        "    var u = document.queryCommandState('underline');" +
                                        "    if (window.javaBridge) window.javaBridge.updateState(b, i, u);" +
                                        "});");
                    } catch (Exception e) {
                        System.err.println("Error setting up JS Bridge: " + e.getMessage());
                    }
                }
            });
        }
    }

    // ==================== LOAD & DISPLAY ====================

    /**
     * Load all notes from service
     */
    private void loadAllNotes() {
        if (currentUser == null)
            return;

        try {
            List<Note> notes = noteService.getNotesByUser(currentUser.getId());
            allNotes.setAll(notes);
            System.out.println("Loaded " + notes.size() + " notes for user " + currentUser.getUsername());
        } catch (Exception e) {
            System.err.println("Error loading notes: " + e.getMessage());
            allNotes.clear();
        }
    }

    /**
     * Display notes in list với filtering và sorting
     */
    private void displayNotesList() {
        if (vboxNotesList == null)
            return;

        // Apply filters
        List<Note> filteredNotes = allNotes.stream()
                .filter(this::matchesSearchFilter)
                .filter(this::matchesStatusFilter)
                .filter(this::matchesFavoriteFilter)
                .sorted(getSortComparator())
                .collect(Collectors.toList());

        // Clear and rebuild list
        vboxNotesList.getChildren().clear();

        if (filteredNotes.isEmpty()) {
            showEmptyListState();
        } else {
            for (Note note : filteredNotes) {
                VBox noteCard = createNoteCard(note);
                vboxNotesList.getChildren().add(noteCard);
            }
        }

        // Update count
        updateNotesCount(filteredNotes.size());
    }

    /**
     * Create note card for list
     */
    private VBox createNoteCard(Note note) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("note-card");

        // Apply color
        String bgColor = note.getColor() != null ? note.getColor() : "#ffffff";
        card.setStyle("-fx-background-color: " + bgColor + "; " +
                "-fx-background-radius: 10; " +
                "-fx-border-radius: 10; " +
                "-fx-border-color: " + (isSelectedNote(note) ? "#3b82f6" : "#e5e7eb") + "; " +
                "-fx-border-width: " + (isSelectedNote(note) ? "2" : "1") + "; " +
                "-fx-cursor: hand;");

        // Header: Status + Favorite
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label(getStatusEmoji(note.getStatus()));
        statusLabel.setStyle("-fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label favIcon = new Label(note.isFavorite() ? "⭐" : "");
        favIcon.setStyle("-fx-font-size: 12px;");

        header.getChildren().addAll(statusLabel, spacer, favIcon);

        // Title
        Label title = new Label(note.getTitle() != null ? note.getTitle() : "Untitled");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1f2937;");
        title.setWrapText(true);

        // Preview
        String preview = getPreviewText(note);
        Label previewLabel = new Label(preview);
        previewLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        previewLabel.setWrapText(true);
        previewLabel.setMaxHeight(40);

        // Time
        String timeText = getTimeAgo(note.getUpdatedAt());
        Label timeLabel = new Label(timeText);
        timeLabel.setStyle("-fx-text-fill: #9ca3af; -fx-font-size: 11px;");

        card.getChildren().addAll(header, title, previewLabel, timeLabel);

        // Click handler
        card.setOnMouseClicked(e -> selectNote(note));

        // Hover effect
        card.setOnMouseEntered(e -> {
            if (!isSelectedNote(note)) {
                card.setStyle(card.getStyle().replace("-fx-border-color: #e5e7eb", "-fx-border-color: #93c5fd"));
            }
        });
        card.setOnMouseExited(e -> {
            if (!isSelectedNote(note)) {
                card.setStyle(card.getStyle().replace("-fx-border-color: #93c5fd", "-fx-border-color: #e5e7eb"));
            }
        });

        return card;
    }

    /**
     * Check if note is currently selected
     */
    private boolean isSelectedNote(Note note) {
        return selectedNote != null && selectedNote.getId() == note.getId();
    }

    /**
     * Get status emoji
     */
    private String getStatusEmoji(String status) {
        if (status == null)
            return "📝";
        switch (status) {
            case "URGENT":
                return "🔥 Khẩn cấp";
            case "IDEAS":
                return "💡 Ý tưởng";
            case "COMPLETED":
                return "✅ Hoàn thành";
            default:
                return "📝 Thường";
        }
    }

    /**
     * Get preview text from note
     */
    private String getPreviewText(Note note) {
        String content = note.getContent();
        if (content == null || content.isEmpty()) {
            // Try to extract from HTML
            String html = note.getHtmlContent();
            if (html != null) {
                content = html.replaceAll("<[^>]*>", " ")
                        .replaceAll("&nbsp;", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
            }
        }

        if (content == null || content.isEmpty()) {
            return "Chưa có nội dung...";
        }

        return content.length() > 80 ? content.substring(0, 80) + "..." : content;
    }

    /**
     * Get time ago string
     */
    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null)
            return "";

        long minutes = java.time.Duration.between(dateTime, LocalDateTime.now()).toMinutes();

        if (minutes < 1)
            return "Vừa xong";
        if (minutes < 60)
            return minutes + " phút trước";

        long hours = minutes / 60;
        if (hours < 24)
            return hours + " giờ trước";

        long days = hours / 24;
        if (days < 7)
            return days + " ngày trước";

        return DATE_FORMAT.format(dateTime);
    }

    /**
     * Show empty list state
     */
    private void showEmptyListState() {
        VBox emptyState = new VBox(15);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(40, 20, 40, 20));

        Label icon = new Label("📝");
        icon.setStyle("-fx-font-size: 48px;");

        Label text = new Label("Chưa có ghi chú nào");
        text.setStyle("-fx-font-size: 16px; -fx-text-fill: #6b7280;");

        Label hint = new Label("Nhấn '✨ Tạo ghi chú mới' để bắt đầu");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #9ca3af;");

        emptyState.getChildren().addAll(icon, text, hint);
        vboxNotesList.getChildren().add(emptyState);
    }

    /**
     * Show empty editor state
     */
    private void showEmptyEditorState() {
        isCreateMode = false;
        selectedNote = null;

        if (lblEditorTitle != null)
            lblEditorTitle.setText("Chọn ghi chú để xem");
        if (txtNoteTitle != null) {
            txtNoteTitle.clear();
            txtNoteTitle.setDisable(true);
        }
        if (htmlEditor != null) {
            htmlEditor.setHtmlText("<p style='color: #9ca3af; text-align: center; padding-top: 100px;'>" +
                    "Chọn một ghi chú từ danh sách bên trái<br>hoặc tạo ghi chú mới</p>");
            htmlEditor.setDisable(true);
        }
        if (lblNoteDate != null)
            lblNoteDate.setText("");
        if (lblSaveStatus != null)
            lblSaveStatus.setText("");
        if (lblStatus != null)
            lblStatus.setText("");
        if (btnFavorite != null) {
            btnFavorite.setText("☆");
            btnFavorite.getStyleClass().remove("favorite-active");
        }
        if (btnDelete != null)
            btnDelete.setDisable(true);
    }

    /**
     * Update notes count label
     */
    private void updateNotesCount(int count) {
        if (lblNotesCount != null) {
            lblNotesCount.setText(count + " ghi chú");
        }
    }

    // ==================== FILTERS ====================

    private boolean matchesSearchFilter(Note note) {
        if (txtSearch == null)
            return true;
        String search = txtSearch.getText();
        if (search == null || search.trim().isEmpty())
            return true;

        search = search.toLowerCase();
        String title = note.getTitle() != null ? note.getTitle().toLowerCase() : "";
        String content = note.getContent() != null ? note.getContent().toLowerCase() : "";

        return title.contains(search) || content.contains(search);
    }

    private boolean matchesStatusFilter(Note note) {
        if ("ALL".equals(currentFilter))
            return true;
        return note.getStatus() != null && note.getStatus().equals(currentFilter);
    }

    private boolean matchesFavoriteFilter(Note note) {
        if (!showFavoritesOnly)
            return true;
        return note.isFavorite();
    }

    private Comparator<Note> getSortComparator() {
        switch (currentSort) {
            case "OLDEST":
                return Comparator.comparing(Note::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "A_Z":
                return Comparator.comparing(n -> n.getTitle() != null ? n.getTitle().toLowerCase() : "");
            case "Z_A":
                return Comparator.comparing((Note n) -> n.getTitle() != null ? n.getTitle().toLowerCase() : "")
                        .reversed();
            default: // NEWEST
                return Comparator.comparing(Note::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
    }

    // ==================== NOTE SELECTION ====================

    /**
     * Select a note to edit
     */
    private void selectNote(Note note) {
        // Save current note if has changes
        if (selectedNote != null && hasUnsavedChanges) {
            performAutoSave();
        }

        selectedNote = note;
        isCreateMode = false;

        // Enable editor
        if (txtNoteTitle != null)
            txtNoteTitle.setDisable(false);
        if (htmlEditor != null)
            htmlEditor.setDisable(false);
        if (btnDelete != null)
            btnDelete.setDisable(false);

        // Display note content
        displayNoteInEditor(note);

        // Update AI view if it's currently visible
        if (currentAIController != null) {
            currentAIController.setNote(note);
        }

        // Refresh list to show selection
        displayNotesList();
    }

    /**
     * Display note in editor
     */
    private void displayNoteInEditor(Note note) {
        if (note == null)
            return;

        if (lblEditorTitle != null) {
            lblEditorTitle.setText("Soạn thảo ghi chú");
        }

        if (txtNoteTitle != null) {
            txtNoteTitle.setText(note.getTitle() != null ? note.getTitle() : "");
        }

        if (htmlEditor != null) {
            String html = note.getHtmlContent();
            if (html == null || html.trim().isEmpty()) {
                // Convert plain text to HTML nếu chưa có HTML
                String plainText = note.getContent();
                if (plainText != null && !plainText.isEmpty()) {
                    html = "<p>" + plainText.replace("\n", "</p><p>") + "</p>";
                } else {
                    html = "<p></p>";
                }
            }
            htmlEditor.setHtmlText(html);
            lastSavedContent = html;
        }

        if (lblNoteDate != null && note.getUpdatedAt() != null) {
            lblNoteDate.setText("Cập nhật: " + DATE_FORMAT.format(note.getUpdatedAt()));
        }

        if (lblStatus != null) {
            lblStatus.setText(note.getStatus() != null ? note.getStatus() : "REGULAR");
            lblStatus.getStyleClass().removeAll("status-urgent", "status-ideas", "status-completed", "status-regular");
            lblStatus.getStyleClass()
                    .add("status-" + (note.getStatus() != null ? note.getStatus().toLowerCase() : "regular"));
        }

        if (btnFavorite != null) {
            btnFavorite.setText(note.isFavorite() ? "⭐" : "☆");
            if (note.isFavorite()) {
                if (!btnFavorite.getStyleClass().contains("favorite-active")) {
                    btnFavorite.getStyleClass().add("favorite-active");
                }
            } else {
                btnFavorite.getStyleClass().remove("favorite-active");
            }
        }

        // Update color selection
        selectedColor = note.getColor() != null ? note.getColor() : "#ffffff";
        updateColorPickerSelection();

        updateSaveStatus("", "");
        hasUnsavedChanges = false;
    }

    // ==================== EVENT HANDLERS - SIDEBAR ====================

    @FXML
    private void handleShowAllNotes() {
        currentFilter = "ALL";
        showFavoritesOnly = false;
        lblContentTitle.setText("Tất cả ghi chú");
        setActiveNavButton(btnAllNotes);
        displayNotesList();

        // Show editor panel if AI view is showing
        showEditorPanel();
    }

    @FXML
    private void handleShowFavorites() {
        System.out.println("=== handleShowFavorites called ===");
        System.out.println("btnFavorites is null? " + (btnFavorites == null));
        currentFilter = "ALL";
        showFavoritesOnly = true;
        lblContentTitle.setText("Yêu thích");
        setActiveNavButton(btnFavorites);
        displayNotesList();

        // Show editor panel if AI view is showing
        showEditorPanel();
    }

    @FXML
    private void handleShowTrash() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/TrashView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnTrash.getScene().getWindow();

            // Thay thế root của scene hiện tại thay vì tạo scene mới
            // Điều này giữ nguyên maximize state
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở thùng rác: " + e.getMessage());
        }
    }

    @FXML
    private void handleFilterRegular() {
        currentFilter = "REGULAR";
        showFavoritesOnly = false;
        lblContentTitle.setText("Ghi chú thường");
        displayNotesList();
    }

    @FXML
    private void handleFilterUrgent() {
        currentFilter = "URGENT";
        showFavoritesOnly = false;
        lblContentTitle.setText("Ghi chú khẩn cấp");
        displayNotesList();
    }

    @FXML
    private void handleFilterIdeas() {
        currentFilter = "IDEAS";
        showFavoritesOnly = false;
        lblContentTitle.setText("Ý tưởng");
        displayNotesList();
    }

    @FXML
    private void handleFilterCompleted() {
        currentFilter = "COMPLETED";
        showFavoritesOnly = false;
        lblContentTitle.setText("Đã hoàn thành");
        displayNotesList();
    }

    // Default and active background styles
    private static final String DEFAULT_BTN_STYLE = "-fx-background-color: #FFE0B2; -fx-text-fill: #3E2723; -fx-background-radius: 12; -fx-border-color: #FFB74D; -fx-border-width: 1; -fx-border-radius: 12;";
    private static final String ACTIVE_BTN_STYLE = "-fx-background-color: #FFA000; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #F57C00; -fx-border-width: 0 0 0 4; -fx-background-radius: 0 12 12 0;";

    private void setActiveNavButton(Button activeBtn) {
        System.out.println("=== setActiveNavButton called ===");
        System.out.println("Active button: " + (activeBtn != null ? activeBtn.getText() : "null"));

        Button[] allButtons = new Button[] { btnAllNotes, btnFavorites, btnAIAssistant, btnTrash };

        // Reset all buttons to default style logic
        for (Button btn : allButtons) {
            if (btn != null) {
                // Remove active class
                btn.getStyleClass().remove("sidebar-btn-active");

                // Ensure default class exists
                if (!btn.getStyleClass().contains("sidebar-btn")) {
                    btn.getStyleClass().add("sidebar-btn");
                }

                // Remove any inline styles that might interfere
                btn.setStyle("");
            }
        }

        // Apply active style to selected button
        if (activeBtn != null) {
            if (!activeBtn.getStyleClass().contains("sidebar-btn-active")) {
                activeBtn.getStyleClass().add("sidebar-btn-active");
            }
            System.out.println("Applied active style to: " + activeBtn.getText());
        }
    }

    /**
     * Show editor panel and hide AI view
     */
    private void showEditorPanel() {
        HBox parent = (HBox) editorPanel.getParent();

        // Remove AI view if exists (only remove nodes with ai-view-panel class)
        parent.getChildren().removeIf(node -> node instanceof VBox && node.getStyleClass().contains("ai-view-panel"));

        // Show editor panel
        editorPanel.setVisible(true);
        editorPanel.setManaged(true);

        // Clear AI controller reference
        currentAIController = null;
        // NOTE: Do NOT reset active button state here - it should be controlled by
        // handleShowAllNotes/handleShowFavorites
    }

    /**
     * Update sidebar button states
     */
    private void updateSidebarButtonStates(Button activeBtn) {
        setActiveNavButton(activeBtn);
    }

    // ==================== EVENT HANDLERS - NOTES ====================

    /**
     * Create new note
     */
    @FXML
    private void handleCreateNote() {
        // Save current if has changes
        if (selectedNote != null && hasUnsavedChanges) {
            performAutoSave();
        }

        isCreateMode = true;
        selectedNote = null;

        // Enable editor
        if (txtNoteTitle != null) {
            txtNoteTitle.setDisable(false);
            txtNoteTitle.clear();
            txtNoteTitle.setPromptText("Nhập tiêu đề ghi chú mới...");
        }

        if (htmlEditor != null) {
            htmlEditor.setDisable(false);
            htmlEditor.setHtmlText("");
        }

        if (lblEditorTitle != null) {
            lblEditorTitle.setText("✨ Tạo ghi chú mới");
        }

        if (lblNoteDate != null)
            lblNoteDate.setText("");
        if (lblSaveStatus != null)
            lblSaveStatus.setText("");
        if (lblStatus != null)
            lblStatus.setText("REGULAR");
        if (btnFavorite != null) {
            btnFavorite.setText("☆");
            btnFavorite.getStyleClass().remove("favorite-active");
        }
        if (btnDelete != null)
            btnDelete.setDisable(true);

        selectedColor = "#ffffff";
        updateColorPickerSelection();

        // Focus title
        Platform.runLater(() -> {
            if (txtNoteTitle != null)
                txtNoteTitle.requestFocus();
        });
    }

    /**
     * Save note (manual or auto)
     */
    @FXML
    private void handleSaveNote() {
        if (isCreateMode) {
            // Create new note
            String title = txtNoteTitle.getText().trim();
            if (title.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập tiêu đề!");
                return;
            }

            String htmlContent = htmlEditor.getHtmlText();

            Note newNote = new Note();
            newNote.setTitle(title);
            newNote.setHtmlContent(htmlContent);
            newNote.setStatus("REGULAR");
            newNote.setColor(selectedColor);
            newNote.setFavorite(false);

            Note created = noteService.createNote(newNote);

            if (created != null) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Ghi chú \"" + title + "\" đã được tạo!");

                isCreateMode = false;
                loadAllNotes();
                displayNotesList();
                selectNote(created);
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo ghi chú!");
            }
        } else if (selectedNote != null) {
            // Update existing note
            performAutoSave();
        }
    }

    /**
     * Delete current note
     */
    @FXML
    private void handleDeleteNote() {
        if (selectedNote == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa ghi chú?");
        confirm.setContentText("Ghi chú \"" + selectedNote.getTitle() + "\" sẽ được chuyển vào thùng rác.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = noteService.deleteNote(selectedNote.getId());

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Ghi chú đã được chuyển vào thùng rác!");

                selectedNote = null;
                loadAllNotes();
                displayNotesList();
                showEmptyEditorState();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa ghi chú!");
            }
        }
    }

    /**
     * Toggle favorite status
     */
    @FXML
    private void handleToggleFavorite() {
        if (selectedNote == null)
            return;

        boolean newState = !selectedNote.isFavorite();
        selectedNote.setFavorite(newState);

        boolean success = noteService.updateNote(selectedNote);

        if (success) {
            boolean isFav = newState;
            btnFavorite.setText(isFav ? "⭐" : "☆");
            if (isFav) {
                if (!btnFavorite.getStyleClass().contains("favorite-active")) {
                    btnFavorite.getStyleClass().add("favorite-active");
                }
            } else {
                btnFavorite.getStyleClass().remove("favorite-active");
            }
            loadAllNotes();
            displayNotesList();
        } else {
            selectedNote.setFavorite(!newState); // Revert
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật!");
        }
    }

    /**
     * Change note status
     */
    @FXML
    private void handleChangeStatus() {
        if (selectedNote == null)
            return;

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                selectedNote.getStatus(),
                "REGULAR", "URGENT", "IDEAS", "COMPLETED");
        dialog.setTitle("Đổi trạng thái");
        dialog.setHeaderText("Chọn trạng thái mới");
        dialog.setContentText("Trạng thái:");

        dialog.showAndWait().ifPresent(status -> {
            selectedNote.setStatus(status);
            boolean success = noteService.updateNote(selectedNote);

            if (success) {
                lblStatus.setText(status);
                loadAllNotes();
                displayNotesList();
            }
        });
    }

    /**
     * Copy content to clipboard
     */
    @FXML
    private void handleCopyContent() {
        if (htmlEditor == null)
            return;

        String content = htmlEditor.getHtmlText()
                .replaceAll("<[^>]*>", "")
                .replaceAll("&nbsp;", " ");

        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent clipContent = new javafx.scene.input.ClipboardContent();
        clipContent.putString(content);
        clipboard.setContent(clipContent);

        updateSaveStatus("📋 Đã sao chép!", "#10b981");
    }

    /**
     * Export note to HTML file
     */
    @FXML
    private void handleExportNote() {
        if (selectedNote == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn ghi chú để xuất!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Xuất ghi chú");
        fileChooser.setInitialFileName(selectedNote.getTitle() + ".html");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("HTML Files", "*.html"));

        File file = fileChooser.showSaveDialog(htmlEditor.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                String html = "<!DOCTYPE html><html><head>" +
                        "<meta charset='UTF-8'>" +
                        "<title>" + selectedNote.getTitle() + "</title>" +
                        "<style>body{font-family:Arial,sans-serif;max-width:800px;margin:0 auto;padding:20px;}</style>"
                        +
                        "</head><body>" +
                        "<h1>" + selectedNote.getTitle() + "</h1>" +
                        htmlEditor.getHtmlText() +
                        "</body></html>";
                writer.write(html);

                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Ghi chú đã được xuất thành công!");
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xuất file: " + e.getMessage());
            }
        }
    }

    // ==================== EVENT HANDLERS - AI ASSISTANT ====================

    @FXML
    private void handleShowAIAssistant() {
        try {
            // Load AI Assistant View
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AIAssistantView.fxml"));
            VBox aiView = loader.load();
            AIAssistantViewController aiController = loader.getController();

            // Store controller reference
            currentAIController = aiController;

            // Set back callback
            aiController.setOnBack(() -> {
                showEditorPanel();
                currentAIController = null;
            });

            // Pass current note to AI Assistant
            if (selectedNote != null) {
                aiController.setNote(selectedNote);
            }

            // Replace editor panel with AI view
            HBox parent = (HBox) editorPanel.getParent();
            int index = parent.getChildren().indexOf(editorPanel);

            editorPanel.setVisible(false);
            editorPanel.setManaged(false);

            aiView.setMinWidth(450);
            HBox.setHgrow(aiView, Priority.ALWAYS);

            parent.getChildren().add(index + 1, aiView);

            // Update button states
            updateSidebarButtonStates(btnAIAssistant);

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Lỗi khi mở AI Assistant: " + e.getMessage());
        }
    }

    /**
     * Show error alert
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== EVENT HANDLERS - SETTINGS ====================

    @FXML
    private void handleShowSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SettingsView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Cài đặt - SmartNotebook");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            // Refresh after settings changed
            loadAllNotes();
            displayNotesList();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở cài đặt: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText("Bạn có chắc muốn đăng xuất?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Save any pending changes
            if (hasUnsavedChanges) {
                performAutoSave();
            }

            // Logout
            authService.logout();

            // Return to login
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) btnLogout.getScene().getWindow();

                // Reset window state
                stage.setMaximized(false);
                stage.setScene(new Scene(root));
                stage.setTitle("SmartNotebook - Đăng nhập");

                // Set fixed size cho login
                stage.setWidth(1000);
                stage.setHeight(650);
                stage.setResizable(true);

                stage.centerOnScreen();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đăng xuất: " + e.getMessage());
            }
        }
    }

    // ==================== AI ASSISTANT ====================

    /**
     * Mở cửa sổ AI Assistant
     */
    @FXML
    private Button btnLogout;

    // ==================== UTILITY ====================

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Cleanup when controller is destroyed
     */
    public void cleanup() {
        if (autoSaveExecutor != null && !autoSaveExecutor.isShutdown()) {
            // Save pending changes
            if (hasUnsavedChanges) {
                performAutoSave();
            }

            autoSaveExecutor.shutdown();
            try {
                if (!autoSaveExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    autoSaveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                autoSaveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Bridge class to receive callbacks from JavaScript
     * Must be public for JSObject to access
     */
    public static class EditorBridge {
        private final ToggleButton boldBtn;
        private final ToggleButton italicBtn;
        private final ToggleButton underlineBtn;

        public EditorBridge(ToggleButton bold, ToggleButton italic, ToggleButton underline) {
            this.boldBtn = bold;
            this.italicBtn = italic;
            this.underlineBtn = underline;
        }

        public void updateState(boolean bold, boolean italic, boolean underline) {
            Platform.runLater(() -> {
                if (boldBtn != null)
                    boldBtn.setSelected(bold);
                if (italicBtn != null)
                    italicBtn.setSelected(italic);
                if (underlineBtn != null)
                    underlineBtn.setSelected(underline);
            });
        }
    }

}
