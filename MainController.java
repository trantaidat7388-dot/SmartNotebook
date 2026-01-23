package com.dat.notebook.controller;

/**
 * EXAMPLE: Tích hợp Rich Text Editor vào MainController
 * 
 * Copy các đoạn code dưới đây vào MainController.java của bạn
 */

// ===== 1. THÊM IMPORT =====

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// ===== 2. THÊM VÀO CLASS MAINCONTROLLER =====

public class MainController {
    
    // Existing code...
    
    // Thêm reference đến Rich Text Editor
    private Stage richTextEditorStage = null;
    private RichTextEditorController richTextEditorController = null;
    
    /**
     * Mở Rich Text Editor trong window riêng
     * Gọi method này khi user click "Rich Text Editor" button
     */
    @FXML
    private void handleOpenRichTextEditor() {
        try {
            // Nếu window đã mở, chỉ cần focus
            if (richTextEditorStage != null && richTextEditorStage.isShowing()) {
                richTextEditorStage.requestFocus();
                
                // Nếu có note được chọn, mở nó
                if (selectedNote != null) {
                    richTextEditorController.openNote(selectedNote);
                }
                return;
            }
            
            // Load FXML
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/RichTextEditorView.fxml")
            );
            Parent root = loader.load();
            
            // Get controller
            richTextEditorController = loader.getController();
            
            // Nếu có note được chọn, mở nó ngay
            if (selectedNote != null) {
                richTextEditorController.openNote(selectedNote);
            }
            
            // Tạo window mới
            richTextEditorStage = new Stage();
            richTextEditorStage.setTitle("SmartNotebook - Rich Text Editor");
            richTextEditorStage.setScene(new Scene(root, 1000, 650));
            
            // Cleanup khi đóng window
            richTextEditorStage.setOnCloseRequest(event -> {
                if (richTextEditorController != null) {
                    richTextEditorController.shutdown();
                }
                richTextEditorStage = null;
                richTextEditorController = null;
            });
            
            richTextEditorStage.show();
            
            System.out.println("Rich Text Editor opened");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi khi mở Rich Text Editor: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    /**
     * Mở note trong Rich Text Editor
     * Gọi khi user double-click vào note card
     */
    private void openNoteInRichTextEditor(Note note) {
        handleOpenRichTextEditor(); // Mở window nếu chưa mở
        
        if (richTextEditorController != null) {
            richTextEditorController.openNote(note);
        }
    }
    
    /**
     * Alternative: Embed Rich Text Editor vào MainView (không dùng window riêng)
     * Uncomment nếu muốn dùng cách này
     */
    /*
    @FXML private BorderPane mainContent; // BorderPane chính của MainView
    
    private void handleOpenRichTextEditorEmbedded() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/RichTextEditorView.fxml")
            );
            Parent richTextView = loader.load();
            
            richTextEditorController = loader.getController();
            
            // Replace center content với Rich Text Editor
            mainContent.setCenter(richTextView);
            
            if (selectedNote != null) {
                richTextEditorController.openNote(selectedNote);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    */
}

// ===== 3. THÊM BUTTON VÀO MAINVIEW.FXML =====

/*
<!-- Trong MainView.fxml, thêm button vào sidebar hoặc toolbar -->

<Button text="✨ Rich Text Editor" 
        styleClass="sidebar-btn"
        onAction="#handleOpenRichTextEditor" 
        maxWidth="Infinity"/>

<!-- Hoặc button lớn hơn trong content area -->

<Button text="📝 Mở Rich Text Editor" 
        styleClass="btn-primary"
        onAction="#handleOpenRichTextEditor"
        style="-fx-font-size: 14px; -fx-padding: 12 24;"/>
*/

// ===== 4. DOUBLE-CLICK NOTE CARD ĐỂ MỞ RICH TEXT EDITOR =====

/*
// Trong method tạo note card (createNoteCard hoặc tương tự)

noteCard.setOnMouseClicked(event -> {
    if (event.getClickCount() == 2) {
        // Double click
        openNoteInRichTextEditor(note);
    } else {
        // Single click
        selectNote(note);
    }
});
*/

// ===== 5. CONTEXT MENU CHO NOTE CARD =====

/*
ContextMenu contextMenu = new ContextMenu();

MenuItem openInEditor = new MenuItem("Mở trong Rich Text Editor");
openInEditor.setOnAction(e -> openNoteInRichTextEditor(note));

MenuItem editHere = new MenuItem("Chỉnh sửa tại đây");
editHere.setOnAction(e -> selectNote(note));

MenuItem delete = new MenuItem("Xóa");
delete.setOnAction(e -> handleDeleteNote());

contextMenu.getItems().addAll(openInEditor, editHere, new SeparatorMenuItem(), delete);

noteCard.setOnContextMenuRequested(event -> {
    contextMenu.show(noteCard, event.getScreenX(), event.getScreenY());
});
*/

// ===== 6. REFRESH NOTE LIST SAU KHI SAVE =====

/*
// Nếu muốn refresh danh sách notes sau khi save trong Rich Text Editor,
// có thể implement listener pattern hoặc callback

// Option 1: Polling (simple but not optimal)
private void startNoteRefreshPolling() {
    Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
        refreshNotesList();
    }));
    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.play();
}

// Option 2: Callback (better)
public interface NoteChangeListener {
    void onNoteChanged(Note note);
    void onNoteDeleted(int noteId);
}

// Trong RichTextEditorController
private List<NoteChangeListener> listeners = new ArrayList<>();

public void addNoteChangeListener(NoteChangeListener listener) {
    listeners.add(listener);
}

private void notifyNoteChanged(Note note) {
    for (NoteChangeListener listener : listeners) {
        listener.onNoteChanged(note);
    }
}

// Trong MainController
richTextEditorController.addNoteChangeListener(new NoteChangeListener() {
    @Override
    public void onNoteChanged(Note note) {
        Platform.runLater(() -> {
            refreshNotesList();
        });
    }
    
    @Override
    public void onNoteDeleted(int noteId) {
        Platform.runLater(() -> {
            removeNoteFromUI(noteId);
        });
    }
});
*/

// ===== 7. COMPLETE EXAMPLE =====

/*
// Đoạn code hoàn chỉnh để thêm vào MainController

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class MainController {
    
    private Stage richTextEditorStage;
    private RichTextEditorController richTextEditorController;
    private Note selectedNote;
    
    @FXML
    private void handleOpenRichTextEditor() {
        try {
            if (richTextEditorStage != null && richTextEditorStage.isShowing()) {
                richTextEditorStage.requestFocus();
                if (selectedNote != null) {
                    richTextEditorController.openNote(selectedNote);
                }
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/RichTextEditorView.fxml")
            );
            Parent root = loader.load();
            richTextEditorController = loader.getController();
            
            if (selectedNote != null) {
                richTextEditorController.openNote(selectedNote);
            }
            
            richTextEditorStage = new Stage();
            richTextEditorStage.setTitle("SmartNotebook - Rich Text Editor");
            richTextEditorStage.setScene(new Scene(root, 1000, 650));
            richTextEditorStage.setOnCloseRequest(e -> {
                if (richTextEditorController != null) {
                    richTextEditorController.shutdown();
                }
                richTextEditorStage = null;
                richTextEditorController = null;
            });
            richTextEditorStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Lỗi: " + e.getMessage());
            alert.show();
        }
    }
    
    private void openNoteInRichTextEditor(Note note) {
        this.selectedNote = note;
        handleOpenRichTextEditor();
    }
}
*/
