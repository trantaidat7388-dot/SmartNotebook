# 🗄️ Database Setup Guide - SmartNotebook

Hướng dẫn setup database SQL Server cho ứng dụng SmartNotebook.

---

## 📋 Yêu cầu

- **SQL Server 2019** trở lên (hoặc SQL Server Express - miễn phí)
- **SQL Server Management Studio (SSMS)** hoặc **Azure Data Studio**
- Port mặc định: **1433**

---

## 🚀 Cài đặt SQL Server

### Option 1: SQL Server Express (Miễn phí)

1. Download từ [Microsoft SQL Server Downloads](https://www.microsoft.com/en-us/sql-server/sql-server-downloads)
2. Chọn **Express Edition** → Download
3. Chạy installer, chọn **Basic Installation**
4. Ghi nhớ **Server name** (thường là `localhost\SQLEXPRESS`)

### Option 2: SQL Server Developer Edition

1. Download SQL Server Developer từ link trên
2. Cài đặt với cấu hình mặc định
3. Server name thường là `localhost`

---

## 📝 Bước 1: Chạy Database Script

### Cách 1: Sử dụng SSMS

1. Mở **SQL Server Management Studio**
2. Kết nối đến server:
   - **Server name**: `localhost` hoặc `localhost\SQLEXPRESS`
   - **Authentication**: Windows Authentication (hoặc SQL Server Authentication)
3. Mở file `database.sql`:
   - File → Open → File → chọn `database.sql`
4. Nhấn **Execute** (F5) để chạy script
5. Kiểm tra console - phải thấy các dòng:
   ```
   ✓ Created database: SmartNotebook
   ✓ Created table: [User]
   ✓ Created table: Notes
   ...
   ✓ Sample data created
   ```

### Cách 2: Sử dụng Azure Data Studio

1. Mở **Azure Data Studio**
2. Kết nối đến SQL Server
3. Right-click server → New Query
4. Copy nội dung file `database.sql` và paste vào
5. Nhấn **Run** để execute

### Cách 3: Command Line (sqlcmd)

```bash
sqlcmd -S localhost -i database.sql
```

---

## 🔌 Bước 2: Cấu hình kết nối trong Java

Sau khi setup database, bạn cần cập nhật connection string trong code Java.

### File cần sửa

**`src/main/java/com/dat/notebook/config/DatabaseConnection.java`**

```java
private static final String SERVER = "localhost";  // Hoặc "localhost\\SQLEXPRESS"
private static final String PORT = "1433";
private static final String DATABASE = "SmartNotebook";
private static final String USER = "sa";           // Username SQL Server
private static final String PASSWORD = "your_password";  // Password của bạn
```

### Các trường hợp thường gặp

#### 1. SQL Server Express với Windows Authentication

```java
private static final String SERVER = "localhost\\SQLEXPRESS";
private static final String DATABASE = "SmartNotebook";

// Connection string
private static final String URL = String.format(
    "jdbc:sqlserver://%s;databaseName=%s;integratedSecurity=true;encrypt=false",
    SERVER, DATABASE
);
```

#### 2. SQL Server với SQL Authentication

```java
private static final String SERVER = "localhost";
private static final String DATABASE = "SmartNotebook";
private static final String USER = "sa";
private static final String PASSWORD = "YourPassword123";

private static final String URL = String.format(
    "jdbc:sqlserver://%s;databaseName=%s;user=%s;password=%s;encrypt=false",
    SERVER, DATABASE, USER, PASSWORD
);
```

---

## 🧪 Bước 3: Test kết nối

### Cách 1: Chạy ứng dụng

```bash
mvn javafx:run
```

Nếu kết nối thành công, bạn sẽ thấy màn hình Login.

### Cách 2: Kiểm tra trong SSMS

```sql
USE SmartNotebook;

-- Kiểm tra users
SELECT * FROM [User];

-- Kiểm tra notes
SELECT COUNT(*) AS TotalNotes FROM Notes;

-- Kiểm tra stored procedures
SELECT name FROM sys.procedures WHERE name LIKE 'sp_%';
```

**Kết quả mong đợi:**
- 3 users (demo, admin, dat09)
- Có sample notes
- 11 stored procedures

---

## 🔄 Khi đổi sang máy mới

### Bước 1: Export database (trên máy cũ)

**Option A: Backup database**
```sql
BACKUP DATABASE SmartNotebook 
TO DISK = 'D:\SmartNotebook_Backup.bak'
WITH FORMAT;
```

**Option B: Script toàn bộ data**
- Right-click database → Tasks → Generate Scripts
- Chọn "Script data" để export cả structure và data

### Bước 2: Import vào máy mới

**Option A: Restore từ backup**
```sql
RESTORE DATABASE SmartNotebook
FROM DISK = 'D:\SmartNotebook_Backup.bak'
WITH REPLACE;
```

**Option B: Chạy lại `database.sql`**
- Đơn giản hơn, chỉ cần chạy lại script `database.sql`
- Database sẽ được tạo lại với sample data

### Bước 3: Update connection trong code

Cập nhật lại `DatabaseConnection.java` với thông tin server mới.

---

## ❗ Troubleshooting

### Lỗi: "Cannot open database SmartNotebook"

**Giải pháp:**
```sql
-- Tạo database thủ công
CREATE DATABASE SmartNotebook;
GO

-- Sau đó chạy phần còn lại của script
```

### Lỗi: "Login failed for user 'sa'"

**Giải pháp:**
1. Mở SSMS → Connect với Windows Authentication
2. Security → Logins → sa → Properties
3. Đặt password mới
4. Status → Login: **Enabled**
5. Server Properties → Security → SQL Server and Windows Authentication mode

### Lỗi: "Connection refused" hoặc "Cannot connect"

**Kiểm tra:**
1. SQL Server service có đang chạy không?
   ```bash
   # Mở Services (services.msc)
   # Tìm "SQL Server (SQLEXPRESS)" hoặc "SQL Server (MSSQLSERVER)"
   # Start nếu chưa chạy
   ```

2. TCP/IP có enabled không?
   - SQL Server Configuration Manager
   - SQL Server Network Configuration
   - Protocols for SQLEXPRESS
   - TCP/IP → **Enabled**

3. Port 1433 có mở không?
   ```bash
   netstat -an | findstr "1433"
   ```

### Lỗi: "The driver could not establish a secure connection"

**Giải pháp:** Thêm `;encrypt=false` vào connection string
```java
"jdbc:sqlserver://localhost;databaseName=SmartNotebook;encrypt=false"
```

---

## 📊 Database Structure

**7 Tables:**
- `[User]` - Người dùng
- `Categories` - Danh mục
- `Notes` - Ghi chú (bảng chính)
- `Tags` - Thẻ tag
- `NoteTags` - Quan hệ Note-Tag
- `NoteVersions` - Lịch sử phiên bản
- `AutoSaveSnapshots` - Auto-save tạm

**11 Stored Procedures:**
- CRUD: `sp_GetNotesByUser`, `sp_CreateNote`, `sp_UpdateNote`, `sp_DeleteNote`, etc.
- Version: `sp_CreateNoteVersion`, `sp_GetNoteVersionHistory`, `sp_RollbackToVersion`

**1 View:**
- `vw_NotesWithVersions`

---

## 🎓 Tài khoản demo

Sau khi chạy script `database.sql`, database sẽ có sẵn 3 tài khoản:

| Username | Password | Mô tả |
|----------|----------|-------|
| demo | abc123 | Tài khoản demo |
| admin | admin123 | Quản trị viên |
| dat09 | 221761 | Tài khoản cá nhân |

---

**Nếu gặp vấn đề, liên hệ:** [trantaidat7388@gmail.com](mailto:trantaidat7388@gmail.com)
