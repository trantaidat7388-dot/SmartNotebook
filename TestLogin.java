import com.dat.notebook.util.DBConnection;
import com.dat.notebook.util.PasswordUtil;
import java.sql.*;

/**
 * Test đăng nhập với username và password
 */
public class TestLogin {
    public static void main(String[] args) {
        // Test hash password
        System.out.println("\n=== TEST PASSWORD HASH ===");
        String testPassword1 = "admin123";
        String testPassword2 = "dat123";
        
        String hash1 = PasswordUtil.hashPassword(testPassword1);
        String hash2 = PasswordUtil.hashPassword(testPassword2);
        
        System.out.println("Password: " + testPassword1 + " => Hash: " + hash1);
        System.out.println("Password: " + testPassword2 + " => Hash: " + hash2);
        
        // Test đăng nhập
        System.out.println("\n=== TEST ĐĂNG NHẬP ===");
        
        try (Connection conn = DBConnection.getConnection()) {
            // Kiểm tra user admin
            String sql = "SELECT user_id, username, password_hash FROM [User] WHERE username = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, "admin");
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                String dbHash = rs.getString("password_hash");
                System.out.println("\nUser: admin");
                System.out.println("Hash trong DB: " + dbHash);
                System.out.println("Hash của 'admin123': " + hash1);
                System.out.println("Khớp? " + (dbHash.equalsIgnoreCase(hash1) ? "✓ CÓ" : "✗ KHÔNG"));
                
                if (dbHash.equalsIgnoreCase(hash1)) {
                    System.out.println("\n✅ CÓ THỂ ĐĂNG NHẬP BẰNG: admin / admin123");
                } else {
                    System.out.println("\n⚠ CHƯA THỂ ĐĂNG NHẬP - CẦN CHẠY UpdatePasswordHash.sql");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Lỗi: " + e.getMessage());
        }
    }
}
