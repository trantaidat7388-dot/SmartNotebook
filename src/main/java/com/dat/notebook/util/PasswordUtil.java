package com.dat.notebook.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class cho việc hash mật khẩu.
 * 
 * Sử dụng MD5 để hash mật khẩu (khớp với database hiện tại).
 * ⚠️ MD5 KHÔNG AN TOÀN cho production - chỉ dùng cho học tập!
 * Production nên sử dụng BCrypt hoặc Argon2.
 * 
 * @author SmartNotebook Team
 * @version 2.0 - Sử dụng MD5 cho compatibility
 */
public class PasswordUtil {
    
    /**
     * Hash mật khẩu sử dụng MD5
     * 
     * @param password Mật khẩu gốc
     * @return Chuỗi hash (hex format, lowercase)
     */
    public static String hashPassword(String password) {
        if (password == null) {
            return null;
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes).toLowerCase(); // SQL Server HASHBYTES returns lowercase
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
    
    /**
     * Kiểm tra mật khẩu có khớp với hash không
     * 
     * @param password Mật khẩu cần kiểm tra
     * @param hash Hash để so sánh
     * @return true nếu khớp
     */
    public static boolean verifyPassword(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }
        return hashPassword(password).equalsIgnoreCase(hash);
    }
    
    /**
     * Convert byte array thành hex string
     * 
     * @param bytes Mảng bytes
     * @return Chuỗi hex
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * Validate độ mạnh mật khẩu
     * 
     * @param password Mật khẩu cần kiểm tra
     * @return Thông báo lỗi hoặc null nếu valid
     */
    public static String validatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "Mật khẩu không được để trống";
        }
        
        if (password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự";
        }
        
        if (password.length() > 50) {
            return "Mật khẩu không được quá 50 ký tự";
        }
        
        // Optional: kiểm tra có chữ và số
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        
        if (!hasLetter || !hasDigit) {
            return "Mật khẩu nên có cả chữ và số";
        }
        
        return null; // Valid
    }
    
    /**
     * Tạo mật khẩu ngẫu nhiên
     * 
     * @param length Độ dài mật khẩu
     * @return Mật khẩu ngẫu nhiên
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }
}
