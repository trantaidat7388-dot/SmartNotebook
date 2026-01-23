package com.dat.notebook;

import com.dat.notebook.util.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Test kết nối SQL Server
 * Chạy file này để kiểm tra kết nối database
 */
public class TestConnection {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  TEST KẾT NỐI SQL SERVER");
        System.out.println("========================================\n");
        
        System.out.println("Đang kiểm tra cấu hình...");
        
        // Test 1: Try connection
        System.out.println("\n[TEST 1] Thử kết nối database...");
        Connection conn = DBConnection.tryGetConnection();
        
        if (conn == null) {
            System.err.println("❌ KHÔNG THỂ KẾT NỐI!");
            System.err.println("\nKiểm tra các bước sau:");
            System.err.println("1. SQL Server có đang chạy không?");
            System.err.println("   - Mở 'services.msc'");
            System.err.println("   - Tìm 'SQL Server (MSSQLSERVER)' hoặc instance của bạn");
            System.err.println("   - Đảm bảo status là 'Running'");
            System.err.println("\n2. Kiểm tra thông tin trong db.properties:");
            System.err.println("   - Server: LAPTOP-AJ5RKUT\\SQL");
            System.err.println("   - Database: SmartNotebookDB");
            System.err.println("   - Username: trantandatSQL");
            System.err.println("   - Password: 221761");
            System.err.println("\n3. Database 'SmartNotebookDB' đã được tạo chưa?");
            System.err.println("   - Mở SQL Server Management Studio");
            System.err.println("   - Kiểm tra database có trong danh sách");
            System.err.println("\n4. TCP/IP Protocol đã bật chưa?");
            System.err.println("   - Mở SQL Server Configuration Manager");
            System.err.println("   - SQL Server Network Configuration");
            System.err.println("   - Protocols for [Instance]");
            System.err.println("   - TCP/IP phải là 'Enabled'");
            return;
        }
        
        try {
            System.out.println("✓ Kết nối thành công!");
            
            // Test 2: Get database info
            System.out.println("\n[TEST 2] Kiểm tra thông tin database...");
            Statement stmt = conn.createStatement();
            
            // Current database
            ResultSet rs = stmt.executeQuery("SELECT DB_NAME() AS CurrentDB");
            if (rs.next()) {
                System.out.println("✓ Database hiện tại: " + rs.getString("CurrentDB"));
            }
            rs.close();
            
            // Test 3: List tables
            System.out.println("\n[TEST 3] Danh sách bảng trong database...");
            rs = stmt.executeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME"
            );
            
            int tableCount = 0;
            while (rs.next()) {
                System.out.println("  - " + rs.getString("TABLE_NAME"));
                tableCount++;
            }
            rs.close();
            
            if (tableCount == 0) {
                System.err.println("⚠ CẢNH BÁO: Database chưa có bảng nào!");
                System.err.println("  Hãy chạy script SQL để tạo bảng:");
                System.err.println("  - Mở file schema.sql trong database/");
                System.err.println("  - Chạy script trong SSMS");
            } else {
                System.out.println("✓ Tìm thấy " + tableCount + " bảng");
            }
            
            // Test 4: Test query User table
            System.out.println("\n[TEST 4] Kiểm tra bảng User...");
            try {
                rs = stmt.executeQuery("SELECT COUNT(*) AS UserCount FROM [User]");
                if (rs.next()) {
                    int count = rs.getInt("UserCount");
                    System.out.println("✓ Bảng User có " + count + " người dùng");
                    
                    if (count == 0) {
                        System.out.println("ℹ Chưa có user nào. Hãy:");
                        System.out.println("  - Đăng ký tài khoản mới trong app");
                        System.out.println("  - Hoặc dùng chế độ Demo");
                    }
                }
                rs.close();
            } catch (Exception e) {
                System.err.println("⚠ Bảng User chưa tồn tại: " + e.getMessage());
            }
            
            // Test 5: Test query Note table
            System.out.println("\n[TEST 5] Kiểm tra bảng Note...");
            try {
                rs = stmt.executeQuery("SELECT COUNT(*) AS NoteCount FROM Note");
                if (rs.next()) {
                    int count = rs.getInt("NoteCount");
                    System.out.println("✓ Bảng Note có " + count + " ghi chú");
                }
                rs.close();
            } catch (Exception e) {
                System.err.println("⚠ Bảng Note chưa tồn tại: " + e.getMessage());
            }
            
            stmt.close();
            
            System.out.println("\n========================================");
            System.out.println("  ✅ TẤT CẢ TEST HOÀN TẤT");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi test: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                    System.out.println("\n✓ Đã đóng connection");
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi đóng connection: " + e.getMessage());
            }
        }
    }
}
