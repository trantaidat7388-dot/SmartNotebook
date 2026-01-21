package com.dat.notebook.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class để kết nối SQL Server
 * Sử dụng Singleton pattern để quản lý connection
 */
public class DBConnection {

    private static final String DEFAULT_CONFIG_RESOURCE = "/db.properties";
    private static final String PLACEHOLDER_PASSWORD = "your_password";

    private static volatile DbConfig cachedConfig;

    private record DbConfig(
            String server,
            String port,
            String database,
            String user,
            String password,
            boolean encrypt,
            boolean trustServerCertificate
    ) {
        String buildConnectionUrl() {
            return String.format(
                    "jdbc:sqlserver://%s:%s;databaseName=%s;encrypt=%s;trustServerCertificate=%s;",
                    server, port, database, encrypt, trustServerCertificate
            );
        }

        boolean looksConfigured() {
            return server != null && !server.isBlank()
                    && port != null && !port.isBlank()
                    && database != null && !database.isBlank()
                    && user != null && !user.isBlank()
                    && password != null && !password.isBlank()
                    && !PLACEHOLDER_PASSWORD.equals(password);
        }
    }

    private static DbConfig loadConfig() {
        DbConfig existing = cachedConfig;
        if (existing != null) {
            return existing;
        }

        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // If config can't be read, we'll fall back to defaults/env.
        }

        String server = firstNonBlank(System.getenv("SMARTNOTEBOOK_DB_SERVER"), props.getProperty("db.server"), "localhost");
        String port = firstNonBlank(System.getenv("SMARTNOTEBOOK_DB_PORT"), props.getProperty("db.port"), "1433");
        String database = firstNonBlank(System.getenv("SMARTNOTEBOOK_DB_NAME"), props.getProperty("db.name"), "SmartNotebook");
        String user = firstNonBlank(System.getenv("SMARTNOTEBOOK_DB_USER"), props.getProperty("db.user"), "sa");
        String password = firstNonBlank(System.getenv("SMARTNOTEBOOK_DB_PASSWORD"), props.getProperty("db.password"), PLACEHOLDER_PASSWORD);

        boolean encrypt = parseBoolean(firstNonBlank(props.getProperty("db.encrypt"), "true"));
        boolean trustServerCertificate = parseBoolean(firstNonBlank(props.getProperty("db.trustServerCertificate"), "true"));

        DbConfig config = new DbConfig(server, port, database, user, password, encrypt, trustServerCertificate);
        cachedConfig = config;
        return config;
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String value : candidates) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static boolean parseBoolean(String raw) {
        return raw != null && raw.equalsIgnoreCase("true");
    }

    /**
     * Lấy connection tới SQL Server
     * 
     * @return Connection object hoặc null nếu có lỗi
     */
    public static Connection getConnection() throws SQLException {
        DbConfig config = loadConfig();
        if (!config.looksConfigured()) {
            throw new SQLException(
                    "Database chưa được cấu hình. Hãy cập nhật src/main/resources/db.properties hoặc đặt SMARTNOTEBOOK_DB_PASSWORD."
            );
        }

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            SQLException sqlEx = new SQLException("Không tìm thấy SQL Server JDBC Driver!", e);
            throw sqlEx;
        }

        return DriverManager.getConnection(config.buildConnectionUrl(), config.user(), config.password());
    }

    /**
     * Thử lấy Connection; trả về null nếu chưa cấu hình hoặc không thể kết nối.
     * Dùng để app vẫn chạy được ở chế độ offline (in-memory).
     */
    public static Connection tryGetConnection() {
        DbConfig config = loadConfig();
        if (!config.looksConfigured()) {
            return null;
        }
        try {
            return getConnection();
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Test kết nối tới SQL Server
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
     * Đóng connection
     */
    public static void closeConnection() {
        // No-op: Connection được tạo theo từng lần gọi và tự đóng bằng try-with-resources.
    }

    /**
     * Kiểm tra xem có đang kết nối không
     * 
     * @return true nếu đang kết nối
     */
    public static boolean isConnected() {
        return testConnection();
    }
}
