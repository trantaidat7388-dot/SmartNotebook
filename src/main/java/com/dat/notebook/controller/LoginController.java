package com.dat.notebook.controller;

import com.dat.notebook.model.User;
import com.dat.notebook.service.AuthService;
import com.dat.notebook.config.DatabaseConfig;
import com.dat.notebook.util.TransitionUtil;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;

import java.io.IOException;

/**
 * Controller cho màn hình đăng nhập - Gen Z Edition
 * 
 * Chức năng:
 * - Đăng nhập / Đăng ký trong 1 màn hình
 * - Animation chuyển đổi Login ↔ Register
 * - Chế độ Demo
 * 
 * @author SmartNotebook Team
 * @version 2.0
 */
public class LoginController {

    // ==================== FXML COMPONENTS - LOGIN ====================

    @FXML
    private VBox loginFormPanel;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private javafx.scene.layout.HBox errorBox;
    @FXML
    private Label errorLabel;

    // ==================== FXML COMPONENTS - REGISTER ====================

    @FXML
    private javafx.scene.control.ScrollPane registerScrollPane;
    @FXML
    private VBox registerFormPanel;
    @FXML
    private TextField regUsernameField;
    @FXML
    private TextField regEmailField;
    @FXML
    private PasswordField regPasswordField;
    @FXML
    private PasswordField regConfirmPasswordField;
    @FXML
    private javafx.scene.layout.HBox registerErrorBox;
    @FXML
    private Label registerErrorLabel;
    @FXML
    private javafx.scene.layout.HBox registerSuccessBox;
    @FXML
    private Label registerSuccessLabel;

    // ==================== SERVICES ====================

    private final AuthService authService = AuthService.getInstance();

    // ==================== INITIALIZATION ====================

    @FXML
    public void initialize() {
        // Clear error on input
        usernameField.textProperty().addListener((obs, old, newVal) -> hideError());
        passwordField.textProperty().addListener((obs, old, newVal) -> hideError());

        // Set focus to username field
        Platform.runLater(() -> usernameField.requestFocus());

        // Check database connection
        checkDatabaseConnection();
    }

    /**
     * Kiểm tra kết nối database và hiển thị cảnh báo nếu không có
     */
    private void checkDatabaseConnection() {
        if (!DatabaseConfig.testConnection()) {
            showWarning("Không thể kết nối database. Bạn có thể sử dụng chế độ Demo để trải nghiệm.");
        }
    }

    // ==================== EVENT HANDLERS ====================

    /**
     * Xử lý đăng nhập
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validate
        if (username.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập");
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Vui lòng nhập mật khẩu");
            passwordField.requestFocus();
            return;
        }

        // Try login
        User user = authService.login(username, password);

        if (user != null) {
            // Login success
            System.out.println("Đăng nhập thành công: " + user.getDisplayName());
            navigateToMainScreen();
        } else {
            // Login failed
            showError("Sai tên đăng nhập hoặc mật khẩu");
            passwordField.clear();
            passwordField.requestFocus();
        }
    }

    /**
     * Xử lý đăng nhập khách (Guest Login)
     */
    @FXML
    private void handleGuestLogin() {
        handleDemoMode();
    }

    /**
     * Xử lý chế độ Demo (không cần đăng nhập)
     */
    @FXML
    private void handleDemoMode() {
        User demoUser = authService.loginAsDemo();
        System.out.println("Đăng nhập chế độ Demo: " + demoUser.getDisplayName());
        navigateToMainScreen();
    }

    /**
     * Hiển thị form đăng ký với animation
     */
    @FXML
    private void handleShowRegister() {
        // Fade out login form
        TransitionUtil.playFadeOut(loginFormPanel, () -> {
            loginFormPanel.setVisible(false);
            loginFormPanel.setManaged(false);

            // Show register form với animation
            registerScrollPane.setVisible(true);
            registerScrollPane.setManaged(true);
            TransitionUtil.playScaleIn(registerScrollPane);
        });
    }

    /**
     * Hiển thị form đăng nhập với animation
     */
    @FXML
    private void handleShowLogin() {
        // Fade out register form
        TransitionUtil.playFadeOut(registerScrollPane, () -> {
            registerScrollPane.setVisible(false);
            registerScrollPane.setManaged(false);

            // Show login form với animation
            loginFormPanel.setVisible(true);
            loginFormPanel.setManaged(true);
            TransitionUtil.playScaleIn(loginFormPanel);
        });
    }

    /**
     * Xử lý đăng ký
     */
    @FXML
    private void handleRegister() {
        String username = regUsernameField.getText().trim();
        String email = regEmailField.getText().trim();
        String password = regPasswordField.getText();
        String confirmPassword = regConfirmPasswordField.getText();

        // Validate
        if (username.isEmpty()) {
            showRegisterError("Vui lòng nhập tên đăng nhập");
            TransitionUtil.playShakeAnimation(regUsernameField);
            return;
        }

        if (username.length() < 3) {
            showRegisterError("Tên đăng nhập phải có ít nhất 3 ký tự");
            TransitionUtil.playShakeAnimation(regUsernameField);
            return;
        }

        if (email.isEmpty()) {
            showRegisterError("Vui lòng nhập email");
            TransitionUtil.playShakeAnimation(regEmailField);
            return;
        }

        if (!email.contains("@")) {
            showRegisterError("Email không hợp lệ");
            TransitionUtil.playShakeAnimation(regEmailField);
            return;
        }

        if (password.isEmpty()) {
            showRegisterError("Vui lòng nhập mật khẩu");
            TransitionUtil.playShakeAnimation(regPasswordField);
            return;
        }

        if (password.length() < 6) {
            showRegisterError("Mật khẩu phải có ít nhất 6 ký tự");
            TransitionUtil.playShakeAnimation(regPasswordField);
            return;
        }

        if (!password.equals(confirmPassword)) {
            showRegisterError("Mật khẩu xác nhận không khớp");
            TransitionUtil.playShakeAnimation(regConfirmPasswordField);
            return;
        }

        // Try register
        com.dat.notebook.model.User newUser = authService.register(username, password, email, username);

        if (newUser != null) {
            showRegisterSuccess("Đăng ký thành công! Chuyển sang đăng nhập...");

            // Clear fields
            regUsernameField.clear();
            regEmailField.clear();
            regPasswordField.clear();
            regConfirmPasswordField.clear();

            // Pre-fill username for login
            usernameField.setText(username);

            // Delay và chuyển về login
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.5));
            pause.setOnFinished(e -> handleShowLogin());
            pause.play();
        } else {
            showRegisterError("Tên đăng nhập hoặc email đã tồn tại");
        }
    }

    // ==================== UI HELPERS - REGISTER ====================

    private void showRegisterError(String message) {
        registerErrorLabel.setText(message);
        registerErrorBox.setVisible(true);
        registerErrorBox.setManaged(true);
        registerSuccessBox.setVisible(false);
        registerSuccessBox.setManaged(false);
    }

    private void showRegisterSuccess(String message) {
        registerSuccessLabel.setText(message);
        registerSuccessBox.setVisible(true);
        registerSuccessBox.setManaged(true);
        registerErrorBox.setVisible(false);
        registerErrorBox.setManaged(false);
    }

    /**
     * Hiển thị dialog đăng ký đơn giản
     */
    private void showSimpleRegisterDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Đăng ký tài khoản");
        dialog.setHeaderText("Tạo tài khoản mới");

        // Create form
        TextField usernameField = new TextField();
        usernameField.setPromptText("Tên đăng nhập");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu (ít nhất 6 ký tự)");

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Xác nhận mật khẩu");

        TextField emailField = new TextField();
        emailField.setPromptText("Email (tùy chọn)");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Họ tên (tùy chọn)");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.getChildren().addAll(
                new Label("Tên đăng nhập *"), usernameField,
                new Label("Mật khẩu *"), passwordField,
                new Label("Xác nhận mật khẩu *"), confirmField,
                new Label("Email"), emailField,
                new Label("Họ tên"), fullNameField);
        content.setPadding(new javafx.geometry.Insets(20));

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String username = usernameField.getText().trim();
                String password = passwordField.getText();
                String confirm = confirmField.getText();
                String email = emailField.getText().trim();
                String fullName = fullNameField.getText().trim();

                // Validate
                if (username.isEmpty()) {
                    showError("Vui lòng nhập tên đăng nhập");
                    return null;
                }

                if (password.length() < 6) {
                    showError("Mật khẩu phải có ít nhất 6 ký tự");
                    return null;
                }

                if (!password.equals(confirm)) {
                    showError("Mật khẩu xác nhận không khớp");
                    return null;
                }

                // Register
                User newUser = authService.register(username, password,
                        email.isEmpty() ? null : email,
                        fullName.isEmpty() ? null : fullName);

                if (newUser != null) {
                    showInfo("Đăng ký thành công! Bạn có thể đăng nhập ngay.");
                    usernameField.setText(username);
                    passwordField.requestFocus();
                } else {
                    showError("Không thể đăng ký. Username có thể đã tồn tại.");
                }
            }
            return buttonType;
        });

        dialog.showAndWait();
    }

    /**
     * Xử lý quên mật khẩu
     */
    @FXML
    private void handleForgotPassword() {
        showInfo("Vui lòng liên hệ quản trị viên để reset mật khẩu.\n\n" +
                "Hoặc sử dụng chế độ Demo để trải nghiệm ứng dụng.");
    }

    // ==================== NAVIGATION ====================

    /**
     * Chuyển đến màn hình chính
     * Sử dụng MainViewV2.fxml với HTMLEditor
     */
    private void navigateToMainScreen() {
        try {
            // Sử dụng MainViewV2 với HTMLEditor (Rich Text Editor)
            // Nếu muốn dùng bản cũ, đổi thành "/views/MainView.fxml"
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MainViewV2.fxml"));
            Parent root = loader.load();

            Stage mainStage = new Stage();
            mainStage.setTitle("SmartNotebook - Sổ tay thông minh v2.0");

            // Kích thước vừa phải, nhưng mặc định maximize để tận dụng không gian
            Scene scene = new Scene(root, 1000, 650);
            mainStage.setScene(scene);
            mainStage.setMinWidth(900);
            mainStage.setMinHeight(550);
            mainStage.setResizable(true);
            mainStage.setMaximized(true); // Maximize mặc định khi đăng nhập

            // Show window
            mainStage.show();

            // Close login window
            Stage loginStage = (Stage) usernameField.getScene().getWindow();
            loginStage.close();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Không thể mở màn hình chính: " + e.getMessage());
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Hiển thị thông báo lỗi
     */
    private void showError(String message) {
        if (errorBox != null) {
            errorBox.setVisible(true);
            errorBox.setManaged(true);
        }
        if (errorLabel != null) {
            errorLabel.setText(message);
        }
    }

    /**
     * Hiển thị cảnh báo
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
     * Ẩn thông báo lỗi
     */
    private void hideError() {
        if (errorBox != null) {
            errorBox.setVisible(false);
            errorBox.setManaged(false);
        }
    }

    /**
     * Hiển thị dialog thông tin
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
