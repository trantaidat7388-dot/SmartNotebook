# 📝 SmartNotebook - Rich Text Editor Implementation Guide

## 🎯 Tổng quan

Hệ thống Rich Text Editor đã được xây dựng hoàn chỉnh với các tính năng:

✅ **HTMLEditor** làm core editor  
✅ **Tab-based interface** - Mỗi note một tab  
✅ **Rich text formatting** - Bold, Italic, Underline, Color, Highlight  
✅ **Insert Image** - Chèn ảnh với Base64 embedding  
✅ **Auto-save** - Tự động lưu sau 3 giây  
✅ **Version History** - Lưu lịch sử chỉnh sửa, có thể rollback  
✅ **Keyboard Shortcuts** - Ctrl+B, Ctrl+I, Ctrl+S, etc.  
✅ **HTML Storage** - Lưu content dạng HTML vào SQL Server  
✅ **CRUD đầy đủ** - Create, Read, Update, Delete  

---

## 📂 Cấu trúc Files đã tạo

### 1. Database Schema
- **RichTextNotes_Schema.sql** - SQL script tạo bảng Notes, NoteVersions, stored procedures

### 2. Model Classes
- **Note.java** (cập nhật) - Thêm `htmlContent`, `versionCount`
- **NoteVersion.java** (mới) - Model cho version history

### 3. DAO Layer
- **NoteDAO.java** (cập nhật) - CRUD với HTML content
- **NoteVersionDAO.java** (mới) - Quản lý version history

### 4. View (FXML)
- **RichTextEditorView.fxml** (mới) - UI với TabPane, Toolbar, HTMLEditor

### 5. Controller
- **RichTextEditorController.java** (mới) - Logic cho editor, tabs, auto-save, shortcuts

### 6. Scripts
- **run-rich-text-schema.bat** - Batch file chạy SQL schema

---

## 🚀 Hướng dẫn Setup

### Bước 1: Chạy SQL Schema

```bash
# Mở file run-rich-text-schema.bat
# Sửa thông tin kết nối database:
set SERVER=localhost
set DATABASE=SmartNotebook_DB
set USERNAME=sa
set PASSWORD=yourpassword

# Chạy script
run-rich-text-schema.bat
```

Hoặc chạy trực tiếp trong SQL Server Management Studio:
```sql
-- Mở file RichTextNotes_Schema.sql và Execute
```

Schema sẽ tạo:
- Cột `HtmlContent` trong bảng `Notes`
- Bảng `NoteVersions` cho version history
- Bảng `AutoSaveSnapshots` cho auto-save temporary
- Stored procedures: `sp_CreateNoteVersion`, `sp_GetNoteVersionHistory`, `sp_RollbackToVersion`
- View: `vw_NotesWithVersions`

### Bước 2: Tích hợp vào MainController

Cập nhật **MainController.java** để mở Rich Text Editor:

```java
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

// Trong MainController class

/**
 * Mở Rich Text Editor window
 */
@FXML
private void handleOpenRichTextEditor() {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/views/RichTextEditorView.fxml")
        );
        Parent root = loader.load();
        
        // Get controller
        RichTextEditorController controller = loader.getController();
        
        // Nếu có note được chọn, mở nó
        if (selectedNote != null) {
            controller.openNote(selectedNote);
        }
        
        // Tạo window mới
        Stage stage = new Stage();
        stage.setTitle("SmartNotebook - Rich Text Editor");
        stage.setScene(new Scene(root, 1000, 650));
        stage.show();
        
    } catch (Exception e) {
        e.printStackTrace();
        showAlert("Lỗi khi mở Rich Text Editor: " + e.getMessage(), Alert.AlertType.ERROR);
    }
}
```

Thêm button vào **MainView.fxml**:

```xml
<Button text="✨ Rich Text Editor" 
        styleClass="btn-primary"
        onAction="#handleOpenRichTextEditor"/>
```

### Bước 3: Compile và chạy

```bash
# Clean và compile
mvn clean compile

# Run application
mvn javafx:run
```

---

## 🎮 Cách sử dụng

### Tạo ghi chú mới

1. Click **"+ Ghi chú mới"** hoặc `Ctrl + N`
2. Tab mới sẽ mở với HTMLEditor
3. Nhập tiêu đề và nội dung
4. Content tự động lưu sau 3 giây

### Mở ghi chú có sẵn

Từ MainController:
```java
richTextEditorController.openNote(note);
```

### Định dạng văn bản

- **Bold**: Click nút B hoặc `Ctrl + B`
- **Italic**: Click nút I hoặc `Ctrl + I`
- **Underline**: Click nút U hoặc `Ctrl + U`
- **Text Color**: Chọn ColorPicker "Màu chữ"
- **Background**: Chọn ColorPicker "Nền"

### Chèn ảnh

1. Click **"🖼️ Ảnh"**
2. Chọn file ảnh (PNG, JPG, GIF)
3. Ảnh sẽ được embed dạng Base64 vào HTML

### Chèn các element khác

- **Link**: Click "🔗 Link" → Nhập URL
- **Bullet List**: Click "• List"
- **Number List**: Click "1. List"
- **Table**: Click "⊞ Bảng" (tạo table 3x3)

### Lưu ghi chú

- **Auto-save**: Tự động sau 3 giây khi có thay đổi
- **Manual save**: Click **"💾 Lưu"** hoặc `Ctrl + S`
  - Manual save sẽ tạo version history

### Version History

1. Click **"🕐 Lịch sử"**
2. Xem danh sách các versions
3. Chọn version → Click **"Khôi phục phiên bản này"**

### Keyboard Shortcuts

| Phím tắt | Chức năng |
|----------|-----------|
| `Ctrl + N` | Tạo ghi chú mới |
| `Ctrl + S` | Lưu ghi chú |
| `Ctrl + W` | Đóng tab hiện tại |
| `Ctrl + B` | Bold |
| `Ctrl + I` | Italic |
| `Ctrl + U` | Underline |

---

## 🗂️ Database Schema Details

### Bảng Notes

```sql
ALTER TABLE Notes
ADD HtmlContent NVARCHAR(MAX) NULL;
```

| Column | Type | Description |
|--------|------|-------------|
| NoteID | INT | Primary Key |
| Title | NVARCHAR(200) | Tiêu đề |
| Content | NVARCHAR(MAX) | Plain text (backward compatibility) |
| **HtmlContent** | **NVARCHAR(MAX)** | **HTML content** |
| CreatedAt | DATETIME | Ngày tạo |
| UpdatedAt | DATETIME | Ngày cập nhật |

### Bảng NoteVersions

| Column | Type | Description |
|--------|------|-------------|
| VersionID | INT | Primary Key |
| NoteID | INT | Foreign Key → Notes |
| Title | NVARCHAR(200) | Tiêu đề version này |
| HtmlContent | NVARCHAR(MAX) | Nội dung HTML |
| PlainTextContent | NVARCHAR(MAX) | Plain text để search |
| VersionNumber | INT | Số thứ tự version (1, 2, 3...) |
| CreatedAt | DATETIME | Thời điểm tạo version |
| CreatedBy | INT | User tạo version |
| ChangeDescription | NVARCHAR(500) | Mô tả thay đổi |

### Stored Procedures

#### sp_CreateNoteVersion
Tạo version mới cho note.

```sql
EXEC sp_CreateNoteVersion 
    @NoteID = 1,
    @Title = N'Tiêu đề',
    @HtmlContent = N'<p>Nội dung HTML</p>',
    @UserID = 1,
    @ChangeDescription = N'Manual save';
```

#### sp_GetNoteVersionHistory
Lấy danh sách versions của note.

```sql
EXEC sp_GetNoteVersionHistory @NoteID = 1, @MaxVersions = 50;
```

#### sp_RollbackToVersion
Rollback note về version cũ.

```sql
EXEC sp_RollbackToVersion 
    @NoteID = 1,
    @VersionID = 5,
    @UserID = 1;
```

---

## 🔧 Customization

### Thay đổi Auto-save Delay

Trong **RichTextEditorController.java**:

```java
private static final long AUTO_SAVE_DELAY_SECONDS = 3; // Đổi thành 5 giây
```

### Giới hạn số versions lưu

Cleanup old versions:

```java
// Trong NoteVersionDAO
versionDAO.keepLatestVersions(noteId, 10); // Giữ 10 versions mới nhất
```

### Custom Toolbar

Sửa **RichTextEditorView.fxml**:

```xml
<Button text="🎨 Custom Button" 
        onAction="#handleCustomAction"/>
```

Thêm handler trong **RichTextEditorController.java**:

```java
@FXML
private void handleCustomAction() {
    // Your custom logic
}
```

---

## 🎨 Styling

Editor sử dụng stylesheet: `fresh-candy.css`

Custom styles cho editor:

```css
/* Trong fresh-candy.css */

.editor-root {
    -fx-background-color: #f8fafc;
}

.editor-main-toolbar {
    -fx-background-color: white;
    -fx-border-color: #e2e8f0;
    -fx-border-width: 0 0 1 0;
}

.format-btn {
    -fx-background-color: #f1f5f9;
    -fx-background-radius: 6;
    -fx-padding: 6 12;
}

.format-btn:hover {
    -fx-background-color: #e2e8f0;
}
```

---

## 🐛 Troubleshooting

### Lỗi: "HtmlContent column not found"

**Giải pháp**: Chạy lại SQL schema:
```bash
run-rich-text-schema.bat
```

### Auto-save không hoạt động

**Kiểm tra**:
- Database connection có OK không?
- User đã login chưa? (`authService.getCurrentUserId() > 0`)

### Ảnh không hiển thị

**Nguyên nhân**: File quá lớn (> 5MB)

**Giải pháp**: Resize ảnh trước khi insert hoặc dùng URL thay vì Base64

### Version history trống

**Nguyên nhân**: Chưa Manual save lần nào

**Giải pháp**: Click "💾 Lưu" để tạo version đầu tiên

---

## 📊 Performance Tips

### 1. Lazy load versions

```java
// Chỉ load khi cần
List<NoteVersion> versions = versionDAO.getVersionsByNoteId(noteId, 10);
```

### 2. Cleanup old auto-save snapshots

```sql
-- Chạy định kỳ
EXEC sp_CleanupOldSnapshots;
```

### 3. Limit HTML content size

```java
// Trong RichTextEditorController
private static final int MAX_HTML_LENGTH = 1_000_000; // 1MB

if (htmlContent.length() > MAX_HTML_LENGTH) {
    showAlert("Nội dung quá dài!", Alert.AlertType.WARNING);
    return;
}
```

---

## 🎓 Luồng hoạt động (Flow)

### 1. Tạo ghi chú mới

```
User click "+ Ghi chú mới"
    ↓
handleNewNote()
    ↓
Tạo Note object mới (id = 0)
    ↓
createNoteTab(note, isNew=true)
    ↓
Tạo HTMLEditor với nội dung mặc định
    ↓
User nhập nội dung
    ↓
scheduleAutoSave() (sau 3s)
    ↓
autoSaveNote()
    ↓
noteService.createNote() → INSERT vào DB
    ↓
note.setId(generatedId)
    ↓
Status: "✓ Đã tự động lưu"
```

### 2. Lưu thủ công (Manual Save)

```
User click "💾 Lưu" hoặc Ctrl+S
    ↓
handleSaveNote()
    ↓
saveNote(note, editor)
    ↓
noteService.updateNote()
    ↓
createVersionHistory()
    ↓
versionDAO.createVersion() → INSERT NoteVersions
    ↓
Alert: "Đã lưu thành công"
```

### 3. Rollback version

```
User click "🕐 Lịch sử"
    ↓
showVersionHistoryDialog()
    ↓
versionDAO.getVersionsByNoteId() → Lấy danh sách versions
    ↓
User chọn version → Click "Khôi phục"
    ↓
restoreVersion()
    ↓
versionDAO.rollbackToVersion()
    ↓
Stored proc: sp_RollbackToVersion
    ↓
UPDATE Notes SET HtmlContent = (version cũ)
    ↓
Tạo version mới với ChangeDescription = "Rolled back"
    ↓
Reload note vào editor
```

---

## 🔐 Security Notes

### XSS Protection

HTMLEditor có built-in XSS protection, nhưng nên validate:

```java
// Trong saveNote()
String htmlContent = sanitizeHtml(editor.getHtmlText());
```

### SQL Injection

Sử dụng PreparedStatement → An toàn

```java
ps.setString(1, note.getHtmlContent()); // Tự động escape
```

---

## 🚢 Deployment Checklist

- [ ] Chạy SQL schema trên production database
- [ ] Test CRUD operations
- [ ] Test version history & rollback
- [ ] Test auto-save với network lag
- [ ] Test với nhiều users đồng thời
- [ ] Backup database trước khi deploy
- [ ] Set auto-save delay phù hợp (3-5s)
- [ ] Config cleanup job cho old versions

---

## 📞 Support

Nếu gặp vấn đề:

1. Kiểm tra database connection
2. Kiểm tra logs trong console
3. Verify SQL schema đã chạy đúng
4. Test từng chức năng riêng lẻ

---

## 🎉 Kết luận

Hệ thống Rich Text Editor đã hoàn chỉnh với đầy đủ tính năng như Notion/Google Keep:

✅ Tab-based multi-note editing  
✅ Rich text formatting  
✅ Image insertion  
✅ Auto-save  
✅ Version history  
✅ Keyboard shortcuts  
✅ HTML storage  
✅ Performance optimization  

**Enjoy coding! 🚀**
