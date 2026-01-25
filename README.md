# 📓 Smart Notebook - Sổ Tay Thông Minh

> **Ứng dụng Desktop quản lý ghi chú với AI hỗ trợ**  
> Java 17 + JavaFX + SQL Server + Local NLP

---

## 📖 Giới thiệu

**SmartNotebook** là ứng dụng quản lý ghi chú cá nhân với giao diện JavaFX hiện đại và các tính năng AI offline giúp người dùng học tập hiệu quả hơn.

### 🎯 Điểm nổi bật

- ✅ Giao diện đẹp mắt với theme Soft Peach & Cream
- 🤖 AI Local NLP (offline, miễn phí, bảo mật)
- 📝 Rich Text Editor với định dạng đa dạng
- 🗂️ Quản lý theo category và tags
- 🔍 Tìm kiếm nhanh và lọc thông minh
- 💾 Version History & Auto-save
- 🗑️ Trash bin với khả năng khôi phục

---

## ✨ Tính năng chính

### 🔐 Quản lý người dùng
- Đăng nhập / Đăng ký
- Mã hóa mật khẩu (MD5)
- Quản lý multi-user

### 📝 Quản lý ghi chú
- Rich Text Editor (bold, italic, underline, color, font, alignment)
- Phân loại: Regular, Urgent, Ideas, Completed
- Category và Tags
- Favorite notes
- Soft delete (Trash)

### 🤖 AI Features (Local NLP - Offline)
- **Auto Summary**: Tóm tắt ghi chú dài bằng thuật toán TF-IDF
- **Title Suggestion**: Gợi ý tiêu đề thông minh
- **Tag Suggestion**: Gợi ý tags tự động từ nội dung
- **Chat Interface**: AI Chat dễ sử dụng

### 🔍 Tính năng khác
- Tìm kiếm realtime
- Lọc theo status, category, favorite
- Version history (rollback)
- Auto-save mỗi 30 giây

---

## 🛠 Tech Stack

| Công nghệ | Phiên bản | Mục đích |
|-----------|-----------|----------|
| Java | 17 | Backend logic |
| JavaFX | 21 | Desktop UI Framework |
| SQL Server | 2019+ | Database |
| JDBC | 12.8.1 | Database connection |
| Maven | 3.x | Build tool |
| Local NLP | Custom | AI features (offline) |

---

## 🚀 Cài đặt

### Yêu cầu

- JDK 17+
- Maven 3.x
- SQL Server 2019+ (hoặc SQL Server Express)

### Bước 1: Clone project

```bash
git clone https://github.com/your-repo/SmartNotebook.git
cd SmartNotebook
```

### Bước 2: Setup Database

Xem chi tiết trong file [DATABASE_SETUP.md](DATABASE_SETUP.md)

Tóm tắt:
```bash
# Chạy script database.sql trong SQL Server Management Studio
```

### Bước 3: Cấu hình kết nối

Sửa file `d:\SmartNotebook\src\main\java\com\dat\notebook\config\DatabaseConnection.java`:

```java
private static final String SERVER = "localhost";
private static final String DATABASE = "SmartNotebook";
private static final String USER = "sa";
private static final String PASSWORD = "your_password";
```

### Bước 4: Build và chạy

```bash
# Build
mvn clean compile

# Run
mvn javafx:run
```

---

## 📖 Hướng dẫn sử dụng

### Đăng nhập

**Tài khoản demo:**
- Username: `demo` | Password: `abc123`
- Username: `admin` | Password: `admin123`
- Username: `dat09` | Password: `221761`

### Tạo ghi chú

1. Click "Create New Note"
2. Nhập title và content
3. Chọn category, status
4. Sử dụng Rich Text toolbar để format
5. Click "Save"

### Sử dụng AI

1. Chọn note cần phân tích
2. Click nút "AI" ở sidebar
3. Chọn:
   - **Tóm tắt**: Tạo bản tóm tắt ngắn gọn
   - **Gợi ý tiêu đề**: Tự động tạo tiêu đề
   - **Gợi ý tags**: Trích xuất từ khóa làm tags

---

## 🗄 Database Schema

```
User (user_id, username, password_hash, email, full_name)
  ↓
Categories (CategoryID, UserID, Name, Color, Icon)
  ↓
Notes (NoteID, UserID, CategoryID, Title, Content, HtmlContent, Summary, Status)
  ↓
Tags (TagID, UserID, Name, Color)
  ↓
NoteTags (NoteID, TagID)

NoteVersions (VersionID, NoteID, Title, HtmlContent, VersionNumber)
AutoSaveSnapshots (SnapshotID, NoteID, HtmlContent, SavedAt)
```

**7 bảng chính + 11 Stored Procedures + 1 View**

---

## 📁 Cấu trúc Project

```
SmartNotebook/
├── database.sql                    # Database setup script
├── DATABASE_SETUP.md               # Hướng dẫn setup DB
├── pom.xml                         # Maven config
└── src/main/
    ├── java/com/dat/notebook/
    │   ├── App.java               # Entry point
    │   ├── config/
    │   │   └── DatabaseConnection.java
    │   ├── controller/
    │   │   ├── LoginController.java
    │   │   ├── MainControllerV2.java
    │   │   ├── AiChatController.java
    │   │   └── ...
    │   ├── model/
    │   │   ├── User.java
    │   │   ├── Note.java
    │   │   └── ...
    │   ├── service/
    │   │   ├── NoteServiceV2.java
    │   │   ├── SummaryService.java
    │   │   ├── TitleSuggestionService.java
    │   │   └── TagSuggestionService.java
    │   └── dao/
    │       └── NoteDAO.java
    └── resources/
        ├── css/
        │   └── fresh-candy.css
        └── views/
            ├── LoginView.fxml
            ├── MainView.fxml
            └── ...
```

---

## 🤖 AI Architecture (Local NLP)

SmartNotebook sử dụng **AI Level 1 - NLP truyền thống**:

- **Không cần API key** (miễn phí 100%)
- **Hoàn toàn offline** (bảo mật tuyệt đối)
- **Thuật toán**: TF-IDF, Tokenization, Stopword Filtering

**3 AI Services:**
1. `SummaryService` - Tóm tắt bằng TF-IDF scoring
2. `TitleSuggestionService` - Gợi ý tiêu đề từ keyword extraction  
3. `TagSuggestionService` - Trích xuất tags từ nội dung

---

## 👨‍💻 Tác giả

**Trần Tài Đạt**
- GitHub: [trantaidat7388](https://github.com/trantaidat7388)
- Email: trantaidat7388@gmail.com

---

## 📄 License

MIT License

---

<p align="center">
  <b>SmartNotebook</b> - Sổ tay thông minh cho học tập 📓✨
</p>
