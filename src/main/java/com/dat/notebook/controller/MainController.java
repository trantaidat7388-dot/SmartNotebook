package com.dat.notebook.controller;

import com.dat.notebook.model.Note;
import com.dat.notebook.model.User;
import com.dat.notebook.service.AuthService;
import com.dat.notebook.service.NoteService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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

public class MainController {

    // ===== LEFT SIDEBAR CONTROLS =====
    @FXML
    private TextField txtSearch;
    @FXML
    private Button btnAllNotes;
    @FXML
    private Button btnFavorites;
    @FXML
    private Button btnShared;
    @FXML
    private Button btnTrash;
    @FXML
    private Button btnProductLaunch;
    @FXML
    private Button btnClientSyncs;

    // ===== MIDDLE PANEL CONTROLS =====
    @FXML
    private Button btnFilterAll;
    @FXML
    private Button btnFilterRegular;
    @FXML
    private Button btnFilterDone;
    @FXML
    private Button btnFilterUrgent;
    @FXML
    private Button btnFilterIdeas;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private VBox vboxNotesList;
    @FXML
    private javafx.scene.layout.FlowPane notesContainer;
    @FXML
    private Label lblNotesCount;
    @FXML
    private Label noteCountLabel;

    // ===== RIGHT PANEL CONTROLS =====
    @FXML
    private Label lblNoteTitle;
    @FXML
    private Label lblNoteDate;
    @FXML
    private Label lblNoteTime;
    @FXML
    private Label lblNoteStatus;
    @FXML
    private TextArea txtContent;
    @FXML
    private Button btnFavoriteNote;
    @FXML
    private Button btnDeleteNote;
    @FXML
    private Button btnEditTitle;
    @FXML
    private VBox editorPanel;

    // ===== DATA =====
    private NoteService noteService = new NoteService();
    private AuthService authService = AuthService.getInstance();
    private User currentUser;
    private ObservableList<Note> allNotes = FXCollections.observableArrayList();
    private Note selectedNote = null;
    private String currentFilter = "ALL"; // ALL, REGULAR, URGENT, IDEAS, COMPLETED
    private boolean showFavoritesOnly = false;
    private String currentSort = "NEWEST"; // NEWEST, OLDEST

    // ===== CREATE MODE CONTROLS =====
    private boolean isCreateMode = false;
    private TextField txtNewTitle;
    private CheckBox chkNewFavorite;
    private String selectedColor = "#a8edea"; // Default mint color

    // Auto-save debounce
    private final ScheduledExecutorService autoSaveExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> autoSaveTask = null;
    private static final long AUTO_SAVE_DELAY_MS = 1000; // 1 second delay

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    @FXML
    public void initialize() {
        // Lấy current user từ AuthService
        currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            showAlert("Lỗi", "Không tìm thấy thông tin người dùng. Vui lòng đăng nhập lại.");
            return;
        }

        setupEventHandlers();
        loadAllNotes();

        // Only load UI if elements exist
        if (vboxNotesList != null || notesContainer != null) {
            System.out.println("initialize: vboxNotesList=" + vboxNotesList + ", notesContainer=" + notesContainer);
            loadNotesIntoList();

            // Select first note by default if available AND editor exists
            if (!allNotes.isEmpty() && selectedNote == null && lblNoteTitle != null) {
                selectNote(allNotes.get(0));
            }
        } else {
            System.err.println("initialize: Both vboxNotesList and notesContainer are NULL!");
        }
    }

    // ===== SETUP =====

    /**
     * Thiết lập các event handlers cho UI controls
     * Bao gồm: search listener, auto-save với debounce, sort listener
     */
    private void setupEventHandlers() {
        // ===== Search functionality =====
        // Tìm kiếm realtime khi user nhập vào search field
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
                filterAndDisplayNotes();
            });
        }

        // ===== Auto-save với debounce =====
        // Chỉ save sau khi user ngừng gõ 1 giây để tránh save liên tục
        if (txtContent != null) {
            txtContent.textProperty().addListener((obs, oldVal, newVal) -> {
                if (selectedNote != null) {
                    // Update content in memory
                    selectedNote.setContent(newVal);

                    // Cancel previous auto-save task nếu có
                    if (autoSaveTask != null && !autoSaveTask.isDone()) {
                        autoSaveTask.cancel(false);
                    }

                    // Schedule new auto-save task với delay 1 giây
                    autoSaveTask = autoSaveExecutor.schedule(() -> {
                        javafx.application.Platform.runLater(this::autoSaveNote);
                    }, AUTO_SAVE_DELAY_MS, TimeUnit.MILLISECONDS);
                }
            });
        }

        // ===== Sort ComboBox =====
        // Thiết lập sort options và listener
        if (sortComboBox != null) {
            sortComboBox.setItems(FXCollections.observableArrayList("Mới nhất", "Cũ nhất"));
            sortComboBox.setValue("Mới nhất"); // Default

            sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    currentSort = newVal.equals("Mới nhất") ? "NEWEST" : "OLDEST";
                    filterAndDisplayNotes();
                }
            });
        }
    }

    /**
     * Load tất cả ghi chú của user từ database
     * 
     * Flow:
     * 1. Query notes từ NoteService
     * 2. Nếu không có notes: Tạo sample notes (chỉ cho demo)
     * 3. Update allNotes ObservableList
     * 
     * NOTE: createSampleNotes() chỉ để demo, có thể xóa trong production
     */
    private void loadAllNotes() {
        if (currentUser == null) {
            System.err.println("loadAllNotes: currentUser is NULL!");
            return;
        }

        System.out.println("loadAllNotes: Loading notes for user ID = " + currentUser.getId() + " ("
                + currentUser.getUsername() + ")");

        try {
            List<Note> notes = noteService.getNotesByUser(currentUser.getId());
            System.out.println("loadAllNotes: Found " + notes.size() + " notes from database");

            // ===== DEMO ONLY: Create sample notes =====
            // TODO: Remove this in production - users should create their own notes
            if (notes.isEmpty()) {
                System.out.println("loadAllNotes: No notes found, creating sample notes...");
                createSampleNotes();
                notes = noteService.getNotesByUser(currentUser.getId());
                System.out.println("loadAllNotes: After creating samples, found " + notes.size() + " notes");
            }

            allNotes.setAll(notes);
            System.out.println("loadAllNotes: allNotes now has " + allNotes.size() + " items");
        } catch (Exception e) {
            System.err.println("Lỗi khi tải ghi chú: " + e.getMessage());
            e.printStackTrace();
            allNotes.clear();
        }
    }

    /**
     * Tạo sample notes để demo
     * 
     * NOTE: Chỉ dùng để demo, nên xóa hoặc comment trong production
     * Users nên tự tạo notes thông qua UI thay vì hardcode
     */
    private void createSampleNotes() {
        if (currentUser == null)
            return;

        // Create sample notes with different statuses - không set CategoryID để tránh
        // FK error
        Note note1 = new Note();
        note1.setUserId(currentUser.getId());
        note1.setTitle("Q4 Roadmap Finalization");
        note1.setContent(
                "Need to sync with the design team regarding the final assets for the mobile release...\n\n📋 Tasks:\n• Review design mockups\n• Schedule meeting with team\n• Prepare presentation slides");
        note1.setStatus("URGENT");
        note1.setCategoryId(null); // Không set category
        note1.setFavorite(true);
        note1.setColor("#fce7f3");
        note1.setCreatedAt(LocalDateTime.now().minusDays(2));
        note1.setUpdatedAt(LocalDateTime.now().minusDays(2));
        noteService.createNote(note1);

        Note note2 = new Note();
        note2.setUserId(currentUser.getId());
        note2.setTitle("Weekly Sync Notes");
        note2.setContent(
                "Discussed the roadmap for Q4 and identified key performance metrics for the next sprint...\n\n📝 Key Takeaways:\n• Refine the UI: Users mentioned the dashboard feels a bit cluttered.\n• AI Latency: Investigating why the summarization tool takes more than 3 seconds on longer documents.\n• Dark Mode Support: High priority for the mobile app release next month.\n\n✅ Action Items:\n☐ Schedule follow-up with design team\n☐ Create performance benchmark tests");
        note2.setStatus("REGULAR");
        note2.setCategoryId(null);
        note2.setColor("#fef3c7");
        note2.setCreatedAt(LocalDateTime.now().minusHours(10));
        note2.setUpdatedAt(LocalDateTime.now().minusHours(10));
        noteService.createNote(note2);

        Note note3 = new Note();
        note3.setUserId(currentUser.getId());
        note3.setTitle("AI Integration Concepts");
        note3.setContent(
                "Exploring how we can use natural language processing to automatically categorize incoming notes...\n\n💡 Ideas:\n• Auto-tagging based on content analysis\n• Smart search with semantic understanding\n• Sentiment analysis for meeting notes\n• Automatic summary generation");
        note3.setStatus("IDEAS");
        note3.setCategoryId(null);
        note3.setFavorite(true);
        note3.setColor("#ede9fe");
        note3.setCreatedAt(LocalDateTime.now().minusDays(1));
        note3.setUpdatedAt(LocalDateTime.now().minusDays(1));
        noteService.createNote(note3);

        Note note4 = new Note();
        note4.setUserId(currentUser.getId());
        note4.setTitle("Landing Page Copy");
        note4.setContent(
                "Finalized the headings for the home page and the features section...\n\n✅ Completed:\n• Hero section copy\n• Feature descriptions\n• Call-to-action buttons\n• Footer content");
        note4.setStatus("COMPLETED");
        note4.setCategoryId(null);
        note4.setColor("#d1fae5");
        note4.setCreatedAt(LocalDateTime.now().minusDays(7));
        note4.setUpdatedAt(LocalDateTime.now().minusDays(7));
        noteService.createNote(note4);
    }

    // ===== NOTES LIST DISPLAY =====

    /**
     * Lọc và hiển thị danh sách ghi chú
     * - Lọc theo: search text, status, favorite, category
     * - Sắp xếp theo: newest/oldest
     */
    private void filterAndDisplayNotes() {
        System.out.println("filterAndDisplayNotes: START with " + allNotes.size() + " total notes");
        System.out.println("filterAndDisplayNotes: currentFilter=" + currentFilter + ", showFavoritesOnly="
                + showFavoritesOnly + ", currentSort=" + currentSort);

        List<Note> filteredNotes = allNotes.stream()
                .filter(note -> matchesSearchFilter(note))
                .filter(note -> matchesStatusFilter(note))
                .filter(note -> matchesFavoriteFilter(note))
                .filter(note -> matchesCategoryFilter(note))
                .sorted(getSortComparator()) // Apply sorting
                .collect(Collectors.toList());

        System.out.println("filterAndDisplayNotes: After filtering, " + filteredNotes.size() + " notes remain");
        displayNotesInList(filteredNotes);
        updateNotesCount();
    }

    /**
     * Lấy Comparator cho sorting dựa trên currentSort
     * 
     * @return Comparator để sắp xếp notes
     */
    private Comparator<Note> getSortComparator() {
        if ("OLDEST".equals(currentSort)) {
            // Cũ nhất: Sort by UpdatedAt ascending (earliest first)
            return Comparator.comparing(Note::getUpdatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        } else {
            // Mới nhất (default): Sort by UpdatedAt descending (latest first)
            return Comparator.comparing(Note::getUpdatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        }
    }

    /**
     * Kiểm tra note có khớp với search text không
     * Tìm kiếm trong: title và content
     */
    private boolean matchesSearchFilter(Note note) {
        if (txtSearch == null) {
            return true;
        }
        String search = txtSearch.getText();
        if (search == null || search.trim().isEmpty()) {
            return true;
        }
        search = search.toLowerCase();
        return note.getTitle().toLowerCase().contains(search) ||
                note.getContent().toLowerCase().contains(search);
    }

    /**
     * Kiểm tra note có khớp với status filter không
     */
    private boolean matchesStatusFilter(Note note) {
        if (currentFilter.equals("ALL")) {
            return true;
        }
        return note.getStatus().equals(currentFilter);
    }

    /**
     * Kiểm tra note có khớp với favorite filter không
     */
    private boolean matchesFavoriteFilter(Note note) {
        if (!showFavoritesOnly) {
            return true;
        }
        return note.isFavorite();
    }

    /**
     * Kiểm tra note có khớp với category filter không
     */
    private boolean matchesCategoryFilter(Note note) {
        // Category filtering not implemented yet
        return true;
    }

    private void loadNotesIntoList() {
        filterAndDisplayNotes();
    }

    private void displayNotesInList(List<Note> notes) {
        System.out.println("displayNotesInList: CALLED with " + notes.size() + " notes");

        // Sử dụng FlowPane notesContainer (từ MainView.fxml) hoặc VBox vboxNotesList
        javafx.scene.layout.Pane targetContainer = null;

        if (notesContainer != null) {
            System.out.println("displayNotesInList: Using FlowPane notesContainer");
            targetContainer = notesContainer;
        } else if (vboxNotesList != null) {
            System.out.println("displayNotesInList: Using VBox vboxNotesList");
            targetContainer = vboxNotesList;
        } else {
            System.err.println("displayNotesInList: No container found!");
            return;
        }

        targetContainer.getChildren().clear();

        if (notes.isEmpty()) {
            System.out.println("displayNotesInList: Notes empty, showing empty state");
            VBox emptyState = createEmptyState();
            targetContainer.getChildren().add(emptyState);
            return;
        }

        System.out.println("displayNotesInList: Displaying " + notes.size() + " notes");

        for (Note note : notes) {
            System.out.println(
                    "displayNotesInList: Creating card for note ID=" + note.getId() + ", title=" + note.getTitle());
            VBox noteCard = createNoteCard(note);
            targetContainer.getChildren().add(noteCard);
        }

        // Update note count label
        updateNotesCount();
        System.out.println("displayNotesInList: COMPLETE");
    }

    private VBox createEmptyState() {
        VBox container = new VBox(15);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(40));
        container.getStyleClass().add("empty-state");

        Text icon = new Text("📝");
        icon.setStyle("-fx-font-size: 48px;");

        Label title = new Label("Chưa có ghi chú nào");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #718096;");

        Label subtitle = new Label("Nhấn '+ New Note' để tạo ghi chú đầu tiên");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #a0aec0;");

        container.getChildren().addAll(icon, title, subtitle);
        return container;
    }

    private VBox createNoteCard(Note note) {
        try {
            System.out.println("createNoteCard: Creating card for note ID=" + note.getId());

            VBox card = new VBox(8);
            card.setPadding(new Insets(14));

            // Apply custom background color if set
            String bgColor = note.getColor() != null ? note.getColor() : "#ffffff";

            // Apply status-specific styling
            String status = note.getStatus() != null ? note.getStatus() : "REGULAR";
            String statusClass = "note-card";
            switch (status) {
                case "URGENT":
                    statusClass += " note-card-urgent";
                    break;
                case "REGULAR":
                    statusClass += " note-card-regular";
                    break;
                case "IDEAS":
                    statusClass += " note-card-ideas";
                    break;
                case "COMPLETED":
                    statusClass += " note-card-completed";
                    break;
            }
            card.getStyleClass().addAll(statusClass.split(" "));

            // Custom color styling
            if (!bgColor.equals("#ffffff")) {
                card.setStyle("-fx-background-color: " + bgColor + ";");
            }

            // Header row with status and favorite
            HBox headerRow = new HBox(8);
            headerRow.setAlignment(Pos.CENTER_LEFT);

            // Status label
            Label statusLabel = new Label(status.toUpperCase());
            String statusLabelClass = "note-card-status-label";
            switch (status) {
                case "URGENT":
                    statusLabelClass += " status-label-urgent";
                    break;
                case "REGULAR":
                    statusLabelClass += " status-label-regular";
                    break;
                case "IDEAS":
                    statusLabelClass += " status-label-ideas";
                    break;
                case "COMPLETED":
                    statusLabelClass += " status-label-completed";
                    break;
            }
            statusLabel.getStyleClass().addAll(statusLabelClass.split(" "));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Favorite star
            Text favIcon = new Text(note.isFavorite() ? "⭐" : "☆");
            favIcon.setStyle("-fx-font-size: 14px; -fx-cursor: hand;");
            favIcon.setOnMouseClicked(e -> {
                e.consume();
                toggleNoteFavorite(note);
            });

            headerRow.getChildren().addAll(statusLabel, spacer, favIcon);

            // Title
            Label title = new Label(note.getTitle() != null ? note.getTitle() : "Untitled");
            title.getStyleClass().add("note-card-title");
            title.setWrapText(true);

            // Preview text
            String preview = note.getContent();
            if (preview != null && preview.length() > 80) {
                preview = preview.substring(0, 80) + "...";
            }
            Label previewLabel = new Label(preview != null ? preview : "");
            previewLabel.getStyleClass().add("note-card-preview");
            previewLabel.setWrapText(true);
            previewLabel.setMaxHeight(40);

            // Time
            String timeAgo = getTimeAgo(note.getUpdatedAt() != null ? note.getUpdatedAt() : note.getCreatedAt());
            Label timeLabel = new Label(timeAgo);
            timeLabel.getStyleClass().add("note-card-time");

            card.getChildren().addAll(headerRow, title, previewLabel, timeLabel);

            // Click handler
            card.setOnMouseClicked(e -> selectNote(note));

            // Highlight if selected
            if (selectedNote != null && selectedNote.getId() == note.getId()) {
                card.getStyleClass().add("note-card-active");
            }

            System.out.println("createNoteCard: Card created successfully for note ID=" + note.getId());
            return card;

        } catch (Exception e) {
            System.err.println("createNoteCard: ERROR creating card for note ID=" + note.getId());
            e.printStackTrace();

            // Return simple error card
            VBox errorCard = new VBox();
            errorCard.setPadding(new Insets(14));
            errorCard.getChildren().add(new Label("Error displaying note"));
            return errorCard;
        }
    }

    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null)
            return "Just now";

        long minutesAgo = java.time.Duration.between(dateTime, LocalDateTime.now()).toMinutes();

        if (minutesAgo < 1)
            return "Just now";
        if (minutesAgo < 60)
            return minutesAgo + "m ago";

        long hoursAgo = minutesAgo / 60;
        if (hoursAgo < 24)
            return hoursAgo + "h ago";

        long daysAgo = hoursAgo / 24;
        if (daysAgo < 7)
            return daysAgo + "d ago";

        return DATE_FORMAT.format(dateTime);
    }

    // ===== NOTE SELECTION & DISPLAY =====

    private void selectNote(Note note) {
        selectedNote = note;
        displayNoteInEditor(note);
        // Refresh notes list to show active state
        displayNotesInList(allNotes.stream()
                .filter(n -> matchesSearchFilter(n))
                .filter(n -> matchesStatusFilter(n))
                .filter(n -> matchesFavoriteFilter(n))
                .filter(n -> matchesCategoryFilter(n))
                .collect(Collectors.toList()));
    }

    private void displayNoteInEditor(Note note) {
        if (note == null) {
            if (lblNoteTitle != null)
                lblNoteTitle.setText("");
            if (lblNoteDate != null)
                lblNoteDate.setText("");
            if (lblNoteTime != null)
                lblNoteTime.setText("");
            if (lblNoteStatus != null)
                lblNoteStatus.setText("");
            if (txtContent != null)
                txtContent.setText("");
            updateFavoriteButton(false);
            return;
        }

        // Display title
        if (lblNoteTitle != null) {
            lblNoteTitle.setText(note.getTitle());
        }

        // Display date and time
        LocalDateTime dt = note.getUpdatedAt() != null ? note.getUpdatedAt() : note.getCreatedAt();
        if (dt != null) {
            if (lblNoteDate != null)
                lblNoteDate.setText(DATE_FORMAT.format(dt));
            if (lblNoteTime != null)
                lblNoteTime.setText(TIME_FORMAT.format(dt).toUpperCase());
        }

        // Display status badge
        if (lblNoteStatus != null) {
            lblNoteStatus.setText(note.getStatus().toUpperCase());
            lblNoteStatus.getStyleClass().removeAll("status-regular", "status-urgent", "status-ideas",
                    "status-completed");
            lblNoteStatus.getStyleClass().add("status-" + note.getStatus().toLowerCase());
        }

        // Display content
        if (txtContent != null) {
            txtContent.setText(note.getContent());
        }

        // Update favorite button
        updateFavoriteButton(note.isFavorite());
    }

    private void updateFavoriteButton(boolean isFavorite) {
        if (btnFavoriteNote != null) {
            btnFavoriteNote.setText(isFavorite ? "⭐" : "☆");
            if (isFavorite) {
                btnFavoriteNote.getStyleClass().add("favorite-active");
            } else {
                btnFavoriteNote.getStyleClass().remove("favorite-active");
            }
        }
    }

    private void updateNotesCount() {
        long count = allNotes.stream()
                .filter(this::matchesSearchFilter)
                .filter(this::matchesStatusFilter)
                .filter(this::matchesFavoriteFilter)
                .count();

        String countText = count + " ghi chú";

        if (lblNotesCount != null) {
            lblNotesCount.setText(countText);
        }
        if (noteCountLabel != null) {
            noteCountLabel.setText(countText);
        }
    }

    /**
     * Auto-save note content với debounce
     * Chỉ được gọi sau khi user ngừng gõ 1 giây (xem setupEventHandlers)
     * 
     * Chức năng:
     * - Cập nhật UpdatedAt timestamp
     * - Lưu vào database
     * - Cập nhật UI timestamp nếu thành công
     */
    private void autoSaveNote() {
        if (selectedNote != null) {
            try {
                selectedNote.setUpdatedAt(LocalDateTime.now());
                boolean success = noteService.updateNote(selectedNote);

                if (success) {
                    System.out.println("Auto-saved note: " + selectedNote.getTitle());
                    // Optionally update UI timestamp without full refresh
                    if (lblNoteDate != null && selectedNote.getUpdatedAt() != null) {
                        lblNoteDate.setText(DATE_FORMAT.format(selectedNote.getUpdatedAt()));
                    }
                    if (lblNoteTime != null && selectedNote.getUpdatedAt() != null) {
                        lblNoteTime.setText(TIME_FORMAT.format(selectedNote.getUpdatedAt()).toUpperCase());
                    }
                } else {
                    System.err.println("Failed to auto-save note: " + selectedNote.getTitle());
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi lưu ghi chú: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ===== FAVORITE HANDLING =====

    /**
     * Toggle trạng thái yêu thích (favorite/pin) của note
     * 
     * Flow:
     * 1. Toggle isFavorite trong memory
     * 2. Cập nhật database
     * 3. Nếu thành công: Update UI favorite button và refresh list
     * 4. Nếu thất bại: Revert lại trạng thái cũ
     * 
     * @param note Note cần toggle favorite
     */
    private void toggleNoteFavorite(Note note) {
        try {
            boolean newFavoriteState = !note.isFavorite();
            note.setFavorite(newFavoriteState);

            boolean success = noteService.updateNote(note);

            if (success) {
                // Update favorite button nếu đang select note này
                if (selectedNote != null && selectedNote.getId() == note.getId()) {
                    updateFavoriteButton(note.isFavorite());
                }

                // Refresh list để cập nhật star icon
                filterAndDisplayNotes();

                // Optional: show brief notification
                String message = newFavoriteState ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích";
                System.out.println(message + ": " + note.getTitle());
            } else {
                // Revert on failure
                note.setFavorite(!newFavoriteState);
                showErrorNotification("Không thể cập nhật trạng thái yêu thích.");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật favorite: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handler cho favorite button click
     * Gọi toggleNoteFavorite cho note đang được select
     */
    @FXML
    private void handleToggleFavorite() {
        if (selectedNote != null) {
            toggleNoteFavorite(selectedNote);
        }
    }

    // ===== EVENT HANDLERS - LEFT SIDEBAR =====

    @FXML
    private void handleShowAllNotes() {
        showFavoritesOnly = false;
        setNavButtonActive(btnAllNotes);
        filterAndDisplayNotes();
    }

    @FXML
    private void handleShowFavorites() {
        showFavoritesOnly = true;
        setNavButtonActive(btnFavorites);
        filterAndDisplayNotes();
    }

    @FXML
    private void handleShowShared() {
        showFavoritesOnly = false;
        setNavButtonActive(btnShared);
        filterAndDisplayNotes();
    }

    @FXML
    private void handleShowTrash() {
        try {
            // Load TrashView
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/TrashView.fxml"));
            Parent root = loader.load();

            // Get current stage and switch scene
            Stage stage = (Stage) btnTrash.getScene().getWindow();

            // Thay thế root của scene hiện tại thay vì tạo scene mới
            // Điều này giữ nguyên maximize state
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Error loading TrashView: " + e.getMessage());
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở thùng rác: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowProductLaunch() {
        setNavButtonActive(btnProductLaunch);
        filterAndDisplayNotes();
    }

    @FXML
    private void handleShowClientSyncs() {
        setNavButtonActive(btnClientSyncs);
        filterAndDisplayNotes();
    }

    private void setNavButtonActive(Button activeBtn) {
        btnAllNotes.getStyleClass().remove("nav-btn-active");
        btnFavorites.getStyleClass().remove("nav-btn-active");
        btnShared.getStyleClass().remove("nav-btn-active");
        if (btnTrash != null)
            btnTrash.getStyleClass().remove("nav-btn-active");
        btnProductLaunch.getStyleClass().remove("nav-btn-active");
        btnClientSyncs.getStyleClass().remove("nav-btn-active");

        if (!activeBtn.getStyleClass().contains("nav-btn-active")) {
            activeBtn.getStyleClass().add("nav-btn-active");
        }
    }

    // ===== EVENT HANDLERS - FILTER BUTTONS =====

    @FXML
    private void handleFilterAll() {
        currentFilter = "ALL";
        setFilterButtonActive(btnFilterAll);
        filterAndDisplayNotes();
    }

    @FXML
    private void handleFilterRegular() {
        currentFilter = "REGULAR";
        setFilterButtonActive(btnFilterRegular);
        filterAndDisplayNotes();
    }

    @FXML
    private void handleFilterDone() {
        currentFilter = "COMPLETED";
        setFilterButtonActive(btnFilterDone);
        filterAndDisplayNotes();
    }

    @FXML
    private void handleFilterUrgent() {
        currentFilter = "URGENT";
        setFilterButtonActive(btnFilterUrgent);
        filterAndDisplayNotes();
    }

    @FXML
    private void handleFilterIdeas() {
        currentFilter = "IDEAS";
        setFilterButtonActive(btnFilterIdeas);
        filterAndDisplayNotes();
    }

    private void setFilterButtonActive(Button activeBtn) {
        btnFilterAll.getStyleClass().remove("filter-btn-active");
        btnFilterRegular.getStyleClass().remove("filter-btn-active");
        btnFilterDone.getStyleClass().remove("filter-btn-active");
        btnFilterUrgent.getStyleClass().remove("filter-btn-active");
        if (btnFilterIdeas != null) {
            btnFilterIdeas.getStyleClass().remove("filter-btn-active");
        }

        if (!activeBtn.getStyleClass().contains("filter-btn-active")) {
            activeBtn.getStyleClass().add("filter-btn-active");
        }
    }

    // ===== EVENT HANDLERS - NEW NOTE =====

    @FXML
    private void handleAddNewNote() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/NewNoteDialog.fxml"));
            Parent root = loader.load();

            NewNoteDialogController dialogController = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Tạo Ghi Chú Mới");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNDECORATED);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialogStage.setScene(scene);
            dialogStage.initStyle(StageStyle.TRANSPARENT);

            // Center on screen
            dialogStage.centerOnScreen();
            dialogStage.showAndWait();

            // Check if note was created
            if (dialogController.isConfirmed() && dialogController.getCreatedNote() != null) {
                Note newNote = dialogController.getCreatedNote();
                newNote.setUserId(currentUser.getId());
                Note createdNote = noteService.createNote(newNote);

                if (createdNote != null) {
                    showSuccessNotification("Ghi chú \"" + newNote.getTitle() + "\" đã được tạo thành công!");

                    loadAllNotes();
                    loadNotesIntoList();

                    // Find and select the newly created note
                    int newNoteId = newNote.getId();
                    Note noteToSelect = allNotes.stream()
                            .filter(n -> n.getId() == newNoteId)
                            .findFirst()
                            .orElse(allNotes.isEmpty() ? null : allNotes.get(0));

                    if (noteToSelect != null) {
                        selectNote(noteToSelect);
                    }
                } else {
                    showErrorNotification("Không thể tạo ghi chú mới. Vui lòng thử lại.");
                }
            }
        } catch (IOException e) {
            // Fallback to simple dialog if custom dialog fails
            handleAddNewNoteSimple();
        }
    }

    /**
     * Wrapper method for FXML compatibility
     * Switches to CREATE MODE in the right panel (tab-based editing)
     */
    @FXML
    private void handleCreateNote() {
        switchToCreateMode();
    }

    private void handleAddNewNoteSimple() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Ghi chú mới");
        dialog.setHeaderText("Tạo ghi chú mới");
        dialog.setContentText("Tiêu đề:");

        dialog.showAndWait().ifPresent(title -> {
            if (!title.trim().isEmpty()) {
                Note newNote = new Note();
                newNote.setUserId(currentUser.getId());
                newNote.setTitle(title);
                newNote.setContent("");
                newNote.setStatus("REGULAR");
                newNote.setCategoryId(null); // Không set category mặc định
                newNote.setCreatedAt(LocalDateTime.now());
                newNote.setUpdatedAt(LocalDateTime.now());

                Note createdNote = noteService.createNote(newNote);
                if (createdNote != null) {
                    showSuccessNotification("Ghi chú \"" + title + "\" đã được tạo thành công!");

                    loadAllNotes();
                    loadNotesIntoList();

                    int newNoteId = newNote.getId();
                    Note noteToSelect = allNotes.stream()
                            .filter(n -> n.getId() == newNoteId)
                            .findFirst()
                            .orElse(allNotes.isEmpty() ? null : allNotes.get(0));

                    if (noteToSelect != null) {
                        selectNote(noteToSelect);
                    }
                } else {
                    showErrorNotification("Không thể tạo ghi chú mới. Vui lòng thử lại.");
                }
            }
        });
    }

    // ===== DELETE NOTE =====

    @FXML
    private void handleDeleteNote() {
        if (selectedNote == null) {
            showWarningNotification("Vui lòng chọn ghi chú để xóa.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận xóa");
        confirmDialog.setHeaderText("Xóa ghi chú?");
        confirmDialog.setContentText("Bạn có chắc muốn xóa ghi chú \"" + selectedNote.getTitle()
                + "\"?\nGhi chú sẽ được chuyển vào thùng rác.");

        ButtonType btnYes = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(btnYes, btnNo);

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == btnYes) {
            String deletedTitle = selectedNote.getTitle();
            boolean success = noteService.deleteNote(selectedNote.getId());

            if (success) {
                showSuccessNotification("Ghi chú \"" + deletedTitle + "\" đã được xóa!");

                allNotes.remove(selectedNote);
                selectedNote = null;

                if (!allNotes.isEmpty()) {
                    selectNote(allNotes.get(0));
                } else {
                    displayNoteInEditor(null);
                }

                filterAndDisplayNotes();
            } else {
                showErrorNotification("Không thể xóa ghi chú. Vui lòng thử lại.");
            }
        }
    }

    // ===== EDIT TITLE =====

    @FXML
    private void handleEditTitle() {
        if (selectedNote == null) {
            showWarningNotification("Vui lòng chọn ghi chú để chỉnh sửa tiêu đề.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selectedNote.getTitle());
        dialog.setTitle("Chỉnh sửa tiêu đề");
        dialog.setHeaderText("Nhập tiêu đề mới");
        dialog.setContentText("Tiêu đề:");

        dialog.showAndWait().ifPresent(newTitle -> {
            if (!newTitle.trim().isEmpty()) {
                String oldTitle = selectedNote.getTitle();
                selectedNote.setTitle(newTitle.trim());
                selectedNote.setUpdatedAt(LocalDateTime.now());

                boolean success = noteService.updateNote(selectedNote);

                if (success) {
                    showSuccessNotification("Tiêu đề đã được cập nhật thành công!");

                    if (lblNoteTitle != null) {
                        lblNoteTitle.setText(selectedNote.getTitle());
                    }
                    filterAndDisplayNotes();
                } else {
                    // Revert on failure
                    selectedNote.setTitle(oldTitle);
                    showErrorNotification("Không thể cập nhật tiêu đề. Vui lòng thử lại.");
                }
            }
        });
    }

    // ===== CHANGE STATUS =====

    @FXML
    private void handleChangeStatus() {
        if (selectedNote == null) {
            showWarningNotification("Vui lòng chọn ghi chú để thay đổi trạng thái.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(selectedNote.getStatus(),
                "REGULAR", "URGENT", "IDEAS", "COMPLETED");
        dialog.setTitle("Thay đổi trạng thái");
        dialog.setHeaderText("Chọn trạng thái mới");
        dialog.setContentText("Trạng thái:");

        dialog.showAndWait().ifPresent(status -> {
            String oldStatus = selectedNote.getStatus();
            selectedNote.setStatus(status);
            selectedNote.setUpdatedAt(LocalDateTime.now());

            boolean success = noteService.updateNote(selectedNote);

            if (success) {
                showSuccessNotification("Trạng thái đã được cập nhật thành công!");

                displayNoteInEditor(selectedNote);
                filterAndDisplayNotes();
            } else {
                // Revert on failure
                selectedNote.setStatus(oldStatus);
                showErrorNotification("Không thể cập nhật trạng thái. Vui lòng thử lại.");
            }
        });
    }

    // ===== EVENT HANDLERS - EDITOR TOOLBAR =====

    @FXML
    private void handleBold() {
        insertFormattedText("**", "**");
    }

    @FXML
    private void handleItalic() {
        insertFormattedText("*", "*");
    }

    @FXML
    private void handleUnderline() {
        insertFormattedText("__", "__");
    }

    @FXML
    private void handleHeading() {
        insertAtLineStart("## ");
    }

    @FXML
    private void handleList() {
        insertAtLineStart("• ");
    }

    @FXML
    private void handleCheckbox() {
        insertAtLineStart("☐ ");
    }

    @FXML
    private void handleCheckedBox() {
        insertAtLineStart("☑ ");
    }

    @FXML
    private void handleImage() {
        showAlert("Thông tin", "Chức năng chèn ảnh sẽ được cập nhật trong phiên bản tiếp theo!");
    }

    @FXML
    private void handleLink() {
        TextInputDialog dialog = new TextInputDialog("https://");
        dialog.setTitle("Chèn liên kết");
        dialog.setHeaderText("Thêm liên kết");
        dialog.setContentText("URL:");

        dialog.showAndWait().ifPresent(url -> {
            if (!url.trim().isEmpty()) {
                String selectedText = txtContent.getSelectedText();
                int start = txtContent.getSelection().getStart();

                if (selectedText.isEmpty()) {
                    String linkMarkdown = "[link](" + url + ")";
                    txtContent.insertText(start, linkMarkdown);
                    txtContent.selectRange(start + 1, start + 5);
                } else {
                    String linkMarkdown = "[" + selectedText + "](" + url + ")";
                    txtContent.replaceSelection(linkMarkdown);
                }
            }
        });
    }

    @FXML
    private void handleShare() {
        if (selectedNote != null) {
            showAlert("Chia sẻ", "Chia sẻ ghi chú: " + selectedNote.getTitle()
                    + "\n\nChức năng này sẽ được cập nhật trong phiên bản tiếp theo!");
        }
    }

    // ===== EVENT HANDLERS - AI ASSISTANT =====

    /**
     * Mở AI Assistant Dialog - THIẾT KẾ MỚI
     * 
     * NGUYÊN TẮC:
     * - KHÔNG tạo Stage/Window mới
     * - CHỈ hiển thị dialog modal nhẹ
     * - AI chỉ là công cụ hỗ trợ, KHÔNG phải tab độc lập
     * - Kết quả được copy để dùng trong editor hiện tại
     */
    @FXML
    private void handleOpenAiChat() {
        // Kiểm tra ghi chú đã được chọn chưa
        if (selectedNote == null) {
            showWarningNotification("Vui lòng chọn ghi chú trước khi sử dụng AI.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AIAssistantDialog.fxml"));
            Parent root = loader.load();

            // Set note hiện tại cho AI
            AIAssistantDialogController aiController = loader.getController();
            aiController.setNote(selectedNote);

            // Tạo dialog modal (KHÔNG phải Stage độc lập)
            Stage dialogStage = new Stage();
            dialogStage.setTitle("AI Assistant");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNDECORATED);
            
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialogStage.setScene(scene);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            
            dialogStage.centerOnScreen();
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            showErrorNotification("Không thể mở AI Assistant: " + e.getMessage());
        }
    }

    // ===== SETTINGS =====

    @FXML
    private void handleOpenSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SettingsView.fxml"));
            Parent root = loader.load();

            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings - SmartNote");
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.setScene(new Scene(root));
            settingsStage.setResizable(false);
            settingsStage.show();
        } catch (IOException e) {
            showAlert("Error", "Cannot open settings: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowSettings() {
        handleOpenSettings();
    }

    @FXML
    private void handleAddCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Thêm danh mục mới");
        dialog.setHeaderText("Tạo danh mục mới");
        dialog.setContentText("Tên danh mục:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(categoryName -> {
            if (!categoryName.trim().isEmpty()) {
                showAlert("Thông báo", "Danh mục \"" + categoryName + "\" đã được thêm!");
            }
        });
    }

    @FXML
    private void handleAIChat() {
        // Gọi cùng method với handleOpenAiChat
        handleOpenAiChat();
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Đăng xuất");
        alert.setHeaderText("Bạn có chắc muốn đăng xuất?");
        alert.setContentText("Các thay đổi chưa lưu sẽ bị mất.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Đăng xuất
                authService.logout();

                // Quay về màn hình đăng nhập
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
                Parent root = loader.load();

                Stage loginStage = new Stage();
                loginStage.setTitle("SmartNotebook - Đăng nhập");
                loginStage.setScene(new Scene(root));
                loginStage.setResizable(false);
                loginStage.show();

                // Đóng cửa sổ hiện tại
                Stage currentStage = (Stage) txtSearch.getScene().getWindow();
                currentStage.close();
            } catch (IOException e) {
                showAlert("Lỗi", "Không thể quay về màn hình đăng nhập: " + e.getMessage());
            }
        }
    }

    // ===== UTILITY =====

    private void insertFormattedText(String prefix, String suffix) {
        if (txtContent == null)
            return;

        String selectedText = txtContent.getSelectedText();
        int start = txtContent.getSelection().getStart();
        int end = txtContent.getSelection().getEnd();

        if (selectedText.isEmpty()) {
            txtContent.insertText(start, prefix + "text" + suffix);
            txtContent.selectRange(start + prefix.length(), start + prefix.length() + 4);
        } else {
            txtContent.replaceSelection(prefix + selectedText + suffix);
            txtContent.selectRange(start + prefix.length(), end + prefix.length());
        }
    }

    private void insertAtLineStart(String prefix) {
        if (txtContent == null)
            return;

        int caretPos = txtContent.getCaretPosition();
        String text = txtContent.getText();

        // Find the start of the current line
        int lineStart = text.lastIndexOf('\n', caretPos - 1) + 1;

        txtContent.insertText(lineStart, prefix);
        txtContent.positionCaret(caretPos + prefix.length());
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Hiển thị thông báo thành công
     */
    private void showSuccessNotification(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thành công");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Hiển thị thông báo lỗi
     */
    private void showErrorNotification(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Hiển thị thông báo cảnh báo
     */
    private void showWarningNotification(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Cảnh báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Refresh UI sau khi thay đổi dữ liệu
     */
    private void refreshUI() {
        loadAllNotes();
        filterAndDisplayNotes();
        updateNotesCount();
    }

    /**
     * Refresh và giữ note hiện tại được chọn
     */
    private void refreshUIKeepSelection() {
        Note currentNote = selectedNote;
        int currentNoteId = (currentNote != null) ? currentNote.getId() : -1;

        loadAllNotes();
        filterAndDisplayNotes();
        updateNotesCount();

        // Re-select the current note if it still exists
        if (currentNoteId > 0) {
            Note noteToSelect = allNotes.stream()
                    .filter(n -> n.getId() == currentNoteId)
                    .findFirst()
                    .orElse(null);

            if (noteToSelect != null) {
                selectNote(noteToSelect);
            }
        }
    }

    // ===== CLEANUP =====

    // ===== FORMAT HANDLERS =====

    /**
     * Format text as bold (wrap với **text**)
     */
    @FXML
    private void handleFormatBold() {
        if (txtContent == null || selectedNote == null)
            return;
        wrapSelectedText("**", "**");
    }

    /**
     * Format text as italic (wrap với *text*)
     */
    @FXML
    private void handleFormatItalic() {
        if (txtContent == null || selectedNote == null)
            return;
        wrapSelectedText("*", "*");
    }

    /**
     * Format text as underline (wrap với __text__)
     */
    @FXML
    private void handleFormatUnderline() {
        if (txtContent == null || selectedNote == null)
            return;
        wrapSelectedText("__", "__");
    }

    /**
     * Insert bullet point
     */
    @FXML
    private void handleInsertBullet() {
        if (txtContent == null || selectedNote == null)
            return;
        insertTextAtCursor("• ");
    }

    /**
     * Insert checkbox
     */
    @FXML
    private void handleInsertCheckbox() {
        if (txtContent == null || selectedNote == null)
            return;
        insertTextAtCursor("☐ ");
    }

    /**
     * Wrap selected text với prefix và suffix
     */
    private void wrapSelectedText(String prefix, String suffix) {
        String selectedText = txtContent.getSelectedText();
        int start = txtContent.getSelection().getStart();
        int end = txtContent.getSelection().getEnd();

        if (selectedText != null && !selectedText.isEmpty()) {
            String wrappedText = prefix + selectedText + suffix;
            txtContent.replaceText(start, end, wrappedText);
            // Select wrapped text
            txtContent.selectRange(start, start + wrappedText.length());
        } else {
            // No selection, insert markers và đặt cursor ở giữa
            String text = prefix + suffix;
            txtContent.insertText(txtContent.getCaretPosition(), text);
            txtContent.positionCaret(txtContent.getCaretPosition() - suffix.length());
        }
    }

    /**
     * Insert text tại cursor position
     */
    private void insertTextAtCursor(String text) {
        int caretPos = txtContent.getCaretPosition();
        txtContent.insertText(caretPos, text);
        txtContent.positionCaret(caretPos + text.length());
    }

    // ==================== CREATE MODE (TAB-BASED EDITOR) ====================

    /**
     * Switches the right panel to CREATE MODE for creating a new note.
     * This provides a Notion-like tab/panel editing experience instead of popup
     * dialogs.
     */
    private void switchToCreateMode() {
        isCreateMode = true;
        selectedNote = null;

        // Clear editor fields
        if (txtContent != null) {
            txtContent.clear();
            txtContent.setPromptText("Nhập nội dung ghi chú...");
        }

        // Update UI for create mode
        if (editorPanel != null) {
            editorPanel.setStyle("-fx-border-color: #3b82f6; -fx-border-width: 0 0 0 3;");
        }

        // Show create panel UI
        showCreatePanel();

        // Auto-focus on title field
        javafx.application.Platform.runLater(() -> {
            if (txtNewTitle != null) {
                txtNewTitle.requestFocus();
            }
        });
    }

    /**
     * Shows the create note panel UI.
     * Displays title input, color picker, favorite checkbox, and create/cancel
     * buttons.
     */
    private void showCreatePanel() {
        if (editorPanel == null) {
            System.err.println("CREATE MODE: editorPanel is null!");
            return;
        }

        // Remove existing create panel if any
        editorPanel.getChildren().removeIf(node -> node instanceof VBox && "createPanel".equals(node.getId()));

        // Create new note panel
        VBox createPanel = new VBox(15);
        createPanel.setId("createPanel");
        createPanel.setStyle("-fx-padding: 15; -fx-background-color: #f8fafc; -fx-background-radius: 8;");

        // Header
        Label headerLabel = new Label("✨ Tạo ghi chú mới");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        // Title input
        VBox titleSection = new VBox(8);
        Label titleLabel = new Label("Tiêu đề *");
        titleLabel.setStyle("-fx-text-fill: #4a5568; -fx-font-weight: bold; -fx-font-size: 13px;");

        txtNewTitle = new TextField();
        txtNewTitle.setPromptText("Nhập tiêu đề ghi chú...");
        txtNewTitle.setStyle(
                "-fx-font-size: 16px; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #cbd5e1; -fx-border-radius: 8;");

        // Enter key to quick create
        txtNewTitle.setOnAction(e -> {
            if (!txtNewTitle.getText().trim().isEmpty()) {
                handleConfirmCreate();
            }
        });

        titleSection.getChildren().addAll(titleLabel, txtNewTitle);

        // Color picker
        VBox colorSection = new VBox(8);
        Label colorLabel = new Label("Màu ghi chú");
        colorLabel.setStyle("-fx-text-fill: #4a5568; -fx-font-weight: bold; -fx-font-size: 13px;");

        HBox colorButtons = createColorPicker();
        colorSection.getChildren().addAll(colorLabel, colorButtons);

        // Favorite checkbox
        HBox favoriteSection = new HBox(10);
        favoriteSection.setAlignment(Pos.CENTER_LEFT);

        chkNewFavorite = new CheckBox();
        Label favoriteLabel = new Label("⭐ Đánh dấu yêu thích");
        favoriteLabel.setStyle("-fx-text-fill: #4a5568; -fx-font-size: 14px;");

        favoriteSection.getChildren().addAll(chkNewFavorite, favoriteLabel);

        // Buttons
        HBox buttonSection = new HBox(10);
        buttonSection.setAlignment(Pos.CENTER);

        Button btnCreate = new Button("✨ Tạo ghi chú");
        btnCreate.setStyle(
                "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;");
        btnCreate.setOnAction(e -> handleConfirmCreate());

        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle(
                "-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> handleCancelCreate());

        buttonSection.getChildren().addAll(btnCreate, btnCancel);

        // Add all sections to panel
        createPanel.getChildren().addAll(headerLabel, titleSection, colorSection, favoriteSection, buttonSection);

        // Insert at top of editor panel (after toolbar if it exists)
        int insertIndex = 0;
        for (int i = 0; i < editorPanel.getChildren().size(); i++) {
            javafx.scene.Node node = editorPanel.getChildren().get(i);
            if (node instanceof HBox && "editor-toolbar".equals(node.getStyleClass().toString())) {
                insertIndex = i + 1;
                break;
            }
        }
        editorPanel.getChildren().add(insertIndex, createPanel);
    }

    /**
     * Creates color picker with 6 pastel colors.
     * 
     * @return HBox containing color buttons
     */
    private HBox createColorPicker() {
        HBox colorBox = new HBox(10);

        String[] colors = {
                "#a8edea", // Mint
                "#c3b1e1", // Purple
                "#fed7e2", // Pink
                "#fed7aa", // Peach
                "#bae6fd", // Sky
                "#d9f99d" // Lime
        };

        selectedColor = colors[0]; // Default mint

        for (int i = 0; i < colors.length; i++) {
            final String color = colors[i];
            final boolean isFirst = (i == 0);

            Button colorBtn = new Button();
            colorBtn.setPrefSize(40, 40);
            String borderStyle = isFirst ? "3" : "2";
            String borderColor = isFirst ? "#3b82f6" : "transparent";
            colorBtn.setStyle("-fx-background-color: " + color
                    + "; -fx-background-radius: 20; -fx-cursor: hand; -fx-border-width: " + borderStyle
                    + "; -fx-border-color: " + borderColor + "; -fx-border-radius: 20;");
            colorBtn.setUserData(color);

            colorBtn.setOnAction(e -> {
                selectedColor = color;
                // Update visual selection
                colorBox.getChildren().forEach(node -> {
                    if (node instanceof Button) {
                        Button btn = (Button) node;
                        String btnColor = (String) btn.getUserData();
                        btn.setStyle("-fx-background-color: " + btnColor
                                + "; -fx-background-radius: 20; -fx-cursor: hand; -fx-border-width: " +
                                (btnColor.equals(color) ? "3" : "2") + "; -fx-border-color: " +
                                (btnColor.equals(color) ? "#3b82f6" : "transparent") + "; -fx-border-radius: 20;");
                    }
                });
            });

            colorBox.getChildren().add(colorBtn);
        }

        return colorBox;
    }

    /**
     * Handles CREATE button click.
     * Validates input and saves note to database using NoteService.
     */
    private void handleConfirmCreate() {
        String title = txtNewTitle.getText().trim();
        String content = txtContent != null ? txtContent.getText() : "";
        boolean isFavorite = chkNewFavorite.isSelected();

        // Validation
        if (title.isEmpty()) {
            txtNewTitle.setStyle(
                    "-fx-border-color: #fc8181; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
            txtNewTitle.setPromptText("⚠️ Tiêu đề không được để trống!");
            shakeNode(txtNewTitle);
            return;
        }

        // Create note object
        Note newNote = new Note();
        newNote.setUserId(currentUser.getId());
        newNote.setTitle(title);
        newNote.setContent(content);
        newNote.setStatus("REGULAR");
        newNote.setFavorite(isFavorite);
        newNote.setColor(selectedColor);
        newNote.setCategoryId(null);
        newNote.setCreatedAt(LocalDateTime.now());
        newNote.setUpdatedAt(LocalDateTime.now());

        // Save to database via NoteService
        Note createdNote = noteService.createNote(newNote);

        if (createdNote != null) {
            showSuccessNotification("✅ Ghi chú \"" + title + "\" đã được tạo!");

            // Reload notes from database
            loadAllNotes();
            loadNotesIntoList();

            // Select newly created note
            Note noteToSelect = allNotes.stream()
                    .filter(n -> n.getId() == createdNote.getId())
                    .findFirst()
                    .orElse(null);

            if (noteToSelect != null) {
                selectNote(noteToSelect);
            }

            // Switch back to view mode
            switchToViewMode();
        } else {
            showErrorNotification("❌ Không thể tạo ghi chú. Vui lòng thử lại!");
        }
    }

    /**
     * Handles CANCEL button click.
     * Returns to view mode without saving (with confirmation if user has typed
     * content).
     */
    private void handleCancelCreate() {
        // Confirm if user has typed anything
        boolean hasTitle = txtNewTitle != null && !txtNewTitle.getText().trim().isEmpty();
        boolean hasContent = txtContent != null && !txtContent.getText().trim().isEmpty();

        if (hasTitle || hasContent) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Hủy tạo ghi chú");
            confirmDialog.setHeaderText("Bạn có chắc muốn hủy?");
            confirmDialog.setContentText("Nội dung bạn đã nhập sẽ không được lưu.");

            ButtonType btnYes = new ButtonType("Hủy", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnNo = new ButtonType("Tiếp tục chỉnh sửa", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmDialog.getButtonTypes().setAll(btnYes, btnNo);

            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == btnYes) {
                    switchToViewMode();
                }
            });
        } else {
            switchToViewMode();
        }
    }

    /**
     * Switches back to VIEW/EDIT mode.
     * Removes create panel and resets UI to normal state.
     */
    private void switchToViewMode() {
        isCreateMode = false;

        if (editorPanel == null)
            return;

        // Remove create panel
        editorPanel.getChildren().removeIf(node -> node instanceof VBox && "createPanel".equals(node.getId()));

        // Reset UI
        editorPanel.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 1;");

        // Clear content
        if (txtContent != null) {
            txtContent.clear();
            txtContent.setPromptText("Bắt đầu viết ghi chú của bạn...");
        }

        // Clear title field reference
        txtNewTitle = null;
        chkNewFavorite = null;
    }

    /**
     * Shake animation for validation errors.
     * 
     * @param node The node to shake
     */
    private void shakeNode(javafx.scene.Node node) {
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                javafx.util.Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.play();
    }

    // ==================== END CREATE MODE ====================

    // ===== CLEANUP =====

    /**
     * Cleanup method được gọi khi controller bị destroy
     * Shutdown auto-save executor để tránh memory leak
     * 
     * NOTE: Nếu dùng JavaFX 8+, có thể gọi từ window close event
     */
    public void cleanup() {
        if (autoSaveExecutor != null && !autoSaveExecutor.isShutdown()) {
            autoSaveExecutor.shutdown();
            try {
                // Wait 2 seconds for pending tasks to complete
                if (!autoSaveExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    autoSaveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                autoSaveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
