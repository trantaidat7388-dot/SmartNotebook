import com.dat.notebook.util.DBConnection;
import java.sql.*;

public class CheckUserTable {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("\n=== KIỂM TRA BẢNG USER ===\n");
            
            // Kiểm tra bảng tồn tại
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) AS Count FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'User'"
            );
            
            if (rs.next() && rs.getInt("Count") > 0) {
                System.out.println("✓ Bảng [User] tồn tại");
                
                // Lấy thông tin cột
                System.out.println("\nCấu trúc bảng:");
                rs = stmt.executeQuery(
                    "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE " +
                    "FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_NAME = 'User' " +
                    "ORDER BY ORDINAL_POSITION"
                );
                while (rs.next()) {
                    System.out.println("  - " + rs.getString("COLUMN_NAME") +
                                     " (" + rs.getString("DATA_TYPE") + ")" +
                                     (rs.getString("IS_NULLABLE").equals("NO") ? " NOT NULL" : ""));
                }
                
                // Đếm số user
                rs = stmt.executeQuery("SELECT COUNT(*) AS UserCount FROM [User]");
                if (rs.next()) {
                    int count = rs.getInt("UserCount");
                    System.out.println("\nSố lượng user: " + count);
                    
                    if (count == 0) {
                        System.out.println("\n⚠ BẢNG USER TRỐNG - CHƯA CÓ TÀI KHOẢN!");
                        System.out.println("\nBạn cần:");
                        System.out.println("1. Chạy script tạo user mẫu");
                        System.out.println("2. Hoặc dùng chế độ Demo");
                        System.out.println("3. Hoặc đăng ký tài khoản mới");
                    } else {
                        // Hiển thị 5 dòng đầu
                        System.out.println("\nDữ liệu mẫu (5 dòng đầu):");
                        rs = stmt.executeQuery("SELECT TOP 5 * FROM [User]");
                        ResultSetMetaData meta = rs.getMetaData();
                        int columnCount = meta.getColumnCount();
                        
                        while (rs.next()) {
                            System.out.print("  - ");
                            for (int i = 1; i <= columnCount; i++) {
                                System.out.print(meta.getColumnName(i) + "=" + rs.getString(i));
                                if (i < columnCount) System.out.print(", ");
                            }
                            System.out.println();
                        }
                    }
                }
            } else {
                System.out.println("❌ BẢNG [User] KHÔNG TỒN TẠI!");
                System.out.println("Cần chạy script tạo bảng trước.");
            }
            
        } catch (SQLException e) {
            System.err.println("Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
