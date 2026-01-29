# 📓 Smart Notebook - Ứng dụng Ghi chú Thông minh

> Đồ án Java Desktop với JavaFX, SQL Server, và AI Offline

---

## 📋 MỤC LỤC

1. [Setup Nhanh (1 Click)](#-setup-nhanh-1-click)
2. [Yêu cầu Hệ thống](#-yêu-cầu-hệ-thống)
3. [Hướng dẫn Cài đặt](#-hướng-dẫn-cài-đặt)
4. [Cấu hình Database](#️-cấu-hình-database)
5. [Chạy Ứng dụng](#-chạy-ứng-dụng)
6. [Troubleshooting](#-troubleshooting)
7. [Tính năng](#-tính-năng)
8. [Công nghệ](#️-công-nghệ)
9. [Cấu trúc Project](#-cấu-trúc-project)

---

## 🚀 SETUP NHANH (1 CLICK)

### **Cho người nhận đồ án:**

```bash
1. Copy toàn bộ folder SmartNotebook
2. Double-click: setup.bat
3. Chờ 3-5 phút
4. Double-click: run.bat
```

**Script `setup.bat` tự động:**
- ✅ Kiểm tra Java
- ✅ Cài đặt Maven Wrapper
- ✅ Download tất cả dependencies (JavaFX, SQL Driver, v.v.)
- ✅ Tạo database SmartNotebook (nếu chưa có)
- ✅ Biên dịch project

---

## 💻 YÊU CẦU HỆ THỐNG

### **Option 1: Traditional Setup**
- ✅ **Windows 10/11**
- ✅ **Java 17+** → [Download JDK](https://www.oracle.com/java/technologies/downloads/#java17)
- ✅ **SQL Server** → [Download SQL Server Express](https://www.microsoft.com/en-us/sql-server/sql-server-downloads)

### **Option 2: Docker Setup** 🐋
- ✅ **Windows 10/11**
- ✅ **Java 17+** → [Download JDK](https://www.oracle.com/java/technologies/downloads/#java17)
- ✅ **Docker Desktop** → [Download Docker](https://www.docker.com/products/docker-desktop/)

> 💡 **Docker = Dễ hơn!** Không cần cài SQL Server, chỉ cần `docker-start.bat`

---

## 📥 HƯỚNG DẪN CÀI ĐẶT

### **Bước 1: Cài đặt Java**

1. Download **JDK 17**: https://www.oracle.com/java/technologies/downloads/#java17
2. Chạy installer → Next → Next → Finish
3. Kiểm tra:
   ```bash
   java -version
   # Output: java version "17.x.x"
   ```

### **Bước 2: Cài đặt SQL Server**

1. Download **SQL Server Express**: https://www.microsoft.com/en-us/sql-server/sql-server-downloads
2. Chọn **Basic** → Accept → Install
3. Nhớ thông tin:
   - Server name: `localhost` hoặc `.\SQLEXPRESS`
   - Authentication: SQL Server Authentication
   - Username: `sa`
   - Password: (tự đặt, ví dụ: `123456`)

### **Bước 3: Chạy Setup Tự Động**

```bash
# Double-click file:
setup.bat
```

**Quá trình:**
1. Kiểm tra Java ✓
2. Download Maven + dependencies (3-5 phút) ✓
3. Tạo database `SmartNotebook` ✓
4. Biên dịch project ✓

### **Bước 4: Cấu hình Database (NẾU CẦN)**

Nếu SQL Server khác cổng 1433 hoặc khác username/password:

1. Mở file `config.properties`
2. Sửa các dòng:
   ```properties
   db.host=localhost          # Địa chỉ SQL Server
   db.port=1433              # Cổng (mặc định 1433)
   db.user=sa                # Username SQL
   db.password=123456        # Password SQL
   ```

---

### **🐋 OPTION: Docker Setup (Dễ hơn!)**

Nếu muốn dùng Docker thay vì cài SQL Server thủ công:

1. **Cài Docker Desktop**: https://www.docker.com/products/docker-desktop/

2. **Start SQL Server:**
   ```batch
   docker-start.bat
   ```

3. **Initialize database:**
   ```batch
   docker-init-db.bat
   ```

4. **DONE!** Run app:
   ```batch
   run.bat
   ```

> 📘 Chi tiết: [DOCKER.md](DOCKER.md)

---

## ▶️ CHẠY ỨNG DỤNG

### **Cách 1: Double-click (Đơn giản nhất)**

```bash
run.bat
```

### **Cách 2: Command Line**

```bash
mvn javafx:run
```

### **Cách 3: IDE (IntelliJ/Eclipse)**

1. Import project as Maven project
2. Right-click `pom.xml` → Maven → Reload
3. Run main class: `com.dat.notebook.App`

---

## 🐛 TROUBLESHOOTING

### **❌ Lỗi: Cannot connect to SQL Server**

**Nguyên nhân:** SQL Server chưa chạy hoặc sai thông tin đăng nhập

**Giải pháp:**

1. **Kiểm tra SQL Server đang chạy:**
   - `Win + R` → `services.msc` → Enter
   - Tìm `SQL Server (MSSQLSERVER)`
   - Click phải → Start

2. **Kiểm tra config.properties:**
   ```properties
   db.host=localhost
   db.port=1433
   db.user=sa
   db.password=123456  # ← SỬA ĐÚNG PASSWORD
   ```

3. **Tạo database thủ công:**
   ```sql
   -- Mở SQL Server Management Studio (SSMS):
   CREATE DATABASE SmartNotebook;
   ```

---

### **❌ Lỗi: JAVA_HOME not set**

**Giải pháp:**

```bash
# Mở Command Prompt (Admin):
setx JAVA_HOME "C:\Program Files\Java\jdk-17"

# Restart CMD và test:
java -version
```

---

### **❌ Lỗi: Dependencies không download**

**Giải pháp:**

```bash
# Xóa cache Maven và download lại:
mvn clean install -U

# Hoặc xóa thủ công folder:
C:\Users\<YourName>\.m2\repository
```

---

### **❌ Lỗi: Port 1433 already in use**

**Giải pháp:**

1. Kiểm tra SQL Server đang dùng cổng nào:
   ```bash
   # SQL Server Configuration Manager
   # → SQL Server Network Configuration
   # → Protocols for MSSQLSERVER
   # → TCP/IP → IP Addresses → IPAll → TCP Port
   ```

2. Sửa `config.properties`:
   ```properties
   db.port=1434  # ← ĐỔI SANG CỔNG ĐÚNG
   ```

---

## 🎨 TÍNH NĂNG

### **📝 Quản lý Ghi chú**
- ✨ Tạo, sửa, xóa ghi chú
- 🎨 Rich Text Editor:
  - **Bold**, *Italic*, <u>Underline</u>
  - Font chữ (Arial, Times New Roman, Courier New, ...)
  - Màu chữ và highlight
  - Alignment (trái, giữa, phải)
- ⭐ Đánh dấu yêu thích
- 🗑️ Thùng rác (có thể khôi phục)
- 🔍 Tìm kiếm nhanh
- 📊 Trạng thái:
  - 📋 Regular (Bình thường)
  - 🔴 Urgent (Khẩn cấp)
  - 💡 Ideas (Ý tưởng)
  - ✅ Completed (Hoàn thành)

### **🤖 AI Assistant (Offline)**
- 📝 Tóm tắt ghi chú tự động
- 🎯 Gợi ý tiêu đề thông minh
- 🏷️ Tự động gắn tag
- 💡 Phân tích nội dung

### **👤 Quản lý Người dùng**
- 🔐 Đăng ký / Đăng nhập
- ⚙️ Cài đặt tài khoản
- 🔑 Đổi mật khẩu
- 📧 Cập nhật email

### **🎨 Giao diện**
- 🍑 **Warm Orange Theme** - Nhẹ nhàng, dễ nhìn, phù hợp làm việc lâu
- 📱 Responsive layout (3 cột: Sidebar - List - Editor)
- 🌈 SVG icons (sắc nét, không vỡ)

---

## 🛠️ CÔNG NGHỆ

| Lớp | Công nghệ | Version |
|------|-----------|---------|
| **Frontend** | JavaFX | 20 |
| **Backend** | Java | 17 |
| **Database** | SQL Server | 2019+ |
| **Build Tool** | Maven | 3.8+ |
| **AI Engine** | Custom NLP | Offline |

### **Dependencies chính:**

```xml
<!-- JavaFX -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>20</version>
</dependency>

<!-- SQL Server Driver -->
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>12.4.0.jre11</version>
</dependency>

<!-- BCrypt (mã hóa password) -->
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

---

## 📁 CẤU TRÚC PROJECT

```
SmartNotebook/
│
├── 📁 src/
│   ├── 📁 main/
│   │   ├── 📁 java/com/dat/notebook/
│   │   │   ├── 📁 controller/          # Controllers (MVC)
│   │   │   │   ├── LoginController.java
│   │   │   │   ├── MainControllerV2.java
│   │   │   │   ├── SettingsController.java
│   │   │   │   └── TrashController.java
│   │   │   │
│   │   │   ├── 📁 dao/                 # Data Access Objects
│   │   │   │   ├── UserDAO.java
│   │   │   │   └── NoteDAO.java
│   │   │   │
│   │   │   ├── 📁 model/               # Models
│   │   │   │   ├── User.java
│   │   │   │   └── Note.java
│   │   │   │
│   │   │   ├── 📁 service/             # Business Logic
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── AIEngine.java
│   │   │   │   └── NoteService.java
│   │   │   │
│   │   │   ├── 📁 util/                # Utilities
│   │   │   │   ├── DatabaseConnection.java
│   │   │   │   ├── RichTextStyleManager.java
│   │   │   │   └── ThemeManager.java
│   │   │   │
│   │   │   └── App.java                # Main Application
│   │   │
│   │   └── 📁 resources/
│   │       ├── 📁 views/               # FXML Files
│   │       │   ├── LoginView.fxml
│   │       │   ├── MainViewV2.fxml
│   │       │   ├── SettingsView.fxml
│   │       │   └── TrashView.fxml
│   │       │
│   │       ├── 📁 css/                 # Stylesheets
│   │       │   ├── warm-orange.css
│   │       │   └── soft-peach.css
│   │       │
│   │       └── 📁 database/            # SQL Scripts
│   │           └── schema.sql
│   │
│   └── 📁 test/                        # Unit Tests
│
├── 📁 target/                          # Compiled files (auto-generated)
│
├── 📄 pom.xml                          # Maven configuration
├── 📄 config.properties                # Database config
├── 📄 .gitignore                       # Git ignore rules
│
├── 🚀 setup.bat                        # Setup script
├── ▶️ run.bat                          # Run script
└── 📖 README.md                        # This file
```

---

## 📊 DATABASE SCHEMA

### **Table: users**

```sql
CREATE TABLE users (
    user_id INT PRIMARY KEY IDENTITY(1,1),
    username NVARCHAR(50) UNIQUE NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    display_name NVARCHAR(100),
    email NVARCHAR(100),
    created_at DATETIME DEFAULT GETDATE()
);
```

### **Table: notes**

```sql
CREATE TABLE notes (
    note_id INT PRIMARY KEY IDENTITY(1,1),
    user_id INT FOREIGN KEY REFERENCES users(user_id),
    title NVARCHAR(200),
    content NVARCHAR(MAX),
    status NVARCHAR(20) DEFAULT 'regular',
    is_favorite BIT DEFAULT 0,
    is_archived BIT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE()
);
```

### **Tạo Database thủ công (nếu cần):**

```sql
-- 1. Tạo database
CREATE DATABASE SmartNotebook;
GO

USE SmartNotebook;
GO

-- 2. Tạo tables (copy từ src/main/resources/database/schema.sql)
```

---

## 📦 FILES QUAN TRỌNG

| File | Công dụng |
|------|-----------|
| `setup.bat` | Script setup tự động (chạy 1 lần khi nhận đồ án) |
| `run.bat` | Script chạy app nhanh |
| `config.properties` | Cấu hình database (sửa username/password) |
| `pom.xml` | Maven config (chứa tất cả dependencies) |
| `.gitignore` | Loại bỏ file không cần thiết khi push Git |

---

## 👨‍💻 TÁC GIẢ

- **Họ tên**: Trần Tấn Đạt
- **MSSV**: [Your Student ID]
- **Trường**: [Your University]
- **Lớp**: [Your Class]
- **Email**: [Your Email]

---

## 📄 LICENSE

Dự án này dành cho mục đích học tập.

---

## 🙏 ACKNOWLEDGMENTS

- [JavaFX Documentation](https://openjfx.io/)
- [SQL Server Documentation](https://docs.microsoft.com/en-us/sql/)
- [Material Design Icons](https://material.io/icons/)
- [Maven Central Repository](https://mvnrepository.com/)

---

## 📞 HỖ TRỢ

Nếu gặp lỗi:
1. Đọc phần **[Troubleshooting](#-troubleshooting)** ở trên
2. Kiểm tra file `config.properties`
3. Chạy lại `setup.bat`
4. Liên hệ: [Your Email]

---

**Made with ❤️ using Java & JavaFX**

**Version 2.10 - Warm Orange Theme**
