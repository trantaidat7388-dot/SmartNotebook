import com.dat.notebook.util.DBConnection;
import com.dat.notebook.util.PasswordUtil;
import java.sql.*;

/**
 * Update password hash trong database
 */
public class UpdatePasswordHashJava {
    public static void main(String[] args) {
        System.out.println("\n=== CẬP NHẬT PASSWORD HASH ===\n");
        
        try (Connection conn = DBConnection.getConnection()) {
            // Hash passwords
            String hash1 = PasswordUtil.hashPassword("admin123");
            String hash2 = PasswordUtil.hashPassword("dat123");
            
            System.out.println("Hash cho 'admin123': " + hash1);
            System.out.println("Hash cho 'dat123': " + hash2);
            
            // Update password hash
            String updateSql = "UPDATE [User] SET password_hash = ? WHERE username = ?";
            
            PreparedStatement ps1 = conn.prepareStatement(updateSql);
            ps1.setString(1, hash1);
            ps1.setString(2, "admin");
            int rows1 = ps1.executeUpdate();
            
            PreparedStatement ps2 = conn.prepareStatement(updateSql);
            ps2.setString(1, hash2);
            ps2.setString(2, "dat");
            int rows2 = ps2.executeUpdate();
            
            System.out.println("\n✓ Đã cập nhật " + rows1 + " user: admin");
            System.out.println("✓ Đã cập nhật " + rows2 + " user: dat");
            
            // Verify
            System.out.println("\n=== KIỂM TRA ===\n");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT user_id, username, password_hash FROM [User]");
            
            while (rs.next()) {
                System.out.println("User " + rs.getInt("user_id") + ": " + 
                                 rs.getString("username") + " => " + 
                                 rs.getString("password_hash"));
            }
            
            System.out.println("\n✅ HOÀN TẤT! Bây giờ có thể đăng nhập:");
            System.out.println("   - Username: admin, Password: admin123");
            System.out.println("   - Username: dat, Password: dat123");
            
        } catch (SQLException e) {
            System.err.println("Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
