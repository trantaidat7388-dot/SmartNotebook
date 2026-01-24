# Hướng Dẫn Cài Đặt Database (Khi Chuyển Máy)

Tài liệu này hướng dẫn cách thiết lập cơ sở dữ liệu cho SmartNotebook khi triển khai trên máy mới hoặc môi trường mới.

## 1. Yêu Cầu Hệ Thống

*   **Database**: SQL Server 2012 trở lên (Khuyến nghị SQL Server 2019 hoặc mới nhất)
*   **Java**: JDK 17 hoặc mới hơn
*   **Công cụ quản lý**: SQL Server Management Studio (SSMS) hoặc Azure Data Studio

## 2. Cài Đặt Cơ Sở Dữ Liệu

File script SQL nằm tại đường dẫn gốc của dự án:  
`d:\SmartNotebook\SmartNotebook_Complete.sql`

### Các bước thực hiện:

1.  Mở **SQL Server Management Studio (SSMS)** và kết nối vào SQL Server instance của bạn.
2.  Mở file `SmartNotebook_Complete.sql` trong SSMS (File > Open > File...).
3.  Nhấn nút **Execute** (hoặc phím `F5`) để chạy toàn bộ script.

**Script này sẽ tự động:**
*   Tạo database `SmartNotebook` (nếu chưa có).
*   Tạo đầy đủ các bảng: `User`, `Notes`, `Categories`, `Tags`, `NoteVersions`...
*   Tạo các Stored Procedures cần thiết.
*   Tạo dữ liệu mẫu (Sample Data) cho user `demo`, `admin` và `dat09`.

## 3. Cấu Hình Kết Nối (db.properties)

Sau khi tạo database thành công, bạn cần cập nhật file cấu hình để ứng dụng kết nối được với SQL Server trên máy mới.

Mở file: `src/main/resources/db.properties`

```properties
# Cấu hình kết nối SQL Server
db.server=localhost
db.port=1433           <-- Kiểm tra port SQL Server (mặc định là 1433)
db.database=SmartNotebook
db.username=sa         <-- Thay bằng user SQL của bạn
db.password=your_pass  <-- Thay bằng mật khẩu SQL của bạn

# Cấu hình bảo mật (BẮT BUỘC cho SQL Server mới + Java mới)
db.encrypt=true
db.trustServerCertificate=true
db.sslProtocol=TLSv1
```

> **Lưu ý quan trọng về TLS 1.0**:  
> Dự án này sử dụng một "hack" trong `DBConnection.java` để cho phép kết nối qua TLS 1.0 (nhằm tương thích ngược). Nếu bạn dùng SQL Server đời mới và muốn bảo mật tốt hơn, hãy cân nhắc cập nhật SQL Server để hỗ trợ TLS 1.2+ và bỏ cấu hình `db.sslProtocol=TLSv1`.

## 4. Kiểm Tra Kết Nối

Dự án có sẵn script để test kết nối mà không cần chạy toàn bộ ứng dụng.

1.  Mở terminal tại thư mục gốc dự án.
2.  Chạy lệnh:
    ```cmd
    .\test-connection.bat
    ```
    *Hoặc nếu dùng CMD/PowerShell trực tiếp:*
    ```powershell
    mvn clean compile exec:java -Dexec.mainClass="com.dat.notebook.util.DBConnection"
    ```

3.  Nếu thấy thông báo `✅ KẾT NỐI SQL SERVER THÀNH CÔNG!`, bạn đã sẵn sàng để chạy ứng dụng.

## 5. Troubleshooting (Sửa Lỗi Thường Gặp)

*   **Lỗi: `TCP/IP connection to the host failed`**
    *   Đảm bảo SQL Server Service đang chạy (kiểm tra trong *Services.msc*).
    *   Đảm bảo TCP/IP protocol đã được Enable trong *SQL Server Configuration Manager*.
    *   Kiểm tra lại port (mặc định 1433).

*   **Lỗi: `Login failed for user...`**
    *   Sai username hoặc password trong `db.properties`.
    *   User SQL Server chưa được cấp quyền truy cập database `SmartNotebook`.

*   **Lỗi: `The driver could not establish a secure connection using...`**
    *   Đảm bảo `db.encrypt=true` và `db.trustServerCertificate=true` trong `db.properties`.
