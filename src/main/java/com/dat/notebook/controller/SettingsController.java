package com.dat.notebook.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class SettingsController {

    @FXML private Slider sliderFontSize;
    @FXML private Label lblFontSize;
    @FXML private ComboBox<String> cmbTheme;
    @FXML private TabPane tabPane;
    @FXML private CheckBox chkAutoSave;
    @FXML private CheckBox chkShowNotifications;
    @FXML private CheckBox chkConfirmDelete;

    @FXML
    public void initialize() {
        // Setup font size slider
        if (sliderFontSize != null && lblFontSize != null) {
            sliderFontSize.valueProperty().addListener((obs, oldVal, newVal) -> {
                lblFontSize.setText(String.format("%.0fpx", newVal.doubleValue()));
            });
        }
        
        // Setup theme ComboBox
        if (cmbTheme != null) {
            cmbTheme.getItems().addAll("Dark (Mặc định)", "Light (Sắp ra mắt)");
            cmbTheme.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleResetDefaults() {
        if (sliderFontSize != null) {
            sliderFontSize.setValue(13);
        }
        if (cmbTheme != null) {
            cmbTheme.getSelectionModel().selectFirst();
        }
        if (chkAutoSave != null) {
            chkAutoSave.setSelected(true);
        }
        if (chkShowNotifications != null) {
            chkShowNotifications.setSelected(true);
        }
        if (chkConfirmDelete != null) {
            chkConfirmDelete.setSelected(true);
        }
        
        showAlert("Thành công", "Đã đặt lại các tùy chọn về mặc định!", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) tabPane.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
