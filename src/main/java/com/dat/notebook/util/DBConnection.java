package com.dat.notebook.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * DBConnection - Quản lý kết nối SQL Server với TLS 1.0 (CHỈ CHO HỌC TẬP)
 * 
 * ⚠️ CẢNH BÁO BẢO MẬT:
 * Class này cấu hình JVM để chấp nhận TLS 1.0 - một giao thức đã lỗi thời và không an toàn.
 * 
 * TẠI SAO CẦN HACK NÀY?
 * - Java 8u292+ và Java 11+ vô hiệu hóa TLS 1.0/1.1 vì lý do bảo mật
 * - SQL Server 2008/2012 cũ chỉ hỗ trợ TLS 1.0
 * - Giải pháp ĐÚNG: Nâng cấp SQL Server lên phiên bản mới hơn hỗ trợ TLS 1.2+
 * - Giải pháp TẠM: Ép JVM chấp nhận TLS 1.0 (CHỈ DÙNG CHO MÔI TRƯỜNG HỌC TẬP)
 * 
 * KHÔNG SỬ DỤNG TRONG PRODUCTION!
 * 
 * @author SmartNotebook Team
 * @version 2.0 - TLS 1.0 Hack Edition
 */
public class DBConnection {
    private static String server;
    private static String port;
    private static String database;
    private static String username;
    private static String password;
    private static String encrypt;
    private static String trustServerCertificate;
    private static String sslProtocol;

    static {
        // BƯỚC 1: Kích hoạt TLS 1.0 trong JVM (HACK BẢO MẬT)
        enableLegacyTLS();
        
        // BƯỚC 2: Tải cấu hình database
        loadDatabaseConfig();
    }

    /**
     * BƯỚC 1: Kích hoạt TLS 1.0 trong JVM
     * 
     * Java mới vô hiệu hóa TLS 1.0/1.1 trong jdk.tls.disabledAlgorithms.
     * Chúng ta cần:
     * 1. Xóa TLSv1 và TLSv1.1 khỏi danh sách disabled
     * 2. Thiết lập jdk.tls.client.protocols cho phép TLSv1
     * 
     * ⚠️ VÌ SAO NGUY HIỂM?
     * - TLS 1.0 có lỗ hổng BEAST, POODLE
     * - Dễ bị tấn công man-in-the-middle
     * - Không hỗ trợ cipher suites hiện đại
     */
    private static void enableLegacyTLS() {
        System.out.println("\n⚠️  BẮT ĐẦU HACK BẢO MẬT JVM - CHỈ CHO HỌC TẬP ⚠️");
        
        // Lấy giá trị hiện tại của jdk.tls.disabledAlgorithms
        String disabledAlgorithms = Security.getProperty("jdk.tls.disabledAlgorithms");
        System.out.println("\n[TRƯỚC] jdk.tls.disabledAlgorithms:");
        System.out.println(disabledAlgorithms);
        
        // Xóa TLSv1 và TLSv1.1 khỏi danh sách disabled
        if (disabledAlgorithms != null) {
            disabledAlgorithms = disabledAlgorithms
                .replaceAll("TLSv1\\.1,?\\s*", "")  // Xóa TLSv1.1
                .replaceAll("TLSv1,?\\s*", "")       // Xóa TLSv1
                .replaceAll(",,", ",")               // Dọn dẹp dấu phẩy thừa
                .replaceAll("^,\\s*", "")            // Xóa dấu phẩy đầu
                .replaceAll(",\\s*$", "");           // Xóa dấu phẩy cuối
            
            Security.setProperty("jdk.tls.disabledAlgorithms", disabledAlgorithms);
            System.out.println("\n[SAU] jdk.tls.disabledAlgorithms:");
            System.out.println(disabledAlgorithms);
        }
        
        // Thiết lập giao thức TLS được phép sử dụng
        // Bao gồm TLSv1 để tương thích với SQL Server cũ
        System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        System.out.println("\n[THIẾT LẬP] jdk.tls.client.protocols = TLSv1,TLSv1.1,TLSv1.2");
        
        // Tắt endpoint identification (không kiểm tra hostname trong certificate)
        // Cần thiết vì SQL Server tự ký certificate
        System.setProperty("jdk.tls.trustNameService", "true");
        
        System.out.println("\n✓ Đã kích hoạt TLS 1.0 - KHÔNG SỬ DỤNG TRONG PRODUCTION!\n");
    }

    /**
     * BƯỚC 2: Tải cấu hình database từ db.properties
     */
    private static void loadDatabaseConfig() {
        Properties props = new Properties();
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                System.err.println("❌ Không tìm thấy db.properties!");
                return;
            }
            props.load(input);
            
            server = props.getProperty("db.server");
            port = props.getProperty("db.port");
            database = props.getProperty("db.database");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");
            encrypt = props.getProperty("db.encrypt", "true");
            trustServerCertificate = props.getProperty("db.trustServerCertificate", "true");
            sslProtocol = props.getProperty("db.sslProtocol", "TLSv1");
            
            System.out.println("✓ Đã tải db.properties thành công");
        } catch (IOException ex) {
            System.err.println("❌ Lỗi đọc db.properties: " + ex.getMessage());
        }
    }
    

    
    /**
     * BƯỚC 3: Tạo kết nối SQL Server với TLS 1.0
     * 
     * Chuỗi kết nối JDBC bao gồm:
     * - encrypt=true: Bắt mã hóa SSL/TLS (BẮT BUỘC với SQL Server)
     * - trustServerCertificate=true: Tin tưởng certificate tự ký của SQL Server
     * - sslProtocol=TLSv1: ÉP sử dụng TLS 1.0 (khớp với SQL Server)
     * 
     * ⚠️ VÌ SAO CẦN trustServerCertificate=true?
     * - SQL Server thường dùng self-signed certificate
     * - Không có trong trust store của Java
     * - Nếu false → lỗi "unable to find valid certification path"
     */
    public static Connection getConnection() throws SQLException {
        String url = String.format(
            "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=%s;trustServerCertificate=%s;sslProtocol=%s",
            server, port, database, encrypt, trustServerCertificate, sslProtocol
        );
        
        System.out.println("\n🔌 Đang kết nối SQL Server...");
        System.out.println("URL: " + url.replace(password, "****"));
        System.out.println("User: " + username);
        System.out.println("SSL Protocol: " + sslProtocol);
        
        return DriverManager.getConnection(url, username, password);
    }
    
    /**
     * Thử lấy Connection; trả về null nếu kết nối thất bại.
     * Dùng để app vẫn chạy được ở chế độ offline (in-memory).
     * 
     * @return Connection object hoặc null
     */
    public static Connection tryGetConnection() {
        try {
            return getConnection();
        } catch (SQLException e) {
            System.err.println("⚠ Không thể kết nối DB (chạy demo mode): " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Test kết nối database
     * 
     * @return true nếu kết nối thành công
     */
    public static boolean testConnection() {
        try (Connection conn = tryGetConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Kiểm tra xem có đang kết nối không
     * 
     * @return true nếu đang kết nối
     */
    public static boolean isConnected() {
        return testConnection();
    }
    

    
    /**
     * Test kết nối và hiển thị thông tin database
     */
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("✅ KẾT NỐI SQL SERVER THÀNH CÔNG!");
            System.out.println("=".repeat(60));
            
            // Hiển thị metadata
            System.out.println("Database: " + conn.getCatalog());
            System.out.println("JDBC Driver: " + conn.getMetaData().getDriverName());
            System.out.println("Driver Version: " + conn.getMetaData().getDriverVersion());
            
            // Kiểm tra TLS version thực tế
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT encrypt_option, protocol_type, protocol_version " +
                "FROM sys.dm_exec_connections " +
                "WHERE session_id = @@SPID"
            );
            
            if (rs.next()) {
                String encryptOption = rs.getString("encrypt_option");
                String protocolType = rs.getString("protocol_type");
                String protocolVersion = rs.getString("protocol_version");
                
                System.out.println("\n🔒 Thông tin bảo mật kết nối:");
                System.out.println("  - Encryption: " + encryptOption);
                System.out.println("  - Protocol Type: " + protocolType);
                System.out.println("  - Protocol Version: " + protocolVersion);
                
                if ("1.0".equals(protocolVersion)) {
                    System.out.println("\n⚠️  CẢNH BÁO: Đang dùng TLS 1.0 - KHÔNG AN TOÀN!");
                    System.out.println("    Chỉ dùng cho môi trường học tập/development");
                }
            }
            
            // Đếm số bảng
            rs = stmt.executeQuery(
                "SELECT COUNT(*) AS TableCount " +
                "FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_TYPE = 'BASE TABLE'"
            );
            if (rs.next()) {
                System.out.println("\n📊 Số bảng trong database: " + rs.getInt("TableCount"));
            }
            
            System.out.println("=".repeat(60) + "\n");
            
        } catch (SQLException e) {
            System.err.println("\n" + "=".repeat(60));
            System.err.println("❌ KẾT NỐI THẤT BẠI!");
            System.err.println("=".repeat(60));
            System.err.println("Error: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("ErrorCode: " + e.getErrorCode());
            System.err.println("=".repeat(60));
            e.printStackTrace();
        }
    }
}
