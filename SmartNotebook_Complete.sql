-- =====================================================
-- SMART NOTEBOOK - COMPLETE DATABASE SCRIPT
-- SQL Server 2012+
-- =====================================================
-- File này gộp tất cả các file SQL thành 1 file duy nhất
-- Chạy trong SQL Server Management Studio hoặc Azure Data Studio
-- =====================================================
-- Bao gồm:
-- 1. Tạo Database và các bảng cơ bản
-- 2. Thêm cột HtmlContent cho Rich Text Editor
-- 3. Stored Procedures cho CRUD operations
-- 4. Stored Procedures cho Version History
-- 5. Views và Indexes
-- 6. Dữ liệu mẫu (demo, admin, dat09)
-- 7. Truy xuất tất cả các bảng (cuối file)
-- =====================================================

-- =====================================================
-- PHẦN 1: TẠO DATABASE VÀ CÁC BẢNG CƠ BẢN
-- =====================================================

-- Tạo Database (nếu chưa có)
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'SmartNotebook')
BEGIN
    CREATE DATABASE SmartNotebook;
    PRINT '✓ Created database: SmartNotebook';
END
GO

USE SmartNotebook;
GO

-- =====================================================
-- DROP TABLES (bỏ comment nếu muốn reset toàn bộ)
-- =====================================================
-- DROP TABLE IF EXISTS AutoSaveSnapshots;
-- DROP TABLE IF EXISTS NoteVersions;
-- DROP TABLE IF EXISTS NoteTags;
-- DROP TABLE IF EXISTS Tags;
-- DROP TABLE IF EXISTS Notes;
-- DROP TABLE IF EXISTS Categories;
-- DROP TABLE IF EXISTS [User];

-- =====================================================
-- 1. BẢNG USER - Quản lý người dùng
-- =====================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'User')
BEGIN
    CREATE TABLE [User] (
        user_id INT IDENTITY(1,1) PRIMARY KEY,
        username NVARCHAR(50) NOT NULL UNIQUE,
        password_hash VARCHAR(32) NOT NULL,     -- MD5 hash (32 chars)
        email NVARCHAR(100),
        full_name NVARCHAR(100),
        is_active BIT DEFAULT 1,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE()
    );
    
    PRINT '✓ Created table: [User]';
END
GO

-- =====================================================
-- 2. BẢNG CATEGORIES - Danh mục ghi chú
-- =====================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Categories')
BEGIN
    CREATE TABLE Categories (
        CategoryID INT IDENTITY(1,1) PRIMARY KEY,
        UserID INT NOT NULL,
        Name NVARCHAR(100) NOT NULL,
        Color NVARCHAR(30) DEFAULT '#a8edea',
        Icon NVARCHAR(10) DEFAULT '📁',
        SortOrder INT DEFAULT 0,
        CreatedAt DATETIME DEFAULT GETDATE(),
        
        CONSTRAINT FK_Categories_User FOREIGN KEY (UserID) REFERENCES [User](user_id)
    );
    
    PRINT '✓ Created table: Categories';
END
GO

-- =====================================================
-- 3. BẢNG NOTES - Ghi chú (BẢNG CHÍNH)
-- =====================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Notes')
BEGIN
    CREATE TABLE Notes (
        NoteID INT IDENTITY(1,1) PRIMARY KEY,
        UserID INT NOT NULL,
        CategoryID INT,
        Title NVARCHAR(200) NOT NULL,
        Content NVARCHAR(MAX),                  -- Plain text content
        HtmlContent NVARCHAR(MAX),              -- Rich text HTML content
        Summary NVARCHAR(500),                  -- Tóm tắt tự động
        Status NVARCHAR(20) DEFAULT 'REGULAR',  -- REGULAR, URGENT, IDEAS, COMPLETED
        IsFavorite BIT DEFAULT 0,
        IsArchived BIT DEFAULT 0,               -- Soft delete
        Color NVARCHAR(30) DEFAULT '#ffffff',
        ViewCount INT DEFAULT 0,
        CreatedAt DATETIME DEFAULT GETDATE(),
        UpdatedAt DATETIME DEFAULT GETDATE(),
        
        CONSTRAINT FK_Notes_User FOREIGN KEY (UserID) REFERENCES [User](user_id),
        CONSTRAINT FK_Notes_Categories FOREIGN KEY (CategoryID) REFERENCES Categories(CategoryID)
    );
    
    -- Indexes để tối ưu truy vấn
    CREATE INDEX IX_Notes_UserID ON Notes(UserID);
    CREATE INDEX IX_Notes_Status ON Notes(Status);
    CREATE INDEX IX_Notes_IsFavorite ON Notes(IsFavorite);
    CREATE INDEX IX_Notes_IsArchived ON Notes(IsArchived);
    
    PRINT '✓ Created table: Notes with indexes';
END
ELSE
BEGIN
    -- Thêm cột HtmlContent nếu chưa có (cho database cũ)
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Notes') AND name = 'HtmlContent')
    BEGIN
        ALTER TABLE Notes ADD HtmlContent NVARCHAR(MAX);
        PRINT '✓ Added column HtmlContent to Notes table';
    END
END
GO

-- =====================================================
-- 4. BẢNG TAGS - Thẻ tag
-- =====================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Tags')
BEGIN
    CREATE TABLE Tags (
        TagID INT IDENTITY(1,1) PRIMARY KEY,
        UserID INT NOT NULL,
        Name NVARCHAR(50) NOT NULL,
        Color NVARCHAR(30) DEFAULT '#c3b1e1',
        CreatedAt DATETIME DEFAULT GETDATE(),
        
        CONSTRAINT FK_Tags_User FOREIGN KEY (UserID) REFERENCES [User](user_id)
    );
    
    PRINT '✓ Created table: Tags';
END
GO

-- =====================================================
-- 5. BẢNG NOTETAGS - Quan hệ Notes-Tags
-- =====================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'NoteTags')
BEGIN
    CREATE TABLE NoteTags (
        NoteID INT NOT NULL,
        TagID INT NOT NULL,
        
        PRIMARY KEY (NoteID, TagID),
        CONSTRAINT FK_NoteTags_Notes FOREIGN KEY (NoteID) REFERENCES Notes(NoteID) ON DELETE CASCADE,
        CONSTRAINT FK_NoteTags_Tags FOREIGN KEY (TagID) REFERENCES Tags(TagID) ON DELETE CASCADE
    );
    
    PRINT '✓ Created table: NoteTags';
END
GO

-- =====================================================
-- 6. BẢNG NOTEVERSIONS - Lưu lịch sử chỉnh sửa
-- =====================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'NoteVersions')
BEGIN
    CREATE TABLE NoteVersions (
        VersionID INT PRIMARY KEY IDENTITY(1,1),
        NoteID INT NOT NULL,
        Title NVARCHAR(200) NOT NULL,
        HtmlContent NVARCHAR(MAX),
        PlainTextContent NVARCHAR(MAX),         -- Bản text thuần để tìm kiếm
        VersionNumber INT NOT NULL DEFAULT 1,
        CreatedAt DATETIME NOT NULL DEFAULT GETDATE(),
        CreatedBy INT NULL,                     -- UserID người tạo version này
        ChangeDescription NVARCHAR(500),        -- Mô tả thay đổi (optional)
        
        CONSTRAINT FK_NoteVersions_Notes FOREIGN KEY (NoteID) 
            REFERENCES Notes(NoteID) ON DELETE CASCADE,
        CONSTRAINT FK_NoteVersions_Users FOREIGN KEY (CreatedBy) 
            REFERENCES [User](user_id) ON DELETE NO ACTION
    );
    
    CREATE INDEX IX_NoteVersions_NoteID ON NoteVersions(NoteID);
    CREATE INDEX IX_NoteVersions_CreatedAt ON NoteVersions(CreatedAt DESC);
    
    PRINT '✓ Created table: NoteVersions';
END
GO

-- =====================================================
-- 7. BẢNG AUTOSAVESNAPSHOTS - Auto-save tạm thời
-- =====================================================
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'AutoSaveSnapshots')
BEGIN
    CREATE TABLE AutoSaveSnapshots (
        SnapshotID INT PRIMARY KEY IDENTITY(1,1),
        NoteID INT NOT NULL,
        Title NVARCHAR(200),
        HtmlContent NVARCHAR(MAX),
        SavedAt DATETIME NOT NULL DEFAULT GETDATE(),
        UserID INT NOT NULL,
        
        CONSTRAINT FK_AutoSave_Notes FOREIGN KEY (NoteID) 
            REFERENCES Notes(NoteID) ON DELETE CASCADE,
        CONSTRAINT FK_AutoSave_Users FOREIGN KEY (UserID) 
            REFERENCES [User](user_id) ON DELETE CASCADE
    );
    
    CREATE INDEX IX_AutoSave_NoteID ON AutoSaveSnapshots(NoteID);
    
    PRINT '✓ Created table: AutoSaveSnapshots';
END
GO

-- =====================================================
-- PHẦN 2: INDEXES BỔ SUNG CHO PERFORMANCE
-- =====================================================

-- Index cho truy vấn theo user và archived status
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Notes_UserID_IsArchived')
BEGIN
    CREATE INDEX IX_Notes_UserID_IsArchived ON Notes(UserID, IsArchived);
    PRINT '✓ Created index: IX_Notes_UserID_IsArchived';
END
GO

-- Index cho tìm kiếm theo title
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Notes_Title')
BEGIN
    CREATE INDEX IX_Notes_Title ON Notes(Title);
    PRINT '✓ Created index: IX_Notes_Title';
END
GO

-- =====================================================
-- PHẦN 3: STORED PROCEDURES CHO NOTES
-- =====================================================

-- Drop existing procedures
IF OBJECT_ID('sp_GetNotesByUser', 'P') IS NOT NULL DROP PROCEDURE sp_GetNotesByUser;
IF OBJECT_ID('sp_GetNoteById', 'P') IS NOT NULL DROP PROCEDURE sp_GetNoteById;
IF OBJECT_ID('sp_CreateNote', 'P') IS NOT NULL DROP PROCEDURE sp_CreateNote;
IF OBJECT_ID('sp_UpdateNote', 'P') IS NOT NULL DROP PROCEDURE sp_UpdateNote;
IF OBJECT_ID('sp_DeleteNote', 'P') IS NOT NULL DROP PROCEDURE sp_DeleteNote;
IF OBJECT_ID('sp_RestoreNote', 'P') IS NOT NULL DROP PROCEDURE sp_RestoreNote;
IF OBJECT_ID('sp_SearchNotes', 'P') IS NOT NULL DROP PROCEDURE sp_SearchNotes;
GO

-- SP: Lấy tất cả ghi chú của user (không archived)
CREATE PROCEDURE sp_GetNotesByUser
    @UserID INT
AS
BEGIN
    SET NOCOUNT ON;
    
    SELECT 
        n.NoteID, n.UserID, n.CategoryID, n.Title, n.Content, n.HtmlContent, 
        n.Summary, n.Status, n.IsFavorite, n.IsArchived, n.Color, 
        n.ViewCount, n.CreatedAt, n.UpdatedAt,
        c.Name AS CategoryName
    FROM Notes n
    LEFT JOIN Categories c ON n.CategoryID = c.CategoryID
    WHERE n.UserID = @UserID 
      AND n.IsArchived = 0
    ORDER BY n.UpdatedAt DESC;
END
GO
PRINT '✓ Created procedure: sp_GetNotesByUser';
GO

-- SP: Lấy ghi chú theo ID (kiểm tra user)
CREATE PROCEDURE sp_GetNoteById
    @NoteID INT,
    @UserID INT
AS
BEGIN
    SET NOCOUNT ON;
    
    SELECT 
        NoteID, UserID, CategoryID, Title, Content, HtmlContent,
        Summary, Status, IsFavorite, IsArchived, Color,
        ViewCount, CreatedAt, UpdatedAt
    FROM Notes 
    WHERE NoteID = @NoteID 
      AND UserID = @UserID;
END
GO
PRINT '✓ Created procedure: sp_GetNoteById';
GO

-- SP: Tạo ghi chú mới
CREATE PROCEDURE sp_CreateNote
    @UserID INT,
    @Title NVARCHAR(200),
    @Content NVARCHAR(MAX) = NULL,
    @HtmlContent NVARCHAR(MAX) = NULL,
    @Summary NVARCHAR(500) = NULL,
    @Status NVARCHAR(20) = 'REGULAR',
    @IsFavorite BIT = 0,
    @Color NVARCHAR(30) = '#ffffff',
    @CategoryID INT = NULL
AS
BEGIN
    SET NOCOUNT ON;
    
    INSERT INTO Notes (
        UserID, CategoryID, Title, Content, HtmlContent, Summary,
        Status, IsFavorite, IsArchived, Color, ViewCount,
        CreatedAt, UpdatedAt
    ) VALUES (
        @UserID, @CategoryID, @Title, @Content, @HtmlContent, @Summary,
        @Status, @IsFavorite, 0, @Color, 0,
        GETDATE(), GETDATE()
    );
    
    -- Return the new NoteID
    SELECT SCOPE_IDENTITY() AS NewNoteID;
END
GO
PRINT '✓ Created procedure: sp_CreateNote';
GO

-- SP: Cập nhật ghi chú
CREATE PROCEDURE sp_UpdateNote
    @NoteID INT,
    @UserID INT,
    @Title NVARCHAR(200),
    @Content NVARCHAR(MAX) = NULL,
    @HtmlContent NVARCHAR(MAX) = NULL,
    @Summary NVARCHAR(500) = NULL,
    @Status NVARCHAR(20) = NULL,
    @IsFavorite BIT = NULL,
    @Color NVARCHAR(30) = NULL,
    @CategoryID INT = NULL
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE Notes SET
        CategoryID = COALESCE(@CategoryID, CategoryID),
        Title = @Title,
        Content = COALESCE(@Content, Content),
        HtmlContent = COALESCE(@HtmlContent, HtmlContent),
        Summary = COALESCE(@Summary, Summary),
        Status = COALESCE(@Status, Status),
        IsFavorite = COALESCE(@IsFavorite, IsFavorite),
        Color = COALESCE(@Color, Color),
        UpdatedAt = GETDATE()
    WHERE NoteID = @NoteID 
      AND UserID = @UserID;
    
    SELECT @@ROWCOUNT AS AffectedRows;
END
GO
PRINT '✓ Created procedure: sp_UpdateNote';
GO

-- SP: Soft Delete ghi chú (chuyển vào thùng rác)
CREATE PROCEDURE sp_DeleteNote
    @NoteID INT,
    @UserID INT
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE Notes SET
        IsArchived = 1,
        UpdatedAt = GETDATE()
    WHERE NoteID = @NoteID 
      AND UserID = @UserID;
    
    SELECT @@ROWCOUNT AS AffectedRows;
END
GO
PRINT '✓ Created procedure: sp_DeleteNote';
GO

-- SP: Restore ghi chú từ thùng rác
CREATE PROCEDURE sp_RestoreNote
    @NoteID INT,
    @UserID INT
AS
BEGIN
    SET NOCOUNT ON;
    
    UPDATE Notes SET
        IsArchived = 0,
        UpdatedAt = GETDATE()
    WHERE NoteID = @NoteID 
      AND UserID = @UserID;
    
    SELECT @@ROWCOUNT AS AffectedRows;
END
GO
PRINT '✓ Created procedure: sp_RestoreNote';
GO

-- SP: Tìm kiếm notes
CREATE PROCEDURE sp_SearchNotes
    @UserID INT,
    @Keyword NVARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;
    
    SELECT 
        n.NoteID, n.UserID, n.CategoryID, n.Title, n.Content, n.HtmlContent,
        n.Summary, n.Status, n.IsFavorite, n.IsArchived, n.Color,
        n.ViewCount, n.CreatedAt, n.UpdatedAt,
        c.Name AS CategoryName
    FROM Notes n
    LEFT JOIN Categories c ON n.CategoryID = c.CategoryID
    WHERE n.UserID = @UserID 
      AND n.IsArchived = 0
      AND (
          n.Title LIKE '%' + @Keyword + '%' 
          OR n.Content LIKE '%' + @Keyword + '%'
          OR n.HtmlContent LIKE '%' + @Keyword + '%'
      )
    ORDER BY n.UpdatedAt DESC;
END
GO
PRINT '✓ Created procedure: sp_SearchNotes';
GO

-- =====================================================
-- PHẦN 4: STORED PROCEDURES CHO VERSION HISTORY
-- =====================================================

IF OBJECT_ID('sp_CreateNoteVersion', 'P') IS NOT NULL DROP PROCEDURE sp_CreateNoteVersion;
IF OBJECT_ID('sp_GetNoteVersionHistory', 'P') IS NOT NULL DROP PROCEDURE sp_GetNoteVersionHistory;
IF OBJECT_ID('sp_RollbackToVersion', 'P') IS NOT NULL DROP PROCEDURE sp_RollbackToVersion;
IF OBJECT_ID('sp_CleanupOldSnapshots', 'P') IS NOT NULL DROP PROCEDURE sp_CleanupOldSnapshots;
GO

-- SP: Tạo version mới khi lưu ghi chú
CREATE PROCEDURE sp_CreateNoteVersion
    @NoteID INT,
    @Title NVARCHAR(200),
    @HtmlContent NVARCHAR(MAX),
    @UserID INT,
    @ChangeDescription NVARCHAR(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    
    DECLARE @NextVersion INT;
    
    -- Lấy version number tiếp theo
    SELECT @NextVersion = ISNULL(MAX(VersionNumber), 0) + 1
    FROM NoteVersions
    WHERE NoteID = @NoteID;
    
    -- Strip HTML tags để tạo plain text (simplified)
    DECLARE @PlainText NVARCHAR(MAX);
    SET @PlainText = @HtmlContent;
    SET @PlainText = REPLACE(@PlainText, '<p>', '');
    SET @PlainText = REPLACE(@PlainText, '</p>', CHAR(10));
    SET @PlainText = REPLACE(@PlainText, '<br>', CHAR(10));
    SET @PlainText = REPLACE(@PlainText, '<br/>', CHAR(10));
    
    -- Tạo version mới
    INSERT INTO NoteVersions (NoteID, Title, HtmlContent, PlainTextContent, 
                              VersionNumber, CreatedBy, ChangeDescription)
    VALUES (@NoteID, @Title, @HtmlContent, @PlainText, 
            @NextVersion, @UserID, @ChangeDescription);
    
    -- Xóa auto-save snapshot cũ
    DELETE FROM AutoSaveSnapshots WHERE NoteID = @NoteID AND UserID = @UserID;
    
    SELECT SCOPE_IDENTITY() AS NewVersionID, @NextVersion AS VersionNumber;
END
GO
PRINT '✓ Created procedure: sp_CreateNoteVersion';
GO

-- SP: Lấy version history của note
CREATE PROCEDURE sp_GetNoteVersionHistory
    @NoteID INT,
    @MaxVersions INT = 50
AS
BEGIN
    SET NOCOUNT ON;
    
    SELECT TOP (@MaxVersions)
        v.VersionID,
        v.NoteID,
        v.Title,
        v.VersionNumber,
        v.CreatedAt,
        v.CreatedBy,
        v.ChangeDescription,
        u.username AS CreatedByUsername,
        LEN(v.HtmlContent) AS ContentLength
    FROM NoteVersions v
    LEFT JOIN [User] u ON v.CreatedBy = u.user_id
    WHERE v.NoteID = @NoteID
    ORDER BY v.VersionNumber DESC;
END
GO
PRINT '✓ Created procedure: sp_GetNoteVersionHistory';
GO

-- SP: Rollback về version cũ
CREATE PROCEDURE sp_RollbackToVersion
    @NoteID INT,
    @VersionID INT,
    @UserID INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    
    DECLARE @Title NVARCHAR(200);
    DECLARE @HtmlContent NVARCHAR(MAX);
    
    -- Lấy nội dung từ version cũ
    SELECT @Title = Title, @HtmlContent = HtmlContent
    FROM NoteVersions
    WHERE VersionID = @VersionID AND NoteID = @NoteID;
    
    IF @Title IS NULL
    BEGIN
        ROLLBACK;
        RAISERROR('Version không tồn tại', 16, 1);
        RETURN;
    END
    
    -- Cập nhật note chính
    UPDATE Notes
    SET Title = @Title,
        HtmlContent = @HtmlContent,
        Content = @Title,
        UpdatedAt = GETDATE()
    WHERE NoteID = @NoteID AND UserID = @UserID;
    
    -- Tạo version mới để đánh dấu rollback
    EXEC sp_CreateNoteVersion 
        @NoteID = @NoteID,
        @Title = @Title,
        @HtmlContent = @HtmlContent,
        @UserID = @UserID,
        @ChangeDescription = 'Rolled back to earlier version';
    
    COMMIT;
END
GO
PRINT '✓ Created procedure: sp_RollbackToVersion';
GO

-- SP: Cleanup old auto-save snapshots (older than 7 days)
CREATE PROCEDURE sp_CleanupOldSnapshots
AS
BEGIN
    DELETE FROM AutoSaveSnapshots
    WHERE SavedAt < DATEADD(DAY, -7, GETDATE());
    
    PRINT 'Đã xóa ' + CAST(@@ROWCOUNT AS VARCHAR) + ' auto-save snapshots cũ';
END
GO
PRINT '✓ Created procedure: sp_CleanupOldSnapshots';
GO

-- =====================================================
-- PHẦN 5: VIEW
-- =====================================================

IF OBJECT_ID('vw_NotesWithVersions', 'V') IS NOT NULL DROP VIEW vw_NotesWithVersions;
GO

CREATE VIEW vw_NotesWithVersions AS
SELECT 
    n.NoteID,
    n.UserID,
    n.Title,
    n.HtmlContent,
    n.Content,
    n.CreatedAt,
    n.UpdatedAt,
    n.IsFavorite,
    n.Status,
    n.Color,
    n.CategoryID,
    n.IsArchived,
    COUNT(v.VersionID) AS VersionCount,
    MAX(v.VersionNumber) AS LatestVersion
FROM Notes n
LEFT JOIN NoteVersions v ON n.NoteID = v.NoteID
GROUP BY n.NoteID, n.UserID, n.Title, n.HtmlContent, n.Content, n.CreatedAt, 
         n.UpdatedAt, n.IsFavorite, n.Status, n.Color, n.CategoryID, n.IsArchived;
GO
PRINT '✓ Created view: vw_NotesWithVersions';
GO

-- =====================================================
-- PHẦN 6: DỮ LIỆU MẪU - USERS
-- =====================================================

-- User demo (password: abc123)
-- MD5('abc123') = e99a18c428cb38d5f260853678922e03
IF NOT EXISTS (SELECT * FROM [User] WHERE username = 'demo')
BEGIN
    INSERT INTO [User] (username, password_hash, email, full_name)
    VALUES ('demo', 'e99a18c428cb38d5f260853678922e03', 'demo@smartnotebook.com', 'Demo User');
    PRINT '✓ Created user: demo (password: abc123)';
END

-- User admin (password: admin123)  
-- MD5('admin123') = 0192023a7bbd73250516f069df18b500
IF NOT EXISTS (SELECT * FROM [User] WHERE username = 'admin')
BEGIN
    INSERT INTO [User] (username, password_hash, email, full_name)
    VALUES ('admin', '0192023a7bbd73250516f069df18b500', 'admin@smartnotebook.com', 'Administrator');
    PRINT '✓ Created user: admin (password: admin123)';
END

-- User dat09 (password: 221761)
-- MD5('221761') = 7e4cbfdfef9bfb85aca0e3afdf2f7158
IF NOT EXISTS (SELECT * FROM [User] WHERE username = 'dat09')
BEGIN
    INSERT INTO [User] (username, password_hash, email, full_name)
    VALUES ('dat09', '7e4cbfdfef9bfb85aca0e3afdf2f7158', 'dat09@smartnotebook.com', 'Dat 09');
    PRINT '✓ Created user: dat09 (password: 221761)';
END
GO

-- =====================================================
-- PHẦN 7: DỮ LIỆU MẪU - CATEGORIES
-- =====================================================

-- Categories cho demo
DECLARE @DemoID INT = (SELECT user_id FROM [User] WHERE username = 'demo');

IF @DemoID IS NOT NULL AND NOT EXISTS (SELECT * FROM Categories WHERE UserID = @DemoID)
BEGIN
    INSERT INTO Categories (UserID, Name, Color, Icon, SortOrder) VALUES
    (@DemoID, 'Personal', '#60a5fa', '📝', 1),
    (@DemoID, 'Work', '#f59e0b', '💼', 2),
    (@DemoID, 'Ideas', '#a78bfa', '💡', 3),
    (@DemoID, 'To-do', '#34d399', '✅', 4);
    PRINT '✓ Created categories for demo';
END
GO

-- Categories cho dat09
DECLARE @Dat09ID INT = (SELECT user_id FROM [User] WHERE username = 'dat09');

IF @Dat09ID IS NOT NULL AND NOT EXISTS (SELECT * FROM Categories WHERE UserID = @Dat09ID)
BEGIN
    INSERT INTO Categories (UserID, Name, Color, Icon, SortOrder) VALUES
    (@Dat09ID, N'Học tập', '#3b82f6', '📚', 1),
    (@Dat09ID, N'Công việc', '#ef4444', '💻', 2),
    (@Dat09ID, N'Dự án', '#8b5cf6', '🚀', 3),
    (@Dat09ID, N'Cá nhân', '#10b981', '🏠', 4),
    (@Dat09ID, N'Ý tưởng', '#f59e0b', '💡', 5);
    PRINT '✓ Created categories for dat09';
END
GO

-- =====================================================
-- PHẦN 8: DỮ LIỆU MẪU - NOTES CHO DEMO USER
-- =====================================================

DECLARE @DemoID2 INT = (SELECT user_id FROM [User] WHERE username = 'demo');
DECLARE @WorkCat INT = (SELECT CategoryID FROM Categories WHERE UserID = @DemoID2 AND Name = 'Work');
DECLARE @IdeasCat INT = (SELECT CategoryID FROM Categories WHERE UserID = @DemoID2 AND Name = 'Ideas');
DECLARE @PersonalCat INT = (SELECT CategoryID FROM Categories WHERE UserID = @DemoID2 AND Name = 'Personal');

IF @DemoID2 IS NOT NULL AND NOT EXISTS (SELECT * FROM Notes WHERE UserID = @DemoID2)
BEGIN
    INSERT INTO Notes (UserID, CategoryID, Title, Content, HtmlContent, Summary, Status, IsFavorite, Color) VALUES
    
    (@DemoID2, @WorkCat, N'Q4 Roadmap Finalization', 
     N'Need to sync with the design team regarding the final assets for the mobile release...

📋 Tasks:
• Review design mockups
• Schedule meeting with team  
• Prepare presentation slides
• Send final approval request', 
     N'<p>Need to sync with the design team regarding the final assets for the mobile release...</p><p><strong>📋 Tasks:</strong></p><ul><li>Review design mockups</li><li>Schedule meeting with team</li><li>Prepare presentation slides</li><li>Send final approval request</li></ul>',
     N'Sync with design team for mobile release', 'URGENT', 1, '#fef3c7'),
    
    (@DemoID2, @WorkCat, N'Weekly Sync Notes', 
     N'Discussed the roadmap for Q4 and identified key performance metrics...

📝 Key Takeaways:
• Refine the UI: Dashboard feels cluttered
• AI Latency: Summarization takes >3 seconds
• Dark Mode: High priority for mobile

✅ Action Items:
☐ Schedule design team follow-up
☐ Create performance benchmarks', 
     N'<p>Discussed the roadmap for Q4 and identified key performance metrics...</p><p><strong>📝 Key Takeaways:</strong></p><ul><li>Refine the UI: Dashboard feels cluttered</li><li>AI Latency: Summarization takes >3 seconds</li><li>Dark Mode: High priority for mobile</li></ul>',
     N'Q4 roadmap discussion and action items', 'REGULAR', 0, '#dbeafe'),
    
    (@DemoID2, @IdeasCat, N'AI Integration Concepts', 
     N'Exploring NLP to auto-categorize notes...

💡 Ideas:
• Auto-tagging based on content
• Smart semantic search
• Sentiment analysis
• Auto summary generation
• Voice-to-text notes', 
     N'<p>Exploring NLP to auto-categorize notes...</p><p><strong>💡 Ideas:</strong></p><ul><li>Auto-tagging based on content</li><li>Smart semantic search</li><li>Sentiment analysis</li><li>Auto summary generation</li><li>Voice-to-text notes</li></ul>',
     N'NLP-based note categorization ideas', 'IDEAS', 1, '#ede9fe'),
    
    (@DemoID2, @PersonalCat, N'Landing Page Copy', 
     N'Finalized headings for home page...

✅ Completed:
• Hero section copy
• Feature descriptions  
• Call-to-action buttons
• Footer content
• SEO meta tags', 
     N'<p>Finalized headings for home page...</p><p><strong>✅ Completed:</strong></p><ul><li>Hero section copy</li><li>Feature descriptions</li><li>Call-to-action buttons</li><li>Footer content</li><li>SEO meta tags</li></ul>',
     N'Home page copy finalized', 'COMPLETED', 0, '#d1fae5');
    
    PRINT '✓ Created sample notes for demo';
END
GO

-- =====================================================
-- PHẦN 9: DỮ LIỆU MẪU - NOTES CHO DAT09 USER
-- =====================================================

DECLARE @Dat09ID2 INT = (SELECT user_id FROM [User] WHERE username = 'dat09');
DECLARE @HocTapCat INT = (SELECT CategoryID FROM Categories WHERE UserID = @Dat09ID2 AND Name = N'Học tập');
DECLARE @CongViecCat INT = (SELECT CategoryID FROM Categories WHERE UserID = @Dat09ID2 AND Name = N'Công việc');
DECLARE @DuAnCat INT = (SELECT CategoryID FROM Categories WHERE UserID = @Dat09ID2 AND Name = N'Dự án');
DECLARE @CaNhanCat INT = (SELECT CategoryID FROM Categories WHERE UserID = @Dat09ID2 AND Name = N'Cá nhân');
DECLARE @YTuongCat INT = (SELECT CategoryID FROM Categories WHERE UserID = @Dat09ID2 AND Name = N'Ý tưởng');

IF @Dat09ID2 IS NOT NULL AND NOT EXISTS (SELECT * FROM Notes WHERE UserID = @Dat09ID2)
BEGIN
    INSERT INTO Notes (UserID, CategoryID, Title, Content, HtmlContent, Summary, Status, IsFavorite, Color) VALUES
    
    (@Dat09ID2, @HocTapCat, N'Ôn tập Java Core', 
     N'📖 Các chủ đề cần ôn tập:

1. OOP Concepts:
   • Encapsulation - Đóng gói
   • Inheritance - Kế thừa
   • Polymorphism - Đa hình
   • Abstraction - Trừu tượng

2. Collections Framework:
   • List: ArrayList, LinkedList
   • Set: HashSet, TreeSet
   • Map: HashMap, TreeMap

3. Exception Handling:
   • try-catch-finally
   • throw vs throws
   • Custom exceptions

4. Multi-threading:
   • Thread class
   • Runnable interface
   • Synchronized keyword

⏰ Deadline: Cuối tuần này!', 
     N'<p><strong>📖 Các chủ đề cần ôn tập:</strong></p><p><strong>1. OOP Concepts:</strong></p><ul><li>Encapsulation - Đóng gói</li><li>Inheritance - Kế thừa</li><li>Polymorphism - Đa hình</li><li>Abstraction - Trừu tượng</li></ul><p><strong>2. Collections Framework:</strong></p><ul><li>List: ArrayList, LinkedList</li><li>Set: HashSet, TreeSet</li><li>Map: HashMap, TreeMap</li></ul>',
     N'Ôn tập các khái niệm Java Core', 'URGENT', 1, '#dbeafe'),

    (@Dat09ID2, @CongViecCat, N'Meeting Notes - Sprint Planning', 
     N'📅 Sprint Planning Meeting - 23/01/2026

👥 Participants: Team Dev, PM, QA

📋 Sprint Goals:
• Complete login/register module ✅
• Implement note CRUD operations
• Add search functionality
• Setup CI/CD pipeline

📌 Assigned Tasks:
1. Backend API - 3 days
2. Frontend UI - 2 days  
3. Integration testing - 2 days
4. Documentation - 1 day

⚠️ Blockers:
• Waiting for design approval
• Server deployment pending

📊 Velocity: 34 story points', 
     N'<p><strong>📅 Sprint Planning Meeting - 23/01/2026</strong></p><p><strong>👥 Participants:</strong> Team Dev, PM, QA</p><p><strong>📋 Sprint Goals:</strong></p><ul><li>Complete login/register module ✅</li><li>Implement note CRUD operations</li><li>Add search functionality</li><li>Setup CI/CD pipeline</li></ul>',
     N'Sprint planning meeting notes', 'REGULAR', 0, '#fef3c7'),

    (@Dat09ID2, @DuAnCat, N'Smart Notebook - Project Ideas', 
     N'🎯 Smart Notebook Enhancement Ideas

✨ New Features:
• Dark mode support 🌙
• Markdown editor
• Rich text formatting
• Image attachments
• Export to PDF/Word
• Cloud sync
• Collaboration features
• Voice notes

🔧 Technical Improvements:
• Performance optimization
• Better error handling
• Unit test coverage > 80%
• API documentation', 
     N'<p><strong>🎯 Smart Notebook Enhancement Ideas</strong></p><p><strong>✨ New Features:</strong></p><ul><li>Dark mode support 🌙</li><li>Markdown editor</li><li>Rich text formatting</li><li>Image attachments</li><li>Export to PDF/Word</li></ul><p><strong>🔧 Technical Improvements:</strong></p><ul><li>Performance optimization</li><li>Better error handling</li></ul>',
     N'Ideas for Smart Notebook enhancements', 'IDEAS', 1, '#ede9fe'),

    (@Dat09ID2, @CaNhanCat, N'Daily Routine Checklist', 
     N'☀️ Morning Routine:
☐ Wake up 6:00 AM
☐ Exercise 30 mins
☐ Healthy breakfast
☐ Review daily goals

💼 Work Time (9AM - 6PM):
☐ Check emails
☐ Team standup meeting
☐ Focus time (no distractions)
☐ Code review
☐ Learning time (1 hour)

🌙 Evening Routine:
☐ Dinner with family
☐ Side project work
☐ Reading (30 mins)
☐ Plan tomorrow
☐ Sleep by 11PM', 
     N'<p><strong>☀️ Morning Routine:</strong></p><ul><li>☐ Wake up 6:00 AM</li><li>☐ Exercise 30 mins</li><li>☐ Healthy breakfast</li><li>☐ Review daily goals</li></ul><p><strong>💼 Work Time (9AM - 6PM):</strong></p><ul><li>☐ Check emails</li><li>☐ Team standup meeting</li><li>☐ Focus time (no distractions)</li></ul>',
     N'Daily productivity routine', 'REGULAR', 0, '#d1fae5'),

    (@Dat09ID2, @YTuongCat, N'Startup Ideas 2026', 
     N'💡 Startup Ideas to Explore:

1. 🤖 AI Study Assistant
   - Summarize textbooks
   - Generate flashcards
   - Quiz generation
   - Progress tracking

2. 🍔 Food Delivery Optimizer
   - Compare prices across apps
   - Best deals aggregator
   - Group ordering

3. 💪 Fitness Social Network
   - Workout challenges
   - Personal trainer matching
   - Progress sharing

⭐ Most Promising: AI Study Assistant', 
     N'<p><strong>💡 Startup Ideas to Explore:</strong></p><p><strong>1. 🤖 AI Study Assistant</strong></p><ul><li>Summarize textbooks</li><li>Generate flashcards</li><li>Quiz generation</li><li>Progress tracking</li></ul><p><strong>2. 🍔 Food Delivery Optimizer</strong></p><ul><li>Compare prices across apps</li></ul>',
     N'Business ideas for 2026', 'IDEAS', 1, '#fef9c3'),

    (@Dat09ID2, @HocTapCat, N'SQL Server Notes', 
     N'📘 SQL Server Quick Reference

-- DDL Commands:
CREATE, ALTER, DROP, TRUNCATE

-- DML Commands:
SELECT, INSERT, UPDATE, DELETE

-- Useful Functions:
• GETDATE() - Current datetime
• ISNULL(col, value) - Null handling
• COALESCE(v1, v2, v3) - First non-null
• CAST/CONVERT - Type conversion

-- Joins:
• INNER JOIN - Matching rows
• LEFT JOIN - All left + matching right

⚡ Performance Tips:
• Use indexes wisely
• Avoid SELECT *
• Use WHERE clauses', 
     N'<p><strong>📘 SQL Server Quick Reference</strong></p><p><code>-- DDL Commands: CREATE, ALTER, DROP, TRUNCATE</code></p><p><code>-- DML Commands: SELECT, INSERT, UPDATE, DELETE</code></p><p><strong>Useful Functions:</strong></p><ul><li>GETDATE() - Current datetime</li><li>ISNULL(col, value) - Null handling</li></ul>',
     N'SQL Server quick reference guide', 'REGULAR', 1, '#e0e7ff');

    PRINT '✓ Created sample notes for dat09';
END
GO

-- =====================================================
-- PHẦN 10: DỮ LIỆU MẪU - TAGS
-- =====================================================

-- Tags cho demo user
DECLARE @DemoID3 INT = (SELECT user_id FROM [User] WHERE username = 'demo');

IF @DemoID3 IS NOT NULL AND NOT EXISTS (SELECT * FROM Tags WHERE UserID = @DemoID3)
BEGIN
    INSERT INTO Tags (UserID, Name, Color) VALUES
    (@DemoID3, 'important', '#ef4444'),
    (@DemoID3, 'work', '#3b82f6'),
    (@DemoID3, 'personal', '#10b981'),
    (@DemoID3, 'urgent', '#f59e0b'),
    (@DemoID3, 'idea', '#8b5cf6');
    PRINT '✓ Created tags for demo user';
END
GO

-- Tags cho dat09
DECLARE @Dat09ID3 INT = (SELECT user_id FROM [User] WHERE username = 'dat09');

IF @Dat09ID3 IS NOT NULL AND NOT EXISTS (SELECT * FROM Tags WHERE UserID = @Dat09ID3)
BEGIN
    INSERT INTO Tags (UserID, Name, Color) VALUES
    (@Dat09ID3, N'quan trọng', '#ef4444'),
    (@Dat09ID3, N'deadline', '#f97316'),
    (@Dat09ID3, N'học tập', '#3b82f6'),
    (@Dat09ID3, N'project', '#8b5cf6'),
    (@Dat09ID3, N'review', '#10b981'),
    (@Dat09ID3, N'todo', '#eab308');
    PRINT '✓ Created tags for dat09';
END
GO

-- =====================================================
-- PHẦN 11: DỮ LIỆU MẪU - NOTETAGS
-- =====================================================

-- NoteTags cho demo user
DECLARE @DemoID4 INT = (SELECT user_id FROM [User] WHERE username = 'demo');
DECLARE @Note1 INT = (SELECT TOP 1 NoteID FROM Notes WHERE UserID = @DemoID4 AND Title LIKE '%Q4 Roadmap%');
DECLARE @Note2 INT = (SELECT TOP 1 NoteID FROM Notes WHERE UserID = @DemoID4 AND Title LIKE '%Weekly Sync%');
DECLARE @Note3 INT = (SELECT TOP 1 NoteID FROM Notes WHERE UserID = @DemoID4 AND Title LIKE '%AI Integration%');
DECLARE @ImportantTag INT = (SELECT TagID FROM Tags WHERE UserID = @DemoID4 AND Name = 'important');
DECLARE @WorkTag INT = (SELECT TagID FROM Tags WHERE UserID = @DemoID4 AND Name = 'work');
DECLARE @UrgentTag INT = (SELECT TagID FROM Tags WHERE UserID = @DemoID4 AND Name = 'urgent');
DECLARE @IdeaTag INT = (SELECT TagID FROM Tags WHERE UserID = @DemoID4 AND Name = 'idea');

IF @Note1 IS NOT NULL AND @ImportantTag IS NOT NULL AND NOT EXISTS (SELECT * FROM NoteTags WHERE NoteID = @Note1)
BEGIN
    INSERT INTO NoteTags (NoteID, TagID) VALUES 
    (@Note1, @ImportantTag),
    (@Note1, @WorkTag),
    (@Note1, @UrgentTag);
    
    IF @Note2 IS NOT NULL
        INSERT INTO NoteTags (NoteID, TagID) VALUES (@Note2, @WorkTag);
    
    IF @Note3 IS NOT NULL AND @IdeaTag IS NOT NULL
        INSERT INTO NoteTags (NoteID, TagID) VALUES (@Note3, @IdeaTag);
    
    PRINT '✓ Created note-tag relationships for demo';
END
GO

-- NoteTags cho dat09
DECLARE @Dat09ID4 INT = (SELECT user_id FROM [User] WHERE username = 'dat09');
DECLARE @JavaNote INT = (SELECT TOP 1 NoteID FROM Notes WHERE UserID = @Dat09ID4 AND Title LIKE N'%Java Core%');
DECLARE @SQLNote INT = (SELECT TOP 1 NoteID FROM Notes WHERE UserID = @Dat09ID4 AND Title LIKE N'%SQL Server%');
DECLARE @ProjectNote INT = (SELECT TOP 1 NoteID FROM Notes WHERE UserID = @Dat09ID4 AND Title LIKE N'%Smart Notebook%');
DECLARE @QuanTrongTag INT = (SELECT TagID FROM Tags WHERE UserID = @Dat09ID4 AND Name = N'quan trọng');
DECLARE @HocTapTag INT = (SELECT TagID FROM Tags WHERE UserID = @Dat09ID4 AND Name = N'học tập');
DECLARE @ProjectTag INT = (SELECT TagID FROM Tags WHERE UserID = @Dat09ID4 AND Name = N'project');
DECLARE @DeadlineTag INT = (SELECT TagID FROM Tags WHERE UserID = @Dat09ID4 AND Name = N'deadline');

IF @JavaNote IS NOT NULL AND @QuanTrongTag IS NOT NULL AND NOT EXISTS (SELECT * FROM NoteTags WHERE NoteID = @JavaNote)
BEGIN
    INSERT INTO NoteTags (NoteID, TagID) VALUES 
    (@JavaNote, @QuanTrongTag),
    (@JavaNote, @HocTapTag),
    (@JavaNote, @DeadlineTag);
    
    IF @SQLNote IS NOT NULL
        INSERT INTO NoteTags (NoteID, TagID) VALUES 
        (@SQLNote, @HocTapTag);
    
    IF @ProjectNote IS NOT NULL AND @ProjectTag IS NOT NULL
        INSERT INTO NoteTags (NoteID, TagID) VALUES 
        (@ProjectNote, @ProjectTag),
        (@ProjectNote, @QuanTrongTag);
    
    PRINT '✓ Created note-tag relationships for dat09';
END
GO

-- =====================================================
-- PHẦN 12: MIGRATE EXISTING CONTENT TO HTML
-- =====================================================
-- Chuyển đổi plain text content sang HTML format cho các notes chưa có HtmlContent

UPDATE Notes 
SET HtmlContent = '<p>' + REPLACE(REPLACE(Content, CHAR(13), ''), CHAR(10), '</p><p>') + '</p>'
WHERE HtmlContent IS NULL 
  AND Content IS NOT NULL 
  AND Content != '';

PRINT '✓ Migrated existing content to HtmlContent';
GO

-- =====================================================
-- PHẦN 13: THỐNG KÊ SAU KHI SETUP
-- =====================================================
PRINT '';
PRINT '=====================================================';
PRINT '     SMART NOTEBOOK DATABASE - SETUP COMPLETE!';
PRINT '=====================================================';
PRINT '';

SELECT 'Users' AS [Table], COUNT(*) AS [Count] FROM [User]
UNION ALL
SELECT 'Categories', COUNT(*) FROM Categories
UNION ALL
SELECT 'Notes', COUNT(*) FROM Notes
UNION ALL
SELECT 'Tags', COUNT(*) FROM Tags
UNION ALL
SELECT 'NoteTags', COUNT(*) FROM NoteTags
UNION ALL
SELECT 'NoteVersions', COUNT(*) FROM NoteVersions
UNION ALL
SELECT 'AutoSaveSnapshots', COUNT(*) FROM AutoSaveSnapshots;

PRINT '';
PRINT '=====================================================';
PRINT '📌 Demo Accounts:';
PRINT '   Username: demo     | Password: abc123';
PRINT '   Username: admin    | Password: admin123';
PRINT '   Username: dat09    | Password: 221761';
PRINT '=====================================================';
PRINT '';
GO

-- =====================================================
-- =====================================================
--         PHẦN 14: TRUY XUẤT TẤT CẢ CÁC BẢNG
-- =====================================================
-- =====================================================

PRINT '============================================================';
PRINT '              TRUY XUẤT DỮ LIỆU TẤT CẢ CÁC BẢNG';
PRINT '============================================================';
PRINT '';
GO

-- ========== 1. BẢNG [USER] ==========
PRINT '========== 1. BẢNG [USER] ==========';
SELECT 
    user_id AS [ID],
    username AS [Username],
    email AS [Email],
    full_name AS [Full Name],
    CASE WHEN is_active = 1 THEN 'Active' ELSE 'Inactive' END AS [Status],
    FORMAT(created_at, 'dd/MM/yyyy HH:mm') AS [Created At]
FROM [User]
ORDER BY user_id;
GO

-- ========== 2. BẢNG CATEGORIES ==========
PRINT '========== 2. BẢNG CATEGORIES ==========';
SELECT 
    c.CategoryID AS [ID],
    u.username AS [Owner],
    c.Name AS [Category Name],
    c.Icon AS [Icon],
    c.Color AS [Color],
    c.SortOrder AS [Order],
    FORMAT(c.CreatedAt, 'dd/MM/yyyy HH:mm') AS [Created At]
FROM Categories c
JOIN [User] u ON c.UserID = u.user_id
ORDER BY u.username, c.SortOrder;
GO

-- ========== 3. BẢNG NOTES ==========
PRINT '========== 3. BẢNG NOTES ==========';
SELECT 
    n.NoteID AS [ID],
    u.username AS [Owner],
    c.Name AS [Category],
    n.Title AS [Title],
    LEFT(COALESCE(n.Content, ''), 50) + CASE WHEN LEN(COALESCE(n.Content, '')) > 50 THEN '...' ELSE '' END AS [Content Preview],
    n.Status AS [Status],
    CASE WHEN n.IsFavorite = 1 THEN '⭐' ELSE '' END AS [Favorite],
    CASE WHEN n.IsArchived = 1 THEN '📦' ELSE '' END AS [Archived],
    CASE WHEN n.HtmlContent IS NOT NULL THEN '✓' ELSE '' END AS [Has HTML],
    n.ViewCount AS [Views],
    FORMAT(n.CreatedAt, 'dd/MM/yyyy') AS [Created],
    FORMAT(n.UpdatedAt, 'dd/MM/yyyy') AS [Updated]
FROM Notes n
JOIN [User] u ON n.UserID = u.user_id
LEFT JOIN Categories c ON n.CategoryID = c.CategoryID
ORDER BY u.username, n.UpdatedAt DESC;
GO

-- ========== 4. BẢNG TAGS ==========
PRINT '========== 4. BẢNG TAGS ==========';
SELECT 
    t.TagID AS [ID],
    u.username AS [Owner],
    t.Name AS [Tag Name],
    t.Color AS [Color],
    (SELECT COUNT(*) FROM NoteTags nt WHERE nt.TagID = t.TagID) AS [Usage Count],
    FORMAT(t.CreatedAt, 'dd/MM/yyyy HH:mm') AS [Created At]
FROM Tags t
JOIN [User] u ON t.UserID = u.user_id
ORDER BY u.username, t.Name;
GO

-- ========== 5. BẢNG NOTETAGS ==========
PRINT '========== 5. BẢNG NOTETAGS ==========';
SELECT 
    nt.NoteID AS [Note ID],
    n.Title AS [Note Title],
    nt.TagID AS [Tag ID],
    t.Name AS [Tag Name],
    u.username AS [Owner]
FROM NoteTags nt
JOIN Notes n ON nt.NoteID = n.NoteID
JOIN Tags t ON nt.TagID = t.TagID
JOIN [User] u ON n.UserID = u.user_id
ORDER BY u.username, n.Title, t.Name;
GO

-- ========== 6. BẢNG NOTEVERSIONS ==========
PRINT '========== 6. BẢNG NOTEVERSIONS ==========';
SELECT 
    v.VersionID AS [Version ID],
    v.NoteID AS [Note ID],
    n.Title AS [Current Note Title],
    v.Title AS [Version Title],
    v.VersionNumber AS [Version #],
    u.username AS [Created By],
    v.ChangeDescription AS [Change Description],
    FORMAT(v.CreatedAt, 'dd/MM/yyyy HH:mm') AS [Created At]
FROM NoteVersions v
JOIN Notes n ON v.NoteID = n.NoteID
LEFT JOIN [User] u ON v.CreatedBy = u.user_id
ORDER BY v.NoteID, v.VersionNumber DESC;
GO

-- ========== 7. BẢNG AUTOSAVESNAPSHOTS ==========
PRINT '========== 7. BẢNG AUTOSAVESNAPSHOTS ==========';
SELECT 
    s.SnapshotID AS [Snapshot ID],
    s.NoteID AS [Note ID],
    n.Title AS [Note Title],
    s.Title AS [Snapshot Title],
    u.username AS [User],
    FORMAT(s.SavedAt, 'dd/MM/yyyy HH:mm') AS [Saved At]
FROM AutoSaveSnapshots s
JOIN Notes n ON s.NoteID = n.NoteID
JOIN [User] u ON s.UserID = u.user_id
ORDER BY s.SavedAt DESC;
GO

-- =====================================================
-- PHẦN 15: THỐNG KÊ NÂNG CAO
-- =====================================================

-- ========== THỐNG KÊ THEO USER ==========
PRINT '========== 8. THỐNG KÊ THEO USER ==========';
SELECT 
    u.username AS [Username],
    u.full_name AS [Full Name],
    (SELECT COUNT(*) FROM Categories c WHERE c.UserID = u.user_id) AS [Categories],
    (SELECT COUNT(*) FROM Notes n WHERE n.UserID = u.user_id) AS [Total Notes],
    (SELECT COUNT(*) FROM Notes n WHERE n.UserID = u.user_id AND n.IsArchived = 0) AS [Active Notes],
    (SELECT COUNT(*) FROM Notes n WHERE n.UserID = u.user_id AND n.IsFavorite = 1) AS [Favorites],
    (SELECT COUNT(*) FROM Tags t WHERE t.UserID = u.user_id) AS [Tags]
FROM [User] u
ORDER BY u.username;
GO

-- ========== NOTES THEO STATUS ==========
PRINT '========== 9. NOTES THEO STATUS ==========';
SELECT 
    Status AS [Status],
    COUNT(*) AS [Count],
    CAST(COUNT(*) * 100.0 / NULLIF((SELECT COUNT(*) FROM Notes), 0) AS DECIMAL(5,2)) AS [Percentage %]
FROM Notes
GROUP BY Status
ORDER BY [Count] DESC;
GO

-- ========== TOP 10 NOTES GẦN NHẤT ==========
PRINT '========== 10. TOP 10 NOTES GẦN NHẤT ==========';
SELECT TOP 10
    n.NoteID AS [ID],
    n.Title AS [Title],
    u.username AS [Owner],
    c.Name AS [Category],
    n.Status AS [Status],
    CASE WHEN n.IsFavorite = 1 THEN '⭐' ELSE '' END AS [Fav],
    FORMAT(n.UpdatedAt, 'dd/MM/yyyy HH:mm') AS [Last Updated]
FROM Notes n
JOIN [User] u ON n.UserID = u.user_id
LEFT JOIN Categories c ON n.CategoryID = c.CategoryID
WHERE n.IsArchived = 0
ORDER BY n.UpdatedAt DESC;
GO

-- ========== CẤU TRÚC CÁC BẢNG ==========
PRINT '========== 11. CẤU TRÚC CÁC BẢNG ==========';
SELECT 
    TABLE_NAME AS [Table],
    COLUMN_NAME AS [Column],
    DATA_TYPE AS [Type],
    CASE 
        WHEN CHARACTER_MAXIMUM_LENGTH = -1 THEN 'MAX' 
        WHEN CHARACTER_MAXIMUM_LENGTH IS NULL THEN '-'
        ELSE CAST(CHARACTER_MAXIMUM_LENGTH AS VARCHAR) 
    END AS [Length],
    IS_NULLABLE AS [Nullable]
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME IN ('User', 'Categories', 'Notes', 'Tags', 'NoteTags', 'NoteVersions', 'AutoSaveSnapshots')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
GO

-- ========== DANH SÁCH STORED PROCEDURES ==========
PRINT '========== 12. DANH SÁCH STORED PROCEDURES ==========';
SELECT 
    name AS [Procedure Name],
    FORMAT(create_date, 'dd/MM/yyyy HH:mm') AS [Created],
    FORMAT(modify_date, 'dd/MM/yyyy HH:mm') AS [Modified]
FROM sys.procedures
WHERE name LIKE 'sp_%'
ORDER BY name;
GO

-- ========== DANH SÁCH INDEXES ==========
PRINT '========== 13. DANH SÁCH INDEXES ==========';
SELECT 
    t.name AS [Table],
    i.name AS [Index Name],
    i.type_desc AS [Type],
    CASE WHEN i.is_unique = 1 THEN 'Yes' ELSE 'No' END AS [Unique]
FROM sys.indexes i
JOIN sys.tables t ON i.object_id = t.object_id
WHERE i.name IS NOT NULL 
  AND t.name IN ('User', 'Categories', 'Notes', 'Tags', 'NoteTags', 'NoteVersions', 'AutoSaveSnapshots')
ORDER BY t.name, i.name;
GO

PRINT '';
PRINT '============================================================';
PRINT '          ✅ TẤT CẢ TRUY VẤN ĐÃ HOÀN THÀNH!';
PRINT '============================================================';
PRINT '';
PRINT 'File này bao gồm:';
PRINT '• 7 bảng: User, Categories, Notes, Tags, NoteTags, NoteVersions, AutoSaveSnapshots';
PRINT '• 11 Stored Procedures cho CRUD và Version History';
PRINT '• 1 View: vw_NotesWithVersions';
PRINT '• Indexes tối ưu performance';
PRINT '• 3 Users mẫu với notes, categories, tags';
PRINT '• 13 truy vấn hiển thị dữ liệu các bảng';
PRINT '';
PRINT '============================================================';
GO
