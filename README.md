# 📓 SMART NOTEBOOK - SỔ TAY THÔNG MINH

> **Đồ án Lập trình Java - Ứng dụng Desktop**  
> Phiên bản: 1.0 | Java 17 + JavaFX 21 + SQL Server

---

## 📋 Mục lục

1. [Giới thiệu](#-giới-thiệu)
2. [Tính năng](#-tính-năng)
3. [Kiến trúc](#-kiến-trúc)
4. [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
5. [Cấu trúc thư mục](#-cấu-trúc-thư-mục)
6. [Hướng dẫn cài đặt](#-hướng-dẫn-cài-đặt)
7. [Hướng dẫn sử dụng](#-hướng-dẫn-sử-dụng)
8. [Database Schema](#-database-schema)
9. [Sơ đồ UML](#-sơ-đồ-uml)
10. [Thuyết trình đồ án](#-thuyết-trình-đồ-án)
11. [Tác giả](#-tác-giả)

---

## 📖 Giới thiệu

**SmartNotebook** là ứng dụng Desktop quản lý ghi chú thông minh, được phát triển bằng Java với giao diện JavaFX hiện đại. Ứng dụng không chỉ cung cấp các chức năng CRUD cơ bản mà còn tích hợp các **tính năng thông minh (Smart Features)** giúp người dùng quản lý ghi chú hiệu quả hơn.

### 🎯 Mục tiêu dự án

- Xây dựng ứng dụng Desktop hoàn chỉnh theo chuẩn đồ án
- Áp dụng kiến trúc phân lớp (Layered Architecture)
- Tích hợp tính năng "thông minh" vào nghiệp vụ
- Đảm bảo code chất lượng theo chuẩn OOP

---

## ✨ Tính năng

### 🔐 Quản lý người dùng
- Đăng nhập / Đăng ký tài khoản
- Mã hóa mật khẩu (SHA-256)
- Chế độ Demo (không cần database)

### 📝 Quản lý ghi chú
- Tạo, sửa, xóa ghi chú
- Phân loại theo trạng thái: Regular, Urgent, Ideas, Completed
- Đánh dấu yêu thích
- Phân loại theo danh mục (Category)
- Gắn nhãn/tag cho ghi chú

### 🧠 Tính năng SMART (Thông minh)

| Tính năng | Mô tả |
|-----------|-------|
| **Auto Summary** | Tự động tóm tắt nội dung ghi chú dài |
| **Smart Title** | Gợi ý tiêu đề nếu người dùng để trống |
| **Keyword Extraction** | Trích xuất từ khóa quan trọng |
| **Highlight Search** | Làm nổi bật từ khóa khi tìm kiếm |
| **Tag Suggestion** | Gợi ý tags dựa trên nội dung |

### 🔍 Tìm kiếm nâng cao
- Tìm theo tiêu đề, nội dung, tóm tắt
- Lọc theo trạng thái, danh mục
- Lọc ghi chú yêu thích

### 📊 Thống kê
- Đếm số ghi chú theo trạng thái
- Thống kê ghi chú yêu thích

---

## 🏗 Kiến trúc

Dự án được xây dựng theo **Kiến trúc phân lớp (Layered Architecture)**:

```
┌─────────────────────────────────────────┐
│              UI Layer                   │
│   (FXML Views + Controllers)            │
├─────────────────────────────────────────┤
│           Service Layer                 │
│   (Business Logic + Smart Features)     │
├─────────────────────────────────────────┤
│         Repository Layer                │
│        (Data Access - JDBC)             │
├─────────────────────────────────────────┤
│           Model Layer                   │
│    (Entities: User, Note, Tag...)       │
├─────────────────────────────────────────┤
│          Database Layer                 │
│           (SQL Server)                  │
└─────────────────────────────────────────┘
```

### Luồng dữ liệu

```
UI Controller → Service → Repository → Database
      ↑                                    ↓
      └──────────── Response ←─────────────┘
```

---

## 🛠 Công nghệ sử dụng

| Công nghệ | Phiên bản | Mục đích |
|-----------|-----------|----------|
| Java | 17 | Ngôn ngữ lập trình chính |
| JavaFX | 21 | Framework giao diện Desktop |
| SQL Server | 2019+ | Cơ sở dữ liệu quan hệ |
| JDBC | 6.4.0 | Kết nối database |
| Maven | 3.x | Quản lý dependencies |
| CSS | 3 | Styling giao diện |

---

## 📁 Cấu trúc thư mục

```
SmartNotebook/
├── database/
│   └── schema.sql              # Script tạo database
├── src/
│   └── main/
│       ├── java/
│       │   └── com/dat/notebook/
│       │       ├── App.java            # Entry point
│       │       ├── config/
│       │       │   └── DatabaseConfig.java
│       │       ├── controller/
│       │       │   ├── LoginController.java
│       │       │   ├── MainController.java
│       │       │   ├── NewNoteDialogController.java
│       │       │   └── SettingsController.java
│       │       ├── model/
│       │       │   ├── User.java
│       │       │   ├── Note.java
│       │       │   ├── Tag.java
│       │       │   └── Category.java
│       │       ├── repository/
│       │       │   ├── UserRepository.java
│       │       │   ├── NoteRepository.java
│       │       │   └── TagRepository.java
│       │       ├── service/
│       │       │   ├── AuthService.java
│       │       │   └── NoteService.java
│       │       └── util/
│       │           ├── PasswordUtil.java
│       │           ├── SmartTextUtil.java
│       │           └── DBConnection.java
│       └── resources/
│           ├── db.properties           # Cấu hình database
│           ├── css/
│           │   └── style.css           # Stylesheet
│           └── views/
│               ├── LoginView.fxml
│               ├── MainView.fxml
│               ├── NewNoteDialog.fxml
│               └── SettingsView.fxml
├── pom.xml                     # Maven configuration
└── README.md                   # File này
```

---

## 🚀 Hướng dẫn cài đặt

### Yêu cầu hệ thống

- **JDK 17** trở lên
- **Maven 3.x**
- **SQL Server 2019** (hoặc có thể dùng chế độ Demo)

### Bước 1: Clone dự án

```bash
git clone https://github.com/your-repo/SmartNotebook.git
cd SmartNotebook
```

### Bước 2: Tạo Database

1. Mở SQL Server Management Studio
2. Tạo database mới tên `SmartNotebook`
3. Chạy script `database/schema.sql`

### Bước 3: Cấu hình kết nối

Chỉnh sửa file `src/main/resources/db.properties`:

```properties
db.server=localhost
db.port=1433
db.name=SmartNotebook
db.user=your_username
db.password=your_password
```

Hoặc sử dụng **biến môi trường**:
```bash
set SMARTNOTEBOOK_DB_SERVER=localhost
set SMARTNOTEBOOK_DB_USER=sa
set SMARTNOTEBOOK_DB_PASSWORD=yourpassword
```

### Bước 4: Build và chạy

```bash
# Build project
mvn clean compile

# Chạy ứng dụng
mvn javafx:run
```

### Chạy không cần Database (Demo Mode)

Ứng dụng hỗ trợ chế độ Demo - nhấn nút "Dùng thử không cần đăng nhập" để trải nghiệm mà không cần cấu hình database.

---

## 📖 Hướng dẫn sử dụng

### Đăng nhập

1. Nhập username và password
2. Hoặc chọn "Dùng thử không cần đăng nhập"

**Tài khoản mặc định:**
- Username: `demo`
- Password: `123456`

### Tạo ghi chú

1. Nhấn nút "+ Tạo ghi chú mới"
2. Nhập tiêu đề (hoặc để trống - hệ thống sẽ tự gợi ý)
3. Nhập nội dung
4. Chọn trạng thái và màu sắc
5. Nhấn "Tạo ghi chú"

### Tìm kiếm

1. Nhập từ khóa vào ô tìm kiếm
2. Kết quả sẽ được lọc realtime
3. Từ khóa sẽ được highlight

### Lọc ghi chú

- Click vào các filter: All, Regular, Urgent, Ideas, Completed
- Click "Yêu thích" để xem ghi chú đã đánh dấu

---

## 🗄 Database Schema

### Sơ đồ ERD

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   Users     │       │   Notes     │       │    Tags     │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ UserID (PK) │───┐   │ NoteID (PK) │   ┌───│ TagID (PK)  │
│ Username    │   └──>│ UserID (FK) │   │   │ UserID (FK) │
│ PasswordHash│       │ CategoryID  │   │   │ TagName     │
│ Email       │       │ Title       │   │   │ Color       │
│ FullName    │       │ Content     │   │   │ UsageCount  │
│ IsActive    │       │ Summary     │   │   └─────────────┘
│ CreatedAt   │       │ Status      │   │
└─────────────┘       │ IsFavorite  │   │   ┌─────────────┐
                      │ Color       │   │   │  NoteTags   │
┌─────────────┐       │ CreatedAt   │   │   ├─────────────┤
│ Categories  │       │ UpdatedAt   │   └──>│ NoteID (FK) │
├─────────────┤       └─────────────┘       │ TagID (FK)  │
│ CategoryID  │<──────────────┘             └─────────────┘
│ UserID (FK) │
│ Name        │
│ Color       │
└─────────────┘
```

### Các bảng chính

| Bảng | Mô tả |
|------|-------|
| Users | Thông tin người dùng |
| Notes | Ghi chú của người dùng |
| Categories | Danh mục ghi chú |
| Tags | Nhãn/từ khóa |
| NoteTags | Bảng trung gian Note-Tag |

---

## 📊 Sơ đồ UML

### Class Diagram (Rút gọn)

```
┌──────────────────┐     ┌──────────────────┐
│    AuthService   │     │   NoteService    │
├──────────────────┤     ├──────────────────┤
│ - userRepository │     │ - noteRepository │
│ - currentUser    │     │ - tagRepository  │
├──────────────────┤     ├──────────────────┤
│ + login()        │     │ + createNote()   │
│ + logout()       │     │ + updateNote()   │
│ + register()     │     │ + searchNotes()  │
└────────┬─────────┘     │ + generateSummary│
         │               └────────┬─────────┘
         ↓                        ↓
┌──────────────────┐     ┌──────────────────┐
│  UserRepository  │     │  NoteRepository  │
├──────────────────┤     ├──────────────────┤
│ + findById()     │     │ + findByUser()   │
│ + findByUsername │     │ + insert()       │
│ + insert()       │     │ + update()       │
│ + update()       │     │ + delete()       │
│ + authenticate() │     │ + search()       │
└──────────────────┘     └──────────────────┘
```

### Sequence Diagram - Tạo ghi chú

```
User          Controller        Service         Repository      Database
  │               │                │                │              │
  │──Create Note─>│                │                │              │
  │               │──createNote()─>│                │              │
  │               │                │──suggestTitle()│              │
  │               │                │<─────title─────│              │
  │               │                │──generateSummary              │
  │               │                │<────summary────│              │
  │               │                │──insert()─────>│              │
  │               │                │                │──INSERT─────>│
  │               │                │                │<────OK───────│
  │               │                │<───success─────│              │
  │               │<───Note────────│                │              │
  │<──Display─────│                │                │              │
```

---

## 🎤 Thuyết trình đồ án

### Nội dung thuyết trình gợi ý

1. **Giới thiệu đề tài** (2 phút)
   - Tên đề tài, lý do chọn đề tài
   - Mục tiêu dự án

2. **Kiến trúc & Công nghệ** (3 phút)
   - Kiến trúc phân lớp
   - Các công nghệ sử dụng
   - Tại sao chọn JavaFX

3. **Cơ sở dữ liệu** (2 phút)
   - ERD diagram
   - Giải thích các bảng chính

4. **Demo chức năng** (5 phút)
   - Đăng nhập/Đăng ký
   - CRUD ghi chú
   - Tính năng Smart

5. **Highlight Code** (3 phút)
   - SmartTextUtil - thuật toán tóm tắt
   - Repository pattern
   - Service layer

6. **Hạn chế & Hướng phát triển** (2 phút)
   - Những gì chưa làm được
   - Kế hoạch cải tiến

### Câu hỏi phản biện thường gặp

1. **Tại sao chọn kiến trúc phân lớp?**
   > Giúp tách biệt concerns, dễ bảo trì, test, và mở rộng

2. **Thuật toán tóm tắt hoạt động như thế nào?**
   > Dựa trên: vị trí câu, tần suất từ khóa, độ dài câu

3. **Xử lý bảo mật mật khẩu?**
   > Sử dụng SHA-256 hash, không lưu plaintext

4. **Có thể mở rộng thêm tính năng gì?**
   > AI summarization, sync cloud, collaboration, mobile app

---

## 👨‍💻 Tác giả

**[Tên sinh viên]**
- MSSV: [Mã số]
- Email: [email@example.com]
- Lớp: [Tên lớp]

**Giảng viên hướng dẫn:** [Tên GV]

---

## 📄 License

MIT License - Xem file [LICENSE](LICENSE) để biết thêm chi tiết.

---

<p align="center">
  <b>SmartNotebook</b> - Sổ tay thông minh cho người Việt 📓✨
</p>
