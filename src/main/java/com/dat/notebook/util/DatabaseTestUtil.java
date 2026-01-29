package com.dat.notebook.util;

import com.dat.notebook.config.DatabaseConfig;
import com.dat.notebook.model.Note;
import com.dat.notebook.model.User;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Database Test Utility - Kiểm tra kết nối và query database
 * 
 * Chức năng:
 * - Test connection
 * - Test SELECT * queries
 * - Verify data loading
 * - Log SQL errors
 * - Check UserID filtering
 * 
 * @author SmartNotebook Team
 * @version 1.0
 */
public class DatabaseTestUtil {
    

    
    /**
     * Test kết nối database cơ bản
     * 
     * @return true nếu kết nối thành công
     */
    public static boolean testBasicConnection() {
        System.out.println("\n========================================");
        System.out.println("🔍 TESTING DATABASE CONNECTION");
        System.out.println("========================================\n");
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                DatabaseMetaData metaData = conn.getMetaData();
                System.out.println("✅ KẾT NỐI THÀNH CÔNG!");
                System.out.println("   Database: " + metaData.getDatabaseProductName());
                System.out.println("   Version: " + metaData.getDatabaseProductVersion());
                System.out.println("   URL: " + metaData.getURL());
                System.out.println("   Driver: " + metaData.getDriverName());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ KHÔNG THỂ KẾT NỐI DATABASE!");
            System.err.println("   Error: " + e.getMessage());
            System.err.println("   SQL State: " + e.getSQLState());
            System.err.println("   Error Code: " + e.getErrorCode());
            e.printStackTrace();
        }
        return false;
    }
    

    
    /**
     * Kiểm tra cấu trúc bảng Notes
     */
    public static void checkNotesTableStructure() {
        System.out.println("\n========================================");
        System.out.println("🔍 CHECKING NOTES TABLE STRUCTURE");
        System.out.println("========================================\n");
        
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE " +
                     "FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_NAME = 'Notes' " +
                     "ORDER BY ORDINAL_POSITION";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("📋 NOTES TABLE COLUMNS:");
            System.out.println("─".repeat(80));
            System.out.printf("%-20s %-15s %-10s %-10s%n", "Column", "Type", "Length", "Nullable");
            System.out.println("─".repeat(80));
            
            int count = 0;
            while (rs.next()) {
                count++;
                String columnName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("DATA_TYPE");
                String length = rs.getString("CHARACTER_MAXIMUM_LENGTH");
                String nullable = rs.getString("IS_NULLABLE");
                
                System.out.printf("%-20s %-15s %-10s %-10s%n", 
                    columnName, 
                    dataType, 
                    length != null ? length : "-",
                    nullable
                );
            }
            
            System.out.println("─".repeat(80));
            System.out.println("✅ Total columns: " + count);
            
        } catch (SQLException e) {
            System.err.println("❌ ERROR CHECKING TABLE STRUCTURE!");
            logSQLException(e);
        }
    }
    
    /**
     * Kiểm tra cấu trúc bảng User
     */
    public static void checkUserTableStructure() {
        System.out.println("\n========================================");
        System.out.println("🔍 CHECKING USER TABLE STRUCTURE");
        System.out.println("========================================\n");
        
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE " +
                     "FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_NAME = 'User' " +
                     "ORDER BY ORDINAL_POSITION";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("📋 USER TABLE COLUMNS:");
            System.out.println("─".repeat(80));
            System.out.printf("%-20s %-15s %-10s %-10s%n", "Column", "Type", "Length", "Nullable");
            System.out.println("─".repeat(80));
            
            int count = 0;
            while (rs.next()) {
                count++;
                String columnName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("DATA_TYPE");
                String length = rs.getString("CHARACTER_MAXIMUM_LENGTH");
                String nullable = rs.getString("IS_NULLABLE");
                
                System.out.printf("%-20s %-15s %-10s %-10s%n", 
                    columnName, 
                    dataType, 
                    length != null ? length : "-",
                    nullable
                );
            }
            
            System.out.println("─".repeat(80));
            System.out.println("✅ Total columns: " + count);
            
        } catch (SQLException e) {
            System.err.println("❌ ERROR CHECKING TABLE STRUCTURE!");
            logSQLException(e);
        }
    }
    

    
    /**
     * Test SELECT * FROM User
     */
    public static void testSelectAllUsers() {
        System.out.println("\n========================================");
        System.out.println("🔍 TEST SELECT * FROM USER");
        System.out.println("========================================\n");
        
        String sql = "SELECT * FROM [User]";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("📋 ALL USERS:");
            System.out.println("─".repeat(100));
            System.out.printf("%-5s %-15s %-35s %-30s %-10s%n", 
                "ID", "Username", "Email", "Full Name", "Active");
            System.out.println("─".repeat(100));
            
            int count = 0;
            while (rs.next()) {
                count++;
                int id = rs.getInt("user_id");
                String username = rs.getString("username");
                String email = rs.getString("email");
                String fullName = rs.getString("full_name");
                boolean active = rs.getBoolean("is_active");
                
                System.out.printf("%-5d %-15s %-35s %-30s %-10s%n", 
                    id, 
                    username, 
                    email != null ? email : "(null)",
                    fullName != null ? fullName : "(null)",
                    active ? "Yes" : "No"
                );
            }
            
            System.out.println("─".repeat(100));
            System.out.println("✅ Total users: " + count);
            
        } catch (SQLException e) {
            System.err.println("❌ ERROR SELECTING USERS!");
            logSQLException(e);
        }
    }
    
    /**
     * Test SELECT * FROM Notes
     */
    public static void testSelectAllNotes() {
        System.out.println("\n========================================");
        System.out.println("🔍 TEST SELECT * FROM NOTES");
        System.out.println("========================================\n");
        
        String sql = "SELECT * FROM Notes ORDER BY UpdatedAt DESC";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("📋 ALL NOTES:");
            System.out.println("─".repeat(120));
            System.out.printf("%-5s %-8s %-30s %-15s %-10s %-10s%n", 
                "ID", "UserID", "Title", "Status", "Favorite", "Archived");
            System.out.println("─".repeat(120));
            
            int count = 0;
            while (rs.next()) {
                count++;
                int id = rs.getInt("NoteID");
                int userId = rs.getInt("UserID");
                String title = rs.getString("Title");
                String status = rs.getString("Status");
                boolean favorite = rs.getBoolean("IsFavorite");
                boolean archived = rs.getBoolean("IsArchived");
                
                // Truncate title if too long
                if (title != null && title.length() > 27) {
                    title = title.substring(0, 27) + "...";
                }
                
                System.out.printf("%-5d %-8d %-30s %-15s %-10s %-10s%n", 
                    id, 
                    userId,
                    title != null ? title : "(no title)",
                    status != null ? status : "REGULAR",
                    favorite ? "⭐" : "",
                    archived ? "🗑️" : ""
                );
            }
            
            System.out.println("─".repeat(120));
            System.out.println("✅ Total notes: " + count);
            
        } catch (SQLException e) {
            System.err.println("❌ ERROR SELECTING NOTES!");
            logSQLException(e);
        }
    }
    
    /**
     * Test SELECT Notes theo UserID cụ thể
     * 
     * @param userId ID của user cần test
     */
    public static void testSelectNotesByUserId(int userId) {
        System.out.println("\n========================================");
        System.out.println("🔍 TEST SELECT NOTES BY USERID = " + userId);
        System.out.println("========================================\n");
        
        String sql = "SELECT * FROM Notes WHERE UserID = ? AND IsArchived = 0 ORDER BY UpdatedAt DESC";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("📋 NOTES FOR USER " + userId + ":");
                System.out.println("─".repeat(120));
                System.out.printf("%-5s %-30s %-15s %-10s %-20s%n", 
                    "ID", "Title", "Status", "Favorite", "Updated At");
                System.out.println("─".repeat(120));
                
                int count = 0;
                while (rs.next()) {
                    count++;
                    int id = rs.getInt("NoteID");
                    String title = rs.getString("Title");
                    String status = rs.getString("Status");
                    boolean favorite = rs.getBoolean("IsFavorite");
                    Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
                    
                    // Truncate title if too long
                    if (title != null && title.length() > 27) {
                        title = title.substring(0, 27) + "...";
                    }
                    
                    System.out.printf("%-5d %-30s %-15s %-10s %-20s%n", 
                        id,
                        title != null ? title : "(no title)",
                        status != null ? status : "REGULAR",
                        favorite ? "⭐" : "",
                        updatedAt != null ? updatedAt.toString().substring(0, 19) : "(null)"
                    );
                }
                
                System.out.println("─".repeat(120));
                System.out.println("✅ Total notes for user " + userId + ": " + count);
                
                if (count == 0) {
                    System.out.println("\n⚠️  WARNING: No notes found for user " + userId);
                    System.out.println("   Possible reasons:");
                    System.out.println("   - User has no notes in database");
                    System.out.println("   - All notes are archived");
                    System.out.println("   - UserID incorrect");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ ERROR SELECTING NOTES BY USERID!");
            logSQLException(e);
        }
    }
    
    /**
     * Test COUNT notes theo UserID
     * 
     * @param userId ID của user
     */
    public static void testCountNotesByUserId(int userId) {
        System.out.println("\n========================================");
        System.out.println("🔍 TEST COUNT NOTES BY USERID = " + userId);
        System.out.println("========================================\n");
        
        String sqlActive = "SELECT COUNT(*) FROM Notes WHERE UserID = ? AND IsArchived = 0";
        String sqlArchived = "SELECT COUNT(*) FROM Notes WHERE UserID = ? AND IsArchived = 1";
        String sqlTotal = "SELECT COUNT(*) FROM Notes WHERE UserID = ?";
        
        try (Connection conn = DatabaseConfig.getConnection()) {
            
            int activeCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlActive)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        activeCount = rs.getInt(1);
                    }
                }
            }
            
            int archivedCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlArchived)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        archivedCount = rs.getInt(1);
                    }
                }
            }
            
            int totalCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlTotal)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalCount = rs.getInt(1);
                    }
                }
            }
            
            System.out.println("📊 NOTE COUNT FOR USER " + userId + ":");
            System.out.println("   Active notes: " + activeCount);
            System.out.println("   Archived notes: " + archivedCount);
            System.out.println("   Total notes: " + totalCount);
            
        } catch (SQLException e) {
            System.err.println("❌ ERROR COUNTING NOTES!");
            logSQLException(e);
        }
    }
    

    
    /**
     * Verify notes không bị load nhầm của user khác
     */
    public static void verifyUserIsolation() {
        System.out.println("\n========================================");
        System.out.println("🔍 VERIFY USER ISOLATION");
        System.out.println("========================================\n");
        
        // Get all users
        String sqlUsers = "SELECT user_id, username FROM [User] WHERE is_active = 1";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlUsers)) {
            
            System.out.println("📋 CHECKING ISOLATION FOR EACH USER:");
            System.out.println("─".repeat(80));
            
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String username = rs.getString("username");
                
                // Count notes for this user
                String sqlCount = "SELECT COUNT(*) FROM Notes WHERE UserID = ? AND IsArchived = 0";
                try (PreparedStatement ps = conn.prepareStatement(sqlCount)) {
                    ps.setInt(1, userId);
                    try (ResultSet rsCount = ps.executeQuery()) {
                        if (rsCount.next()) {
                            int count = rsCount.getInt(1);
                            System.out.printf("User %-15s (ID=%d) has %d active notes%n", 
                                username, userId, count);
                        }
                    }
                }
            }
            
            System.out.println("─".repeat(80));
            System.out.println("✅ USER ISOLATION VERIFIED");
            
        } catch (SQLException e) {
            System.err.println("❌ ERROR VERIFYING ISOLATION!");
            logSQLException(e);
        }
    }
    

    
    /**
     * Log chi tiết SQL Exception
     * 
     * @param e SQLException cần log
     */
    public static void logSQLException(SQLException e) {
        System.err.println("\n🔴 SQL EXCEPTION DETAILS:");
        System.err.println("─".repeat(80));
        System.err.println("Message: " + e.getMessage());
        System.err.println("SQL State: " + e.getSQLState());
        System.err.println("Error Code: " + e.getErrorCode());
        
        // Chain exceptions
        SQLException next = e.getNextException();
        int count = 1;
        while (next != null) {
            System.err.println("\nChained Exception " + count + ":");
            System.err.println("Message: " + next.getMessage());
            System.err.println("SQL State: " + next.getSQLState());
            System.err.println("Error Code: " + next.getErrorCode());
            next = next.getNextException();
            count++;
        }
        
        System.err.println("\nStack Trace:");
        e.printStackTrace();
        System.err.println("─".repeat(80));
    }
    
    /**
     * Run all tests
     */
    public static void runAllTests() {
        System.out.println("\n");
        System.out.println("═".repeat(100));
        System.out.println("          DATABASE COMPREHENSIVE TEST SUITE");
        System.out.println("═".repeat(100));
        
        // Test 1: Connection
        testBasicConnection();
        
        // Test 2: Table Structure
        checkUserTableStructure();
        checkNotesTableStructure();
        
        // Test 3: Data
        testSelectAllUsers();
        testSelectAllNotes();
        
        // Test 4: UserID filtering
        System.out.println("\n\n--- TESTING USERID FILTERING ---");
        testCountNotesByUserId(1);  // Test với user ID 1
        testSelectNotesByUserId(1);
        
        // Test 5: Security
        verifyUserIsolation();
        
        System.out.println("\n");
        System.out.println("═".repeat(100));
        System.out.println("          TEST SUITE COMPLETED");
        System.out.println("═".repeat(100));
        System.out.println("\n");
    }
    
    /**
     * Main method để chạy tests standalone
     */
    public static void main(String[] args) {
        runAllTests();
    }
}
