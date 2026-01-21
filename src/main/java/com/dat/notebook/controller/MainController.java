package com.dat.notebook.controller;

import com.dat.notebook.model.Note;
import com.dat.notebook.model.Category;
import com.dat.notebook.util.NoteDAO;
import com.dat.notebook.util.CategoryDAO;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainController {

    // FXML Controls
    @FXML
    private TextField txtSearch;
    @FXML
    private TextField txtTitle;
    @FXML
    private TextArea txtContent;
    @FXML
    private ListView<Note> listNotes;
    @FXML
    private ComboBox<Category> cmbCategory;
    @FXML
    private Label lblCurrentCategory;
    @FXML
    private Label lblNoteCount;
    @FXML
    private Label lblLastUpdated;

    // Category Buttons
    @FXML
    private Button btnAllNotes;
    @FXML
    private Button btnWorkNotes;
    @FXML
    private Button btnPersonalNotes;
    @FXML
    private Button btnStudyNotes;

    // Data
    private NoteDAO noteDAO = new NoteDAO();
    private CategoryDAO categoryDAO = new CategoryDAO();
    private ObservableList<Note> notesData = FXCollections.observableArrayList();
    private Note selectedNote = null;
    private int currentCategoryFilter = -1; // -1 = All notes

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        // Load categories into ComboBox
        loadCategories();

        // Load all notes
        loadNotes();

        // Configure ListView
        configureNotesList();

        // Configure search
        configureSearch();

        // Update note count
        updateNoteCount();
    }

    private void loadCategories() {
        List<Category> categories = categoryDAO.getAllCategories();
        cmbCategory.setItems(FXCollections.observableArrayList(categories));

        // Custom display for ComboBox
        cmbCategory.setCellFactory(lv -> new ListCell<Category>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        cmbCategory.setButtonCell(new ListCell<Category>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
    }

    private void loadNotes() {
        List<Note> notes;
        if (currentCategoryFilter == -1) {
            notes = noteDAO.getAllNotes();
        } else {
            notes = noteDAO.getNotesByCategory(currentCategoryFilter);
        }
        notesData.setAll(notes);
        listNotes.setItems(notesData);
        updateNoteCount();
    }

    private void configureNotesList() {
        // Custom cell factory to display note title
        listNotes.setCellFactory(lv -> new ListCell<Note>() {
            @Override
            protected void updateItem(Note note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText("📝 " + note.getTitle());
                }
            }
        });

        // Selection listener
        listNotes.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedNote = newVal;
                displayNote(newVal);
            }
        });
    }

    private void configureSearch() {
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                loadNotes();
            } else {
                List<Note> results = noteDAO.searchNotes(newVal);
                if (currentCategoryFilter != -1) {
                    results = results.stream()
                            .filter(n -> n.getCategoryId() == currentCategoryFilter)
                            .toList();
                }
                notesData.setAll(results);
                updateNoteCount();
            }
        });
    }

    private void displayNote(Note note) {
        txtTitle.setText(note.getTitle());
        txtContent.setText(note.getContent());

        // Select category in ComboBox
        for (Category cat : cmbCategory.getItems()) {
            if (cat.getId() == note.getCategoryId()) {
                cmbCategory.getSelectionModel().select(cat);
                break;
            }
        }

        // Update last modified label
        if (note.getUpdatedAt() != null) {
            lblLastUpdated.setText("Cập nhật: " + note.getUpdatedAt().format(DATE_FORMAT));
        } else if (note.getCreatedAt() != null) {
            lblLastUpdated.setText("Tạo: " + note.getCreatedAt().format(DATE_FORMAT));
        }
    }

    private void updateNoteCount() {
        int count = notesData.size();
        lblNoteCount.setText(count + " ghi chú");
    }

    private void clearEditor() {
        txtTitle.clear();
        txtContent.clear();
        cmbCategory.getSelectionModel().clearSelection();
        lblLastUpdated.setText("");
        selectedNote = null;
        listNotes.getSelectionModel().clearSelection();
    }

    private void setCategoryButtonActive(Button activeBtn) {
        // Remove active class from all
        btnAllNotes.getStyleClass().remove("category-active");
        btnWorkNotes.getStyleClass().remove("category-active");
        btnPersonalNotes.getStyleClass().remove("category-active");
        btnStudyNotes.getStyleClass().remove("category-active");

        // Add active class to selected
        if (!activeBtn.getStyleClass().contains("category-active")) {
            activeBtn.getStyleClass().add("category-active");
        }
    }

    // === EVENT HANDLERS ===

    @FXML
    private void handleSaveNote() {
        String title = txtTitle.getText();
        String content = txtContent.getText();

        if (title == null || title.trim().isEmpty()) {
            showAlert("Thông báo", "Vui lòng nhập tiêu đề!");
            return;
        }

        Category selectedCategory = cmbCategory.getSelectionModel().getSelectedItem();
        int categoryId = selectedCategory != null ? selectedCategory.getId() : 0;

        if (selectedNote != null) {
            // Update existing note
            selectedNote.setTitle(title);
            selectedNote.setContent(content);
            selectedNote.setCategoryId(categoryId);
            noteDAO.updateNote(selectedNote);
            showAlert("Thành công", "Đã cập nhật ghi chú: " + title);
        } else {
            // Create new note
            Note newNote = new Note();
            newNote.setTitle(title);
            newNote.setContent(content);
            newNote.setCategoryId(categoryId);
            noteDAO.insertNote(newNote);
            showAlert("Thành công", "Đã tạo ghi chú mới: " + title);
        }

        loadNotes();
    }

    @FXML
    private void handleAddNewNote() {
        clearEditor();
        txtTitle.requestFocus();
    }

    @FXML
    private void handleDeleteNote() {
        if (selectedNote == null) {
            showAlert("Thông báo", "Vui lòng chọn ghi chú cần xóa!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn xóa ghi chú '" + selectedNote.getTitle() + "'?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                noteDAO.deleteNote(selectedNote.getId());
                clearEditor();
                loadNotes();
                showAlert("Thành công", "Đã xóa ghi chú!");
            }
        });
    }

    @FXML
    private void handleShowAllNotes() {
        currentCategoryFilter = -1;
        lblCurrentCategory.setText("Tất cả ghi chú");
        setCategoryButtonActive(btnAllNotes);
        loadNotes();
    }

    @FXML
    private void handleShowWorkNotes() {
        currentCategoryFilter = 1; // Công việc
        lblCurrentCategory.setText("Công việc");
        setCategoryButtonActive(btnWorkNotes);
        loadNotes();
    }

    @FXML
    private void handleShowPersonalNotes() {
        currentCategoryFilter = 2; // Cá nhân
        lblCurrentCategory.setText("Cá nhân");
        setCategoryButtonActive(btnPersonalNotes);
        loadNotes();
    }

    @FXML
    private void handleShowStudyNotes() {
        currentCategoryFilter = 3; // Học tập
        lblCurrentCategory.setText("Học tập");
        setCategoryButtonActive(btnStudyNotes);
        loadNotes();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
