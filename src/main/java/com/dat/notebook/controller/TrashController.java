package com.dat.notebook.controller;

import com.dat.notebook.model.Note;
import com.dat.notebook.model.User;
import com.dat.notebook.service.AuthService;
import com.dat.notebook.service.NoteService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class TrashController {

    // FXML Components
    @FXML private Button btnBack;
    @FXML private Button btnEmptyTrash;
    @FXML private Label lblArchivedCount;
    @FXML private VBox vboxArchivedNotes;
    @FXML private Label lblNoteTitle;
    @FXML private Label lblNoteStatus;
    @FXML private Label lblArchivedDate;
    @FXML private Label lblNoteContent;
    @FXML private Button btnRestore;
    @FXML private Button btnDeletePermanently;

    // Services & Data
    private final NoteService noteService = new NoteService();
    private final User currentUser = AuthService.getInstance().getCurrentUser();
    private List<Note> archivedNotes;
    private Note selectedNote;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        System.out.println("TrashController: Initializing...");
        
        if (currentUser == null) {
            showAlert("Lỗi", "Không tìm thấy người dùng đã đăng nhập!");
            return;
        }
        
        loadArchivedNotes();
    }

    /**
     * Load all archived notes for current user
     */
    private void loadArchivedNotes() {
        System.out.println("TrashController: Loading archived notes for user ID=" + currentUser.getId());
        
        archivedNotes = noteService.getArchivedNotes(currentUser.getId());
        System.out.println("TrashController: Found " + archivedNotes.size() + " archived notes");
        
        displayArchivedNotes();
        updateArchivedCount();
    }

    /**
     * Display archived notes in sidebar
     */
    private void displayArchivedNotes() {
        vboxArchivedNotes.getChildren().clear();
        
        if (archivedNotes.isEmpty()) {
            VBox emptyState = new VBox(15);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(50));
            
            Label emptyIcon = new Label("🗑️");
            emptyIcon.setStyle("-fx-font-size: 48px;");
            
            Label emptyText = new Label("Thùng rác trống");
            emptyText.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8;");
            
            emptyState.getChildren().addAll(emptyIcon, emptyText);
            vboxArchivedNotes.getChildren().add(emptyState);
            return;
        }
        
        for (Note note : archivedNotes) {
            VBox noteCard = createArchivedNoteCard(note);
            vboxArchivedNotes.getChildren().add(noteCard);
        }
    }

    /**
     * Create card for archived note
     */
    private VBox createArchivedNoteCard(Note note) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.getStyleClass().add("note-card");
        card.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8; -fx-border-color: #334155; -fx-border-radius: 8;");
        
        // Title
        Label title = new Label(note.getTitle() != null ? note.getTitle() : "Untitled");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e2e8f0;");
        title.setWrapText(true);
        
        // Preview
        String preview = note.getContent();
        if (preview != null && preview.length() > 60) {
            preview = preview.substring(0, 60) + "...";
        }
        Label previewLabel = new Label(preview != null ? preview : "");
        previewLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        previewLabel.setWrapText(true);
        previewLabel.setMaxHeight(40);
        
        // Deleted date
        String deletedDate = note.getUpdatedAt() != null ? 
            "Deleted: " + DATE_FORMAT.format(note.getUpdatedAt()) : 
            "Deleted: Unknown";
        Label dateLabel = new Label(deletedDate);
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        
        card.getChildren().addAll(title, previewLabel, dateLabel);
        
        // Click handler
        card.setOnMouseClicked(e -> selectNote(note));
        
        // Highlight if selected
        if (selectedNote != null && selectedNote.getId() == note.getId()) {
            card.setStyle("-fx-background-color: #334155; -fx-background-radius: 8; -fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 8;");
        }
        
        return card;
    }

    /**
     * Select note and display in preview panel
     */
    private void selectNote(Note note) {
        selectedNote = note;
        
        // Update preview panel
        lblNoteTitle.setText(note.getTitle() != null ? note.getTitle() : "Untitled");
        lblNoteStatus.setText("Status: " + (note.getStatus() != null ? note.getStatus() : "REGULAR"));
        lblArchivedDate.setText("Deleted: " + (note.getUpdatedAt() != null ? DATE_FORMAT.format(note.getUpdatedAt()) : "Unknown"));
        lblNoteContent.setText(note.getContent() != null ? note.getContent() : "No content");
        
        // Enable action buttons
        btnRestore.setDisable(false);
        btnDeletePermanently.setDisable(false);
        
        // Refresh list to show selection
        displayArchivedNotes();
    }

    /**
     * Update archived notes count
     */
    private void updateArchivedCount() {
        int count = archivedNotes.size();
        lblArchivedCount.setText(count + " ghi chú đã xóa");
    }

    // ===== EVENT HANDLERS =====

    @FXML
    private void handleBack() {
        try {
            // Go back to main view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainView.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) btnBack.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 900);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Error loading MainView: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRestore() {
        if (selectedNote == null) {
            showAlert("Thông báo", "Vui lòng chọn ghi chú để khôi phục.");
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Xác nhận khôi phục");
        confirmDialog.setHeaderText("Khôi phục ghi chú?");
        confirmDialog.setContentText("Bạn muốn khôi phục ghi chú \"" + selectedNote.getTitle() + "\"?");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = noteService.restoreNote(selectedNote.getId());
            if (success) {
                showInfoAlert("Thành công", "Ghi chú đã được khôi phục!");
                loadArchivedNotes(); // Refresh
                
                // Clear selection
                selectedNote = null;
                lblNoteTitle.setText("Chọn một ghi chú để xem");
                lblNoteStatus.setText("Status: -");
                lblArchivedDate.setText("Deleted: -");
                lblNoteContent.setText("Nội dung sẽ hiển thị ở đây...");
                btnRestore.setDisable(true);
                btnDeletePermanently.setDisable(true);
            } else {
                showAlert("Lỗi", "Không thể khôi phục ghi chú.");
            }
        }
    }

    @FXML
    private void handleDeletePermanently() {
        if (selectedNote == null) {
            showAlert("Thông báo", "Vui lòng chọn ghi chú để xóa.");
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.WARNING);
        confirmDialog.setTitle("XÓA VĨNH VIỄN");
        confirmDialog.setHeaderText("⚠️ CẢNH BÁO: Hành động này KHÔNG THỂ HOÀN TÁC!");
        confirmDialog.setContentText("Bạn có CHẮC CHẮN muốn xóa vĩnh viễn ghi chú \"" + selectedNote.getTitle() + "\"?\n\nGhi chú sẽ bị mất hoàn toàn và không thể khôi phục.");
        
        ButtonType btnConfirm = new ButtonType("XÓA VĨNH VIỄN", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(btnConfirm, btnCancel);
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == btnConfirm) {
            boolean success = noteService.deleteNotePermanently(selectedNote.getId());
            if (success) {
                showInfoAlert("Đã xóa", "Ghi chú đã được xóa vĩnh viễn.");
                loadArchivedNotes(); // Refresh
                
                // Clear selection
                selectedNote = null;
                lblNoteTitle.setText("Chọn một ghi chú để xem");
                lblNoteStatus.setText("Status: -");
                lblArchivedDate.setText("Deleted: -");
                lblNoteContent.setText("Nội dung sẽ hiển thị ở đây...");
                btnRestore.setDisable(true);
                btnDeletePermanently.setDisable(true);
            } else {
                showAlert("Lỗi", "Không thể xóa ghi chú.");
            }
        }
    }

    @FXML
    private void handleEmptyTrash() {
        if (archivedNotes.isEmpty()) {
            showAlert("Thông báo", "Thùng rác đã trống.");
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.WARNING);
        confirmDialog.setTitle("XÓA TẤT CẢ");
        confirmDialog.setHeaderText("⚠️ CẢNH BÁO NGHIÊM TRỌNG!");
        confirmDialog.setContentText("Bạn có CHẮC CHẮN muốn xóa vĩnh viễn TẤT CẢ " + archivedNotes.size() + " ghi chú?\n\nHành động này KHÔNG THỂ HOÀN TÁC!");
        
        ButtonType btnConfirm = new ButtonType("XÓA TẤT CẢ", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getButtonTypes().setAll(btnConfirm, btnCancel);
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == btnConfirm) {
            int successCount = 0;
            for (Note note : archivedNotes) {
                if (noteService.deleteNotePermanently(note.getId())) {
                    successCount++;
                }
            }
            
            if (successCount > 0) {
                showInfoAlert("Hoàn tất", "Đã xóa vĩnh viễn " + successCount + " ghi chú.");
                loadArchivedNotes(); // Refresh
                
                // Clear selection
                selectedNote = null;
                lblNoteTitle.setText("Chọn một ghi chú để xem");
                lblNoteStatus.setText("Status: -");
                lblArchivedDate.setText("Deleted: -");
                lblNoteContent.setText("Nội dung sẽ hiển thị ở đây...");
                btnRestore.setDisable(true);
                btnDeletePermanently.setDisable(true);
            }
        }
    }

    // ===== UTILITY METHODS =====

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
