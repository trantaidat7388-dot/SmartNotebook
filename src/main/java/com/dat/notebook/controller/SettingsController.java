package com.dat.notebook.controller;

import com.dat.notebook.model.User;
import com.dat.notebook.service.AuthService;
import com.dat.notebook.service.UserService;
import com.dat.notebook.util.ValidationUtil;
import com.dat.notebook.util.TransitionUtil;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controller cho trang Settings
 * 
 * Chức năng:
 * - Cập nhật thông tin cá nhân (tên hiển thị, email)
 * - Đổi mật khẩu
 * - Đăng xuất
 * - Hiển thị thông báo success/error
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class SettingsController {


    
    // Profile Fields
    @FXML private TextField txtDisplayName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtUsername;
    @FXML private Label profileStatusLabel;
    
    // Password Fields
    @FXML private PasswordField txtOldPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label passwordStatusLabel;
    
    // Notification
    @FXML private HBox notificationBox;
    @FXML private Label notificationIcon;
    @FXML private Label notificationLabel;
    

    
    private final AuthService authService = AuthService.getInstance();
    private final UserService userService = UserService.getInstance();


    
    @FXML
    public void initialize() {
        loadUserInfo();
    }
    
    /**
     * Load thông tin user hiện tại vào form
     */
    private void loadUserInfo() {
        User currentUser = authService.getCurrentUser();
        
        if (currentUser != null) {
            // Set username (read-only)
            if (txtUsername != null) {
                txtUsername.setText(currentUser.getUsername());
            }
            
            // Set display name
            if (txtDisplayName != null) {
                String displayName = currentUser.getFullName();
                if (displayName == null || displayName.trim().isEmpty()) {
                    displayName = currentUser.getUsername();
                }
                txtDisplayName.setText(displayName);
            }
            
            // Set email
            if (txtEmail != null) {
                String email = currentUser.getEmail();
                if (email == null || email.trim().isEmpty()) {
                    email = "";
                }
                txtEmail.setText(email);
            }
        }
    }


    
    /**
     * Xử lý cập nhật thông tin cá nhân (tên hiển thị, email)
     */
    @FXML
    private void handleUpdateProfile() {
        // Get input
        String displayName = txtDisplayName.getText();
        String email = txtEmail.getText();
        
        // Validate display name
        if (displayName == null || displayName.trim().isEmpty()) {
            showNotification("⚠️", "Tên hiển thị không được để trống!", "warning");
            txtDisplayName.requestFocus();
            return;
        }
        
        // Validate email if provided
        if (email != null && !email.trim().isEmpty()) {
            String emailError = ValidationUtil.validateEmail(email.trim());
            if (emailError != null) {
                showNotification("⚠️", emailError, "warning");
                txtEmail.requestFocus();
                return;
            }
        }
        
        // Update profile
        boolean success = authService.updateProfile(displayName.trim(), email.trim());
        
        if (success) {
            showNotification("✅", "Cập nhật thông tin thành công!", "success");
            showTemporaryStatus(profileStatusLabel, "✓ Đã lưu", "#48bb78");
        } else {
            showNotification("❌", "Không thể cập nhật thông tin. Vui lòng thử lại!", "error");
        }
    }
    

    
    /**
     * Xử lý đổi mật khẩu
     */
    @FXML
    private void handleChangePassword() {
        // Get input
        String oldPassword = txtOldPassword.getText();
        String newPassword = txtNewPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        
        // Validate old password
        if (oldPassword == null || oldPassword.isEmpty()) {
            showNotification("⚠️", "Vui lòng nhập mật khẩu hiện tại!", "warning");
            txtOldPassword.requestFocus();
            return;
        }
        
        // Validate new password
        String passwordError = ValidationUtil.validatePassword(newPassword);
        if (passwordError != null) {
            showNotification("⚠️", passwordError, "warning");
            txtNewPassword.requestFocus();
            return;
        }
        
        // Validate confirm password
        if (!newPassword.equals(confirmPassword)) {
            showNotification("⚠️", "Mật khẩu xác nhận không khớp!", "warning");
            txtConfirmPassword.requestFocus();
            return;
        }
        
        // Check old password same as new
        if (oldPassword.equals(newPassword)) {
            showNotification("⚠️", "Mật khẩu mới phải khác mật khẩu cũ!", "warning");
            txtNewPassword.requestFocus();
            return;
        }
        
        // Try change password
        boolean success = authService.changePassword(oldPassword, newPassword);
        
        if (success) {
            showNotification("✅", "Đổi mật khẩu thành công!", "success");
            showTemporaryStatus(passwordStatusLabel, "✓ Đã cập nhật", "#48bb78");
            
            // Clear password fields
            txtOldPassword.clear();
            txtNewPassword.clear();
            txtConfirmPassword.clear();
        } else {
            showNotification("❌", "Mật khẩu hiện tại không đúng!", "error");
            txtOldPassword.requestFocus();
        }
    }
    

    
    /**
     * Xử lý đăng xuất
     */
    @FXML
    private void handleLogout() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận đăng xuất");
        confirmAlert.setHeaderText("Bạn có chắc muốn đăng xuất?");
        confirmAlert.setContentText("Bạn sẽ cần đăng nhập lại để sử dụng ứng dụng.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Logout
                    authService.logout();
                    
                    // Close all windows and open login
                    Stage settingsStage = (Stage) txtUsername.getScene().getWindow();
                    
                    // Close main window if exists
                    Stage.getWindows().forEach(window -> {
                        if (window instanceof Stage && window != settingsStage) {
                            ((Stage) window).close();
                        }
                    });
                    
                    // Navigate to login screen
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
                    Parent root = loader.load();
                    
                    Stage loginStage = new Stage();
                    loginStage.setTitle("SmartNotebook - Đăng nhập");
                    loginStage.setScene(new Scene(root));
                    loginStage.setResizable(false);
                    loginStage.show();
                    
                    // Close settings window
                    settingsStage.close();
                    
                } catch (IOException e) {
                    showNotification("❌", "Không thể mở màn hình đăng nhập: " + e.getMessage(), "error");
                }
            }
        });
    }


    
    /**
     * Hiển thị notification với kiểu success/error/warning
     * 
     * @param icon Icon emoji
     * @param message Nội dung thông báo
     * @param type Loại: "success", "error", "warning"
     */
    private void showNotification(String icon, String message, String type) {
        if (notificationBox == null || notificationIcon == null || notificationLabel == null) {
            // Fallback to alert dialog
            Alert.AlertType alertType = Alert.AlertType.INFORMATION;
            if ("error".equals(type)) {
                alertType = Alert.AlertType.ERROR;
            } else if ("warning".equals(type)) {
                alertType = Alert.AlertType.WARNING;
            }
            showAlert("Thông báo", message, alertType);
            return;
        }
        
        // Set content
        notificationIcon.setText(icon);
        notificationLabel.setText(message);
        
        // Set style based on type
        notificationBox.getStyleClass().removeAll("notification-success", "notification-error", "notification-warning");
        
        if ("success".equals(type)) {
            notificationBox.setStyle("-fx-background-color: #c6f6d5; -fx-border-color: #48bb78; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12px;");
            notificationLabel.setStyle("-fx-text-fill: #22543d;");
        } else if ("error".equals(type)) {
            notificationBox.setStyle("-fx-background-color: #fed7d7; -fx-border-color: #f56565; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12px;");
            notificationLabel.setStyle("-fx-text-fill: #742a2a;");
        } else if ("warning".equals(type)) {
            notificationBox.setStyle("-fx-background-color: #fef5e7; -fx-border-color: #f6ad55; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 12px;");
            notificationLabel.setStyle("-fx-text-fill: #7c2d12;");
        }
        
        // Show notification
        notificationBox.setVisible(true);
        notificationBox.setManaged(true);
        
        // Auto-hide after 4 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(4));
        pause.setOnFinished(e -> {
            notificationBox.setVisible(false);
            notificationBox.setManaged(false);
        });
        pause.play();
    }
    
    /**
     * Hiển thị status tạm thời bên cạnh nút
     * 
     * @param label Label để hiển thị
     * @param text Text hiển thị
     * @param color Màu text
     */
    private void showTemporaryStatus(Label label, String text, String color) {
        if (label == null) return;
        
        label.setText(text);
        label.setStyle("-fx-text-fill: " + color + ";");
        label.setVisible(true);
        
        // Hide after 3 seconds
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            label.setText("");
            label.setVisible(false);
        });
        pause.play();
    }

    /**
     * Hiển thị alert dialog
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
