-- =============================================
-- SmartNotebook Database Setup
-- =============================================

-- Tạo database
CREATE DATABASE SmartNotebook;
GO

USE SmartNotebook;
GO

-- Tạo bảng Categories
CREATE TABLE Categories (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    color VARCHAR(20) DEFAULT '#667eea'
);

-- Tạo bảng Notes
CREATE TABLE Notes (
    id INT IDENTITY(1,1) PRIMARY KEY,
    title NVARCHAR(200) NOT NULL,
    content NVARCHAR(MAX),
    category_id INT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (category_id) REFERENCES Categories(id) ON DELETE SET NULL
);

-- Thêm dữ liệu mẫu
INSERT INTO Categories (name, color) VALUES 
(N'Công việc', '#4CAF50'),
(N'Cá nhân', '#2196F3'),
(N'Học tập', '#FF9800');

INSERT INTO Notes (title, content, category_id) VALUES 
(N'Ghi chú đầu tiên', N'Đây là nội dung ghi chú mẫu.', 1),
(N'Danh sách việc cần làm', N'1. Học Java\n2. Làm project\n3. Review code', 1);

-- Hiển thị kết quả
SELECT * FROM Categories;
SELECT * FROM Notes;
