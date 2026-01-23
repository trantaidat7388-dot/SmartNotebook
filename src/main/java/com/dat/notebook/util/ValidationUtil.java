package com.dat.notebook.util;

import java.util.regex.Pattern;

/**
 * ValidationUtil - Utility class cho việc validate dữ liệu đầu vào.
 * 
 * Các phương thức validate:
 * - Email
 * - Username
 * - Password
 * - Title/Content
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class ValidationUtil {
    
    // ==================== REGEX PATTERNS ====================
    
    /** Email pattern theo RFC 5322 đơn giản */
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    /** Username: 3-30 ký tự, chữ cái, số, underscore */
    private static final Pattern USERNAME_PATTERN = 
        Pattern.compile("^[a-zA-Z0-9_]{3,30}$");
    
    // ==================== STRING VALIDATION ====================
    
    /**
     * Kiểm tra chuỗi có rỗng không (null hoặc chỉ có whitespace)
     * 
     * @param str Chuỗi cần kiểm tra
     * @return true nếu rỗng
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * Kiểm tra chuỗi không rỗng
     * 
     * @param str Chuỗi cần kiểm tra
     * @return true nếu không rỗng
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * Kiểm tra độ dài chuỗi
     * 
     * @param str Chuỗi cần kiểm tra
     * @param min Độ dài tối thiểu
     * @param max Độ dài tối đa
     * @return true nếu độ dài hợp lệ
     */
    public static boolean isLengthValid(String str, int min, int max) {
        if (str == null) return min <= 0;
        int len = str.trim().length();
        return len >= min && len <= max;
    }
    
    // ==================== EMAIL VALIDATION ====================
    
    /**
     * Validate email address
     * 
     * @param email Email cần kiểm tra
     * @return null nếu valid, thông báo lỗi nếu invalid
     */
    public static String validateEmail(String email) {
        if (isEmpty(email)) {
            return "Email không được để trống";
        }
        
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Email không hợp lệ";
        }
        
        if (email.length() > 100) {
            return "Email không được quá 100 ký tự";
        }
        
        return null; // Valid
    }
    
    /**
     * Kiểm tra email có hợp lệ không
     * 
     * @param email Email cần kiểm tra
     * @return true nếu hợp lệ
     */
    public static boolean isValidEmail(String email) {
        return validateEmail(email) == null;
    }
    
    // ==================== USERNAME VALIDATION ====================
    
    /**
     * Validate username
     * 
     * @param username Username cần kiểm tra
     * @return null nếu valid, thông báo lỗi nếu invalid
     */
    public static String validateUsername(String username) {
        if (isEmpty(username)) {
            return "Tên đăng nhập không được để trống";
        }
        
        String trimmed = username.trim();
        
        if (trimmed.length() < 3) {
            return "Tên đăng nhập phải có ít nhất 3 ký tự";
        }
        
        if (trimmed.length() > 30) {
            return "Tên đăng nhập không được quá 30 ký tự";
        }
        
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            return "Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới";
        }
        
        // Không được bắt đầu bằng số
        if (Character.isDigit(trimmed.charAt(0))) {
            return "Tên đăng nhập không được bắt đầu bằng số";
        }
        
        return null; // Valid
    }
    
    /**
     * Kiểm tra username có hợp lệ không
     * 
     * @param username Username cần kiểm tra
     * @return true nếu hợp lệ
     */
    public static boolean isValidUsername(String username) {
        return validateUsername(username) == null;
    }
    
    // ==================== PASSWORD VALIDATION ====================
    
    /**
     * Validate password
     * 
     * @param password Password cần kiểm tra
     * @return null nếu valid, thông báo lỗi nếu invalid
     */
    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Mật khẩu không được để trống";
        }
        
        if (password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự";
        }
        
        if (password.length() > 50) {
            return "Mật khẩu không được quá 50 ký tự";
        }
        
        return null; // Valid
    }
    
    /**
     * Validate password với yêu cầu cao hơn (chứa chữ và số)
     * 
     * @param password Password cần kiểm tra
     * @return null nếu valid, thông báo lỗi nếu invalid
     */
    public static String validatePasswordStrong(String password) {
        String basicError = validatePassword(password);
        if (basicError != null) {
            return basicError;
        }
        
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        
        if (!hasLetter) {
            return "Mật khẩu phải chứa ít nhất 1 chữ cái";
        }
        
        if (!hasDigit) {
            return "Mật khẩu phải chứa ít nhất 1 số";
        }
        
        return null; // Valid
    }
    
    /**
     * Kiểm tra 2 password có khớp nhau không
     * 
     * @param password Password
     * @param confirmPassword Confirm password
     * @return null nếu khớp, thông báo lỗi nếu không khớp
     */
    public static String validatePasswordMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            return "Mật khẩu không được để trống";
        }
        
        if (!password.equals(confirmPassword)) {
            return "Mật khẩu xác nhận không khớp";
        }
        
        return null; // Valid
    }
    
    // ==================== NOTE VALIDATION ====================
    
    /**
     * Validate tiêu đề ghi chú
     * 
     * @param title Tiêu đề cần kiểm tra
     * @return null nếu valid, thông báo lỗi nếu invalid
     */
    public static String validateNoteTitle(String title) {
        if (isEmpty(title)) {
            return "Tiêu đề không được để trống";
        }
        
        if (title.trim().length() > 200) {
            return "Tiêu đề không được quá 200 ký tự";
        }
        
        return null; // Valid
    }
    
    /**
     * Validate nội dung ghi chú
     * 
     * @param content Nội dung cần kiểm tra
     * @return null nếu valid, thông báo lỗi nếu invalid
     */
    public static String validateNoteContent(String content) {
        // Nội dung có thể để trống
        if (content != null && content.length() > 50000) {
            return "Nội dung không được quá 50,000 ký tự";
        }
        
        return null; // Valid
    }
    
    // ==================== FORM VALIDATION RESULT ====================
    
    /**
     * Class để chứa kết quả validate form
     */
    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;
        private String fieldName;
        
        public ValidationResult(boolean valid) {
            this.valid = valid;
        }
        
        public ValidationResult(String errorMessage, String fieldName) {
            this.valid = false;
            this.errorMessage = errorMessage;
            this.fieldName = fieldName;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public String getFieldName() {
            return fieldName;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true);
        }
        
        public static ValidationResult error(String message, String field) {
            return new ValidationResult(message, field);
        }
    }
    
    // ==================== FORM VALIDATION ====================
    
    /**
     * Validate form đăng ký
     * 
     * @param username Username
     * @param email Email
     * @param password Password
     * @param confirmPassword Confirm password
     * @return ValidationResult
     */
    public static ValidationResult validateRegisterForm(
            String username, String email, String password, String confirmPassword) {
        
        String error;
        
        error = validateUsername(username);
        if (error != null) {
            return ValidationResult.error(error, "username");
        }
        
        error = validateEmail(email);
        if (error != null) {
            return ValidationResult.error(error, "email");
        }
        
        error = validatePassword(password);
        if (error != null) {
            return ValidationResult.error(error, "password");
        }
        
        error = validatePasswordMatch(password, confirmPassword);
        if (error != null) {
            return ValidationResult.error(error, "confirmPassword");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate form đăng nhập
     * 
     * @param username Username
     * @param password Password
     * @return ValidationResult
     */
    public static ValidationResult validateLoginForm(String username, String password) {
        if (isEmpty(username)) {
            return ValidationResult.error("Vui lòng nhập tên đăng nhập", "username");
        }
        
        if (isEmpty(password)) {
            return ValidationResult.error("Vui lòng nhập mật khẩu", "password");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate form tạo ghi chú
     * 
     * @param title Tiêu đề
     * @param content Nội dung
     * @return ValidationResult
     */
    public static ValidationResult validateNoteForm(String title, String content) {
        String error;
        
        error = validateNoteTitle(title);
        if (error != null) {
            return ValidationResult.error(error, "title");
        }
        
        error = validateNoteContent(content);
        if (error != null) {
            return ValidationResult.error(error, "content");
        }
        
        return ValidationResult.success();
    }
}
