# 📝 Hướng dẫn sử dụng Rich Text Editor

## ✅ HTMLEditor Built-in Toolbar

HTMLEditor của JavaFX đã có **TOOLBAR HOÀN CHỈNH** ngay bên trong editor, không cần custom buttons!

### 🎨 Các công cụ có sẵn trong HTMLEditor:

#### 1. **Định dạng văn bản cơ bản**
- **Bold** (In đậm): Click nút **B** hoặc `Ctrl + B`
- **Italic** (In nghiêng): Click nút **I** hoặc `Ctrl + I`
- **Underline** (Gạch chân): Click nút **U** hoặc `Ctrl + U`
- **Strikethrough** (Gạch ngang): Click nút S̶

#### 2. **Màu sắc**
- **Text Color** (Màu chữ): Click nút màu thứ nhất
- **Background Color** (Màu nền): Click nút màu thứ hai

#### 3. **Căn lề**
- Align Left: Căn trái
- Align Center: Căn giữa
- Align Right: Căn phải
- Justify: Căn đều

#### 4. **Lists (Danh sách)**
- **Bullet List**: Danh sách dấu đầu dòng
- **Number List**: Danh sách số thứ tự

#### 5. **Format (Định dạng)**
- Dropdown menu chọn:
  - Paragraph (Đoạn văn)
  - Heading 1, 2, 3 (Tiêu đề)
  - Preformatted (Code block)

#### 6. **Font**
- Font family: Arial, Times New Roman, Courier, etc.
- Font size: 8, 10, 12, 14, 18, 24, 36

#### 7. **Insert (Chèn)**
- **Horizontal Rule**: Đường kẻ ngang
- **Link**: Chèn liên kết (Ctrl + K)

#### 8. **Undo/Redo**
- Undo: Hoàn tác
- Redo: Làm lại

---

## 🎯 Cách sử dụng

### **Bước 1: Mở Rich Text Editor**

Từ MainController, thêm method:

```java
@FXML
private void handleOpenRichTextEditor() {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/views/RichTextEditorView.fxml")
        );
        Parent root = loader.load();
        RichTextEditorController controller = loader.getController();
        
        Stage stage = new Stage();
        stage.setTitle("Rich Text Editor");
        stage.setScene(new Scene(root, 1000, 650));
        stage.show();
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

Và thêm button vào MainView.fxml:
```xml
<Button text="✨ Rich Text Editor" onAction="#handleOpenRichTextEditor"/>
```

### **Bước 2: Sử dụng HTMLEditor**

1. Click **"+ Ghi chú mới"**
2. Nhập tiêu đề
3. Click vào editor area
4. Sử dụng **toolbar ngay phía trên editor** để định dạng:
   - Chọn text → Click **B** để in đậm
   - Chọn text → Click **I** để in nghiêng
   - Chọn text → Click **U** để gạch chân
   - Chọn text → Click màu để đổi màu chữ
   - Click dropdown "Paragraph" → Chọn "Heading 1" cho tiêu đề lớn

### **Bước 3: Chèn ảnh**

- Click nút **"🖼️ Chèn ảnh"** phía trên
- Chọn file ảnh (PNG, JPG, GIF)
- Ảnh sẽ được nhúng vào HTML

### **Bước 4: Lưu ghi chú**

- **Auto-save**: Tự động sau 3 giây
- **Manual save**: `Ctrl + S` hoặc click **"💾 Lưu"**
  - Tạo version history

---

## 💡 Tips & Tricks

### **1. In đậm/nghiêng nhanh**
```
Gõ text → Bôi đen (drag chuột) → Ctrl + B (đậm) hoặc Ctrl + I (nghiêng)
```

### **2. Tạo tiêu đề**
```
Click vào dòng text → Dropdown "Paragraph" → Chọn "Heading 1"
```

### **3. Đổi màu chữ**
```
Bôi đen text → Click nút màu (color picker) → Chọn màu
```

### **4. Tạo danh sách**
```
Click vào vị trí cần tạo list → Click nút "Bullet List" hoặc "Number List"
```

### **5. Chèn link**
```
Bôi đen text → Click nút Link (hoặc Ctrl + K) → Nhập URL
```

### **6. Copy/Paste từ Word**
```
Copy từ Word → Paste vào editor → Giữ nguyên định dạng!
```

---

## ⚙️ Các tính năng đặc biệt

### **Auto-save**
- Tự động lưu sau 3 giây khi có thay đổi
- Hiển thị "✓ Đã tự động lưu" ở góc trên
- **KHÔNG** tạo version history

### **Manual Save (Ctrl + S)**
- Lưu ngay lập tức
- **TẠO** version history
- Hiển thị alert "Đã lưu thành công"

### **Version History**
1. Click **"🕐 Lịch sử"**
2. Xem danh sách các versions
3. Chọn version cũ → Click **"Khôi phục phiên bản này"**

### **Multi-tab editing**
- Mở nhiều ghi chú cùng lúc
- Mỗi ghi chú một tab
- `Ctrl + W` để đóng tab

---

## 🎨 Ví dụ định dạng

### **Tiêu đề lớn**
```
1. Nhập: "Tiêu đề chính"
2. Dropdown "Paragraph" → "Heading 1"
3. Chọn màu xanh cho text
```

### **Text highlight**
```
1. Bôi đen text
2. Click nút màu nền (background color)
3. Chọn màu vàng
```

### **Mixed formatting**
```
1. "Đây là text in đậm và nghiêng"
2. Bôi đen → Ctrl + B → Ctrl + I
3. Chọn màu đỏ
```

---

## 🐛 Troubleshooting

### **Không thấy toolbar?**
→ Toolbar nằm **NGAY PHÍA TRÊN** editor area, không phải ở top của window

### **Bold/Italic không hoạt động?**
→ Phải **BÔI ĐEN TEXT** trước khi click nút B/I/U

### **Chèn ảnh không thấy?**
→ Scroll xuống trong editor, ảnh có thể nằm dưới

### **Copy từ Word mất format?**
→ Paste bằng `Ctrl + V` (không dùng Ctrl + Shift + V)

---

## 📊 So sánh với Word

| Tính năng | Word | HTMLEditor | Ghi chú |
|-----------|------|------------|---------|
| Bold/Italic/Underline | ✅ | ✅ | Ctrl+B/I/U |
| Text Color | ✅ | ✅ | Color picker |
| Highlight | ✅ | ✅ | Background color |
| Font Family | ✅ | ✅ | Dropdown |
| Font Size | ✅ | ✅ | Dropdown |
| Heading | ✅ | ✅ | Paragraph dropdown |
| Bullet List | ✅ | ✅ | Nút list |
| Number List | ✅ | ✅ | Nút list |
| Insert Image | ✅ | ✅ | Custom button |
| Insert Link | ✅ | ✅ | Ctrl+K |
| Insert Table | ✅ | ❌ | Chưa hỗ trợ |
| Spell Check | ✅ | ❌ | Chưa hỗ trợ |

---

## 🎓 Video Tutorial (Text)

### **Tutorial 1: Tạo ghi chú với định dạng**

```
1. Click "+ Ghi chú mới"
2. Nhập tiêu đề: "Meeting Notes"
3. Trong editor:
   - Gõ "Important Points"
   - Bôi đen → Dropdown "Paragraph" → "Heading 1"
   - Chọn màu đỏ
4. Enter, gõ "Point 1: Complete project"
   - Click nút "Bullet List"
5. Ctrl + S để lưu
```

### **Tutorial 2: Chèn ảnh vào ghi chú**

```
1. Mở ghi chú
2. Đặt con trỏ vào vị trí cần chèn ảnh
3. Click "🖼️ Chèn ảnh"
4. Chọn file ảnh
5. Ảnh xuất hiện trong editor
6. Ctrl + S để lưu
```

### **Tutorial 3: Sử dụng Version History**

```
1. Mở ghi chú đã tồn tại
2. Chỉnh sửa nội dung
3. Ctrl + S (tạo version mới)
4. Chỉnh sửa thêm
5. Ctrl + S lần nữa
6. Click "🕐 Lịch sử"
7. Thấy 2 versions
8. Chọn version đầu → "Khôi phục"
9. Nội dung trở về trạng thái cũ
```

---

## ✨ Best Practices

### **1. Lưu thường xuyên**
- Dùng Ctrl + S sau mỗi đoạn quan trọng
- Tạo version checkpoint

### **2. Đặt tên rõ ràng**
- Tiêu đề ngắn gọn, dễ hiểu
- "Meeting 2026-01-23" thay vì "Note 1"

### **3. Sử dụng Heading**
- H1 cho tiêu đề chính
- H2 cho phần con
- H3 cho chi tiết

### **4. Màu sắc hợp lý**
- Đỏ: Quan trọng
- Vàng: Cần chú ý
- Xanh: Thông tin bổ sung

### **5. Ảnh nhẹ**
- Resize ảnh trước khi chèn
- Nên < 1MB mỗi ảnh

---

## 🚀 Keyboard Shortcuts

| Phím tắt | Chức năng |
|----------|-----------|
| `Ctrl + B` | Bold |
| `Ctrl + I` | Italic |
| `Ctrl + U` | Underline |
| `Ctrl + S` | Lưu ghi chú |
| `Ctrl + N` | Ghi chú mới |
| `Ctrl + W` | Đóng tab |
| `Ctrl + K` | Chèn link |
| `Ctrl + Z` | Undo |
| `Ctrl + Y` | Redo |

---

**Chúc bạn sử dụng Rich Text Editor hiệu quả! 🎉**
