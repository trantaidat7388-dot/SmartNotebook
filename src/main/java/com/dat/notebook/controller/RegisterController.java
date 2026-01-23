package com.dat.notebook.controller;

import com.dat.notebook.model.User;
import com.dat.notebook.service.UserService;
import com.dat.notebook.config.DatabaseConfig;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.io.IOException;

/**
 * Controller cho màn hình đăng ký tài khoản.
 * 
 * Chức năng:
 * - Xử lý sự kiện đăng ký tài khoản mới
 * - Validate dữ liệu đầu vào (username, password, email)
 * - Kiểm tra username/email đã tồn tại
 * - Insert user mới vào database
 * - Hiển thị thông báo cho người dùng
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class RegisterController {
    
    // ==================== FXML COMPONENTS ====================
    
    /** TextField nhập tên đăng nhập */
    @FXML private TextField usernameField;
    
    /** TextField nhập email */
    @FXML private TextField emailField;
    
    /** PasswordField nhập mật khẩu */
    @FXML private PasswordField passwordField;
    
    /** PasswordField xác nhận mật khẩu */
    @FXML private PasswordField confirmPasswordField;
    
    /** Error/Success boxes */
    @FXML private javafx.scene.layout.HBox errorBox;
    @FXML private Label errorLabel;
    @FXML private javafx.scene.layout.HBox successBox;
    @FXML private Label successLabel;
    
    /** Error labels cho từng field */
    @FXML private Label usernameError;
    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    
    // ==================== SERVICES ====================
    
    /** Service xử lý logic người dùng */
    private final UserService userService = UserService.getInstance();
    
    // ==================== INITIALIZATION ====================
    
    /**
     * Phương thức khởi tạo, được gọi sau khi FXML được load
     */
    @FXML
    public void initialize() {
        // Xóa thông báo khi người dùng nhập liệu
        usernameField.textProperty().addListener((obs, old, newVal) -> hideMessage());
        emailField.textProperty().addListener((obs, old, newVal) -> hideMessage());
        passwordField.textProperty().addListener((obs, old, newVal) -> hideMessage());
        confirmPasswordField.textProperty().addListener((obs, old, newVal) -> hideMessage());
        
        // Focus vào ô username khi mở form
        Platform.runLater(() -> usernameField.requestFocus());
        
        // Kiểm tra kết nối database
        checkDatabaseConnection();
    }
    
    /**
     * Kiểm tra kết nối database và hiển thị cảnh báo nếu không có
     */
    private void checkDatabaseConnection() {
        if (!DatabaseConfig.testConnection()) {
            showWarning("⚠️ Không thể kết nối database. Chức năng đăng ký có thể không hoạt động.");
        }
    }
    
    // ==================== EVENT HANDLERS ====================
    
    /**
     * Xử lý sự kiện nút Đăng ký
     * 
     * Quy trình:
     * 1. Lấy dữ liệu từ form
     * 2. Validate dữ liệu (rỗng, định dạng, trùng khớp)
     * 3. Kiểm tra username/email đã tồn tại
     * 4. Tạo user mới trong database
     * 5. Hiển thị thông báo kết quả
     */
    @FXML
    private void handleRegister() {
        // ===== BƯỚC 1: Lấy dữ liệu từ form =====
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // ===== BƯỚC 2: Validate dữ liệu =====
        
        // 2.1 Kiểm tra username rỗng
        if (username.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập!");
            usernameField.requestFocus();
            return;
        }
        
        // 2.2 Kiểm tra độ dài username (3-20 ký tự)
        if (username.length() < 3 || username.length() > 20) {
            showError("Tên đăng nhập phải từ 3-20 ký tự!");
            usernameField.requestFocus();
            return;
        }
        
        // 2.3 Kiểm tra username chỉ chứa chữ cái, số và dấu gạch dưới
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            showError("Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới!");
            usernameField.requestFocus();
            return;
        }
        
        // 2.4 Kiểm tra mật khẩu rỗng
        if (password.isEmpty()) {
            showError("Vui lòng nhập mật khẩu!");
            passwordField.requestFocus();
            return;
        }
        
        // 2.5 Kiểm tra độ dài mật khẩu (tối thiểu 6 ký tự)
        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự!");
            passwordField.requestFocus();
            return;
        }
        
        // 2.6 Kiểm tra xác nhận mật khẩu
        if (!password.equals(confirmPassword)) {
            showError("Mật khẩu xác nhận không khớp!");
            confirmPasswordField.requestFocus();
            confirmPasswordField.clear();
            return;
        }
        
        // 2.7 Kiểm tra định dạng email (nếu có nhập)
        if (!email.isEmpty() && !isValidEmail(email)) {
            showError("Email không hợp lệ! Ví dụ: user@example.com");
            emailField.requestFocus();
            return;
        }
        
        // ===== BƯỚC 3 & 4: Gọi UserService để đăng ký =====
        try {
            // Gọi service đăng ký
            UserService.RegistrationResult result = userService.registerUser(username, password, email);
            
            // ===== BƯỚC 5: Xử lý kết quả =====
            switch (result.getStatus()) {
                case SUCCESS:
                    // Đăng ký thành công
                    showSuccess("✅ Đăng ký thành công! Chào mừng " + username + "!");
                    showInfoDialog("Đăng ký thành công", 
                        "Tài khoản của bạn đã được tạo thành công!\n\n" +
                        "Tên đăng nhập: " + username + "\n\n" +
                        "Bạn có thể đăng nhập ngay bây giờ.");
                    
                    // Chuyển về màn hình đăng nhập sau 2 giây
                    Platform.runLater(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {}
                        handleBackToLogin();
                    });
                    break;
                    
                case USERNAME_EXISTS:
                    // Username đã tồn tại
                    showError("❌ Tên đăng nhập '" + username + "' đã được sử dụng!");
                    usernameField.requestFocus();
                    usernameField.selectAll();
                    break;
                    
                case EMAIL_EXISTS:
                    // Email đã tồn tại
                    showError("❌ Email '" + email + "' đã được đăng ký!");
                    emailField.requestFocus();
                    emailField.selectAll();
                    break;
                    
                case CONNECTION_ERROR:
                    // Lỗi kết nối database
                    showError("❌ Lỗi kết nối database! Vui lòng thử lại sau.");
                    showErrorDialog("Lỗi kết nối", 
                        "Không thể kết nối đến cơ sở dữ liệu.\n\n" +
                        "Vui lòng kiểm tra:\n" +
                        "• SQL Server đã khởi động chưa\n" +
                        "• Thông tin kết nối trong db.properties\n" +
                        "• Firewall có chặn kết nối không");
                    break;
                    
                case DATABASE_ERROR:
                    // Lỗi database (insert thất bại)
                    showError("❌ Lỗi cơ sở dữ liệu! " + result.getMessage());
                    showErrorDialog("Lỗi database", 
                        "Không thể tạo tài khoản mới.\n\n" +
                        "Chi tiết: " + result.getMessage());
                    break;
                    
                case INVALID_DATA:
                    // Dữ liệu không hợp lệ
                    showError("❌ Dữ liệu không hợp lệ! " + result.getMessage());
                    break;
                    
                default:
                    // Lỗi không xác định
                    showError("❌ Đã xảy ra lỗi không xác định!");
            }
            
        } catch (Exception e) {
            // Xử lý exception không mong đợi
            e.printStackTrace();
            showError("❌ Lỗi hệ thống: " + e.getMessage());
            showErrorDialog("Lỗi hệ thống", 
                "Đã xảy ra lỗi không mong đợi.\n\n" +
                "Chi tiết: " + e.getMessage());
        }
    }
    
    /**
     * Xử lý sự kiện nút Làm mới (Reset)
     * Xóa tất cả dữ liệu trong form
     */
    @FXML
    private void handleReset() {
        usernameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        hideMessage();
        usernameField.requestFocus();
        
        System.out.println("Form đăng ký đã được làm mới");
    }
    
    /**
     * Xử lý sự kiện nút Quay lại đăng nhập
     * Chuyển về màn hình Login
     */
    @FXML
    private void handleBackToLogin() {
        try {
            // Load LoginView.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
            Parent root = loader.load();
            
            // Lấy Stage hiện tại
            Stage stage = (Stage) usernameField.getScene().getWindow();
            
            // Đổi scene sang Login
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("SmartNotebook - Đăng nhập");
            
            System.out.println("Chuyển về màn hình đăng nhập");
            
        } catch (IOException e) {
            e.printStackTrace();
            showError("Không thể mở màn hình đăng nhập: " + e.getMessage());
        }
    }
    
    // ==================== VALIDATION METHODS ====================
    
    /**
     * Kiểm tra định dạng email hợp lệ
     * 
     * @param email Email cần kiểm tra
     * @return true nếu email hợp lệ
     */
    private boolean isValidEmail(String email) {
        // Regex kiểm tra email cơ bản
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
    
    // ==================== UI UTILITY METHODS ====================
    
    /**
     * Hiển thị thông báo lỗi (màu đỏ)
     * 
     * @param message Nội dung thông báo
     */
    private void showError(String message) {
        if (errorBox != null) {
            errorBox.setVisible(true);
            errorBox.setManaged(true);
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
        }
        if (successBox != null) {
            successBox.setVisible(false);
            successBox.setManaged(false);
        }
    }
    
    /**
     * Hiển thị thông báo thành công (màu xanh)
     * 
     * @param message Nội dung thông báo
     */
    private void showSuccess(String message) {
        if (successBox != null) {
            successBox.setVisible(true);
            successBox.setManaged(true);
        }
        if (successLabel != null) {
            successLabel.setText(message);
        }
        if (errorBox != null) {
            errorBox.setVisible(false);
            errorBox.setManaged(false);
        }
    }
    
    /**
     * Hiển thị cảnh báo (màu cam)
     * 
     * @param message Nội dung thông báo
     */
    private void showWarning(String message) {
        if (errorBox != null) {
            errorBox.setVisible(true);
            errorBox.setManaged(true);
            errorBox.setStyle("-fx-background-color: #feebc8; -fx-border-color: #f6ad55;");
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #c05621;");
        }
    }
    
    /**
     * Ẩn thông báo
     */
    private void hideMessage() {
        if (errorBox != null) {
            errorBox.setVisible(false);
            errorBox.setManaged(false);
        }
        if (successBox != null) {
            successBox.setVisible(false);
            successBox.setManaged(false);
        }
    }
    
    /**
     * Hiển thị dialog thông tin (JOptionPane equivalent)
     * 
     * @param title Tiêu đề dialog
     * @param message Nội dung thông báo
     */
    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Thông báo");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Hiển thị dialog lỗi (JOptionPane equivalent)
     * 
     * @param title Tiêu đề dialog
     * @param message Nội dung thông báo
     */
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Đã xảy ra lỗi");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Hiển thị dialog xác nhận (JOptionPane equivalent)
     * 
     * @param title Tiêu đề dialog
     * @param message Nội dung câu hỏi
     * @return true nếu người dùng chọn OK
     */
    private boolean showConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText("Xác nhận");
        alert.setContentText(message);
        
        return alert.showAndWait()
                    .filter(response -> response == ButtonType.OK)
                    .isPresent();
    }
}
