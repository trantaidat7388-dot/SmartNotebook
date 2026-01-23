# 🚀 SMART NOTEBOOK V2.0 - HƯỚNG DẪN CẬP NHẬT

## 📋 TỔNG QUAN CÁC THAY ĐỔI

### 1. KIẾN TRÚC MVC CHUẨN

```
src/main/java/com/dat/notebook/
├── dao/
│   └── NoteDAO.java          ← MỚI: Data Access Layer
├── service/
│   └── NoteServiceV2.java    ← MỚI: Business Logic
├── controller/
│   └── MainControllerV2.java ← MỚI: UI Controller
└── util/
    └── SmartTextUtil.java    ← CẬP NHẬT: Thêm stripHtml()
```

### 2. FILES MỚI TẠO

| File | Mô tả |
|------|-------|
| `dao/NoteDAO.java` | Data Access Object với userId filtering |
| `service/NoteServiceV2.java` | Service layer không chứa SQL |
| `controller/MainControllerV2.java` | Controller với HTMLEditor + Auto-save |
| `resources/views/MainViewV2.fxml` | FXML với HTMLEditor thay TextArea |
| `SmartNotebook_Schema_V2.sql` | SQL script cập nhật database |

---

## 🗄️ CẬP NHẬT DATABASE

### Chạy SQL Script:
```sql
-- Mở SQL Server Management Studio
-- Chạy file: SmartNotebook_Schema_V2.sql
```

### Thay đổi chính:
- Thêm cột `HtmlContent NVARCHAR(MAX)` cho rich text
- Stored procedures với userId filtering
- Indexes cho performance

---

## 📝 RICH TEXT EDITOR

### HTMLEditor Features:
- ✅ Bold (Ctrl+B)
- ✅ Italic (Ctrl+I)  
- ✅ Underline (Ctrl+U)
- ✅ Font size, font family
- ✅ Text color, background color
- ✅ Lists (ordered, unordered)
- ✅ Alignment (left, center, right)

### Auto-save:
- Tự động lưu sau 1.5 giây không gõ
- Hiển thị trạng thái: "Đang lưu..." → "✔ Đã lưu"
- Keyboard shortcut: Ctrl+S để lưu ngay

---

## 🔒 BẢO MẬT ĐA USER

### Mọi query đều có `WHERE UserID = ?`:
```java
// NoteDAO.java
private static final String SQL_FIND_ALL_BY_USER = 
    "SELECT ... FROM Notes WHERE UserID = ? AND IsArchived = 0";

// Tất cả methods đều yêu cầu userId:
public List<Note> findAllByUser(int userId) {...}
public Optional<Note> findById(int noteId, int userId) {...}
public boolean update(Note note, int userId) {...}
public boolean delete(int noteId, int userId) {...}
```

---

## 🎨 CẢI TIẾN UI/UX

### Editor Panel:
- Chiếm 750px (trước: 350px)
- HTMLEditor với toolbar đầy đủ
- Title input riêng biệt
- Color picker 6 màu pastel
- Status badge (REGULAR, URGENT, IDEAS, COMPLETED)

### Notes List:
- Card hiển thị: Status emoji + Favorite star + Title + Preview + Time
- Hover effect
- Selected highlight
- Empty state khi không có notes

### Save Status:
- "Đang chỉnh sửa..." (màu vàng)
- "Đang lưu..." (màu xanh dương)
- "✔ Đã lưu" (màu xanh lá)
- "⚠ Lỗi" (màu đỏ)

---

## ⌨️ KEYBOARD SHORTCUTS

| Shortcut | Chức năng |
|----------|-----------|
| Ctrl+S | Lưu ghi chú |
| Ctrl+N | Tạo ghi chú mới |
| Ctrl+B | In đậm (trong editor) |
| Ctrl+I | In nghiêng (trong editor) |
| Ctrl+U | Gạch chân (trong editor) |

---

## 🔄 LUỒNG HOẠT ĐỘNG CRUD

### CREATE:
```
1. User click "✨ Tạo ghi chú mới"
2. Editor chuyển sang Create Mode
3. User nhập: Title, Content (HTML), Color
4. Click "💾 Lưu ngay" hoặc Ctrl+S
5. MainControllerV2.handleSaveNote()
   → NoteServiceV2.createNote(note)
   → NoteDAO.insert(note)
   → Database INSERT
6. Refresh notes list, select new note
```

### READ:
```
1. User click vào note card trong list
2. MainControllerV2.selectNote(note)
3. displayNoteInEditor(note)
   → Set txtNoteTitle.setText()
   → Set htmlEditor.setHtmlText()
   → Update status, favorite, color
```

### UPDATE:
```
1. User chỉnh sửa content trong HTMLEditor
2. Periodic check mỗi 1.5s phát hiện thay đổi
3. triggerAutoSave() với debounce
4. performAutoSave()
   → NoteServiceV2.updateNote(note)
   → NoteDAO.update(note, userId)
   → Database UPDATE
5. Update save status "✔ Đã lưu"
```

### DELETE:
```
1. User click nút 🗑️
2. Hiện confirm dialog
3. MainControllerV2.handleDeleteNote()
   → NoteServiceV2.deleteNote(noteId)
   → NoteDAO.delete(noteId, userId)
   → Database UPDATE IsArchived = 1 (soft delete)
4. Refresh list, show empty editor
```

---

## 📁 CẤU TRÚC HOÀN CHỈNH

```
SmartNotebook/
├── src/main/java/com/dat/notebook/
│   ├── App.java
│   ├── config/
│   │   └── DatabaseConfig.java
│   ├── controller/
│   │   ├── LoginController.java      ← Đã cập nhật: load MainViewV2
│   │   ├── MainController.java       ← Giữ lại (bản cũ)
│   │   ├── MainControllerV2.java     ← MỚI: HTMLEditor + auto-save
│   │   ├── SettingsController.java
│   │   └── TrashController.java
│   ├── dao/
│   │   └── NoteDAO.java              ← MỚI: CRUD với userId
│   ├── model/
│   │   ├── Note.java                 ← Đã có htmlContent support
│   │   └── User.java
│   ├── repository/
│   │   └── NoteRepository.java       ← Giữ lại (backward compatible)
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── NoteService.java          ← Giữ lại (bản cũ)
│   │   └── NoteServiceV2.java        ← MỚI: Gọi NoteDAO
│   └── util/
│       └── SmartTextUtil.java        ← Cập nhật: stripHtml(), textToHtml()
├── src/main/resources/
│   ├── css/
│   │   └── fresh-candy.css           ← Cập nhật: styles cho HTMLEditor
│   └── views/
│       ├── MainView.fxml             ← Giữ lại (bản cũ)
│       └── MainViewV2.fxml           ← MỚI: HTMLEditor layout
└── SmartNotebook_Schema_V2.sql       ← MỚI: Database update script
```

---

## 🚀 CHẠY ỨNG DỤNG

```bash
# Compile
mvn clean compile

# Run
mvn javafx:run
```

---

## 📈 NÂNG CẤP TIẾP THEO (ĐỀ XUẤT)

1. **Version History**: Lưu lịch sử thay đổi của note
2. **Search Highlight**: Highlight từ khóa khi tìm kiếm
3. **Export PDF**: Xuất ghi chú sang PDF
4. **Tags**: Hệ thống tags cho notes
5. **Dark Mode**: Theme tối
6. **Sync**: Đồng bộ qua cloud

---

## ⚠️ LƯU Ý

1. **Backup database** trước khi chạy SQL script
2. Nếu gặp lỗi, có thể đổi về bản cũ:
   - Sửa LoginController.java: đổi `MainViewV2.fxml` → `MainView.fxml`
3. HTMLEditor yêu cầu JavaFX WebView module

---

**Version**: 2.0  
**Updated**: 2026-01-23  
**Author**: SmartNotebook Team
