package com.dat.notebook.controller;

import com.dat.notebook.model.Note;
import com.dat.notebook.service.AuthService;
import com.dat.notebook.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;

/**
 * Controller cho dialog tạo ghi chú mới
 */
public class NewNoteDialogController {

    // FXML fields matching the new FXML
    @FXML private TextField titleField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private CheckBox favoriteCheck;
    
    // Color buttons
    @FXML private Button colorMint;
    @FXML private Button colorPurple;
    @FXML private Button colorPink;
    @FXML private Button colorPeach;
    @FXML private Button colorSky;
    @FXML private Button colorLime;
    
    // Legacy fields for backwards compatibility
    @FXML private TextField txtTitle;
    @FXML private TextArea txtContent;
    @FXML private ToggleButton btnStatusRegular;
    @FXML private ToggleButton btnStatusUrgent;
    @FXML private ToggleButton btnStatusIdeas;
    @FXML private CheckBox chkFavorite;
    @FXML private HBox colorContainer;
    @FXML private Button btnClose;
    @FXML private Button btnCreate;

    private ToggleGroup statusGroup;
    private String selectedStatus = "REGULAR";
    private String selectedColor = "#ffffff";
    private Note createdNote = null;
    private boolean confirmed = false;

    @FXML
    public void initialize() {
        // Setup new FXML style
        if (titleField != null) {
            setupNewStyle();
        }
        // Setup old FXML style
        else if (txtTitle != null) {
            setupOldStyle();
        }
    }
    
    private void setupNewStyle() {
        // Setup category combo
        if (categoryCombo != null) {
            categoryCombo.getItems().addAll("Personal", "Work", "Ideas", "To-do");
            categoryCombo.getSelectionModel().selectFirst();
        }
        
        // Setup color buttons
        setupColorButton(colorMint, "#a8edea");
        setupColorButton(colorPurple, "#c3b1e1");
        setupColorButton(colorPink, "#fed7e2");
        setupColorButton(colorPeach, "#fed7aa");
        setupColorButton(colorSky, "#bae6fd");
        setupColorButton(colorLime, "#d9f99d");
        
        // Focus vào title khi mở
        titleField.requestFocus();
        
        // Enter để tạo nhanh
        titleField.setOnAction(e -> {
            if (!titleField.getText().trim().isEmpty()) {
                handleCreate();
            }
        });
    }
    
    private void setupColorButton(Button button, String color) {
        if (button != null) {
            button.setUserData(color);
            button.setOnAction(e -> {
                selectedColor = color;
                // Update visual selection
                clearColorSelection();
                button.setStyle(button.getStyle() + " -fx-border-color: #3b82f6; -fx-border-width: 3;");
            });
        }
    }
    
    private void clearColorSelection() {
        Button[] buttons = {colorMint, colorPurple, colorPink, colorPeach, colorSky, colorLime};
        for (Button btn : buttons) {
            if (btn != null) {
                String baseStyle = "-fx-background-color: " + btn.getUserData() + "; -fx-background-radius: 20; -fx-pref-width: 40; -fx-pref-height: 40; -fx-cursor: hand;";
                btn.setStyle(baseStyle);
            }
        }
    }
    
    private void setupOldStyle() {
        // Tạo ToggleGroup cho status buttons
        if (btnStatusRegular != null && btnStatusUrgent != null && btnStatusIdeas != null) {
            statusGroup = new ToggleGroup();
            btnStatusRegular.setToggleGroup(statusGroup);
            btnStatusUrgent.setToggleGroup(statusGroup);
            btnStatusIdeas.setToggleGroup(statusGroup);
            
            // Chọn mặc định là Regular
            btnStatusRegular.setSelected(true);
            updateStatusSelection();
            
            // Lắng nghe thay đổi status
            statusGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null) {
                    oldVal.setSelected(true);
                } else {
                    updateStatusSelection();
                }
            });
        }
        
        // Focus vào title khi mở
        txtTitle.requestFocus();
        
        // Enter để tạo nhanh
        txtTitle.setOnAction(e -> {
            if (!txtTitle.getText().trim().isEmpty()) {
                handleCreate();
            }
        });
    }

    private void updateStatusSelection() {
        if (btnStatusRegular != null && btnStatusRegular.isSelected()) {
            selectedStatus = "REGULAR";
        } else if (btnStatusUrgent != null && btnStatusUrgent.isSelected()) {
            selectedStatus = "URGENT";
        } else if (btnStatusIdeas != null && btnStatusIdeas.isSelected()) {
            selectedStatus = "IDEAS";
        }
    }

    @FXML
    private void handleColorSelect(javafx.event.ActionEvent event) {
        Button clickedButton = (Button) event.getSource();
        selectedColor = (String) clickedButton.getUserData();
        
        // Cập nhật visual selection
        if (colorContainer != null) {
            colorContainer.getChildren().forEach(node -> {
                if (node instanceof Button) {
                    node.getStyleClass().remove("color-btn-selected");
                }
            });
            clickedButton.getStyleClass().add("color-btn-selected");
        }
    }

    @FXML
    private void handleCreate() {
        String title;
        String content = "";
        boolean isFavorite = false;
        
        // Get values from appropriate fields
        if (titleField != null) {
            title = titleField.getText().trim();
            isFavorite = favoriteCheck != null && favoriteCheck.isSelected();
            
            // Map category to status
            if (categoryCombo != null) {
                String category = categoryCombo.getValue();
                switch (category) {
                    case "Work":
                        selectedStatus = "URGENT";
                        break;
                    case "Ideas":
                        selectedStatus = "IDEAS";
                        break;
                    case "To-do":
                        selectedStatus = "REGULAR";
                        break;
                    default:
                        selectedStatus = "REGULAR";
                }
            }
        } else {
            title = txtTitle != null ? txtTitle.getText().trim() : "";
            content = txtContent != null ? txtContent.getText() : "";
            isFavorite = chkFavorite != null && chkFavorite.isSelected();
        }
        
        // Validate
        String error = ValidationUtil.validateNoteTitle(title);
        if (error != null) {
            // Hiển thị lỗi
            if (titleField != null) {
                titleField.setStyle("-fx-border-color: #fc8181;");
                titleField.setPromptText(error);
                shakeNode(titleField);
            } else if (txtTitle != null) {
                txtTitle.setStyle("-fx-border-color: #fc8181;");
                txtTitle.setPromptText(error);
                shakeNode(txtTitle);
            }
            return;
        }
        
        // Tạo note mới
        createdNote = new Note();
        createdNote.setTitle(title);
        createdNote.setContent(content);
        createdNote.setStatus(selectedStatus);
        createdNote.setFavorite(isFavorite);
        createdNote.setColor(selectedColor);
        createdNote.setCategoryId(null);  // Không set category mặc định - sẽ được set sau
        
        // Set UserID from authenticated user
        if (AuthService.getInstance().getCurrentUser() != null) {
            createdNote.setUserId(AuthService.getInstance().getCurrentUser().getId());
            System.out.println("NewNoteDialog: Set UserID=" + createdNote.getUserId() + " for new note");
        } else {
            System.err.println("NewNoteDialog: ERROR - No authenticated user!");
        }
        
        createdNote.setCreatedAt(LocalDateTime.now());
        createdNote.setUpdatedAt(LocalDateTime.now());
        
        confirmed = true;
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        handleClose();
    }

    @FXML
    private void handleClose() {
        confirmed = false;
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = null;
        if (titleField != null) {
            stage = (Stage) titleField.getScene().getWindow();
        } else if (txtTitle != null) {
            stage = (Stage) txtTitle.getScene().getWindow();
        }
        if (stage != null) {
            stage.close();
        }
    }

    private void shakeNode(javafx.scene.Node node) {
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
            javafx.util.Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.play();
    }

    public Note getCreatedNote() {
        return createdNote;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
