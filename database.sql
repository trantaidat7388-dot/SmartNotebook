-- SMART NOTEBOOK - DATABASE SETUP (SQL Server 2012+)

-- Tạo Database
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'SmartNotebook')
BEGIN
    CREATE DATABASE SmartNotebook;
    PRINT '✓ Created database: SmartNotebook';
END
GO

USE SmartNotebook;
GO

-- TABLES

-- User Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'User')
BEGIN
    CREATE TABLE [User] (
        user_id INT IDENTITY(1,1) PRIMARY KEY,
        username NVARCHAR(50) NOT NULL UNIQUE,
        password_hash VARCHAR(32) NOT NULL,
        email NVARCHAR(100),
        full_name NVARCHAR(100),
        is_active BIT DEFAULT 1,
        created_at DATETIME DEFAULT GETDATE(),
        updated_at DATETIME DEFAULT GETDATE()
    );
    PRINT '✓ Created table: [User]';
END
GO

-- Categories Table
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

-- Notes Table (Main Table)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Notes')
BEGIN
    CREATE TABLE Notes (
        NoteID INT IDENTITY(1,1) PRIMARY KEY,
        UserID INT NOT NULL,
        CategoryID INT,
        Title NVARCHAR(1000) NOT NULL,
        Content NVARCHAR(MAX),
        HtmlContent NVARCHAR(MAX),
        Summary NVARCHAR(2000),
        Status NVARCHAR(20) DEFAULT 'REGULAR',
        IsFavorite BIT DEFAULT 0,
        IsArchived BIT DEFAULT 0,
        Color NVARCHAR(30) DEFAULT '#ffffff',
        ViewCount INT DEFAULT 0,
        CreatedAt DATETIME DEFAULT GETDATE(),
        UpdatedAt DATETIME DEFAULT GETDATE(),
        CONSTRAINT FK_Notes_User FOREIGN KEY (UserID) REFERENCES [User](user_id),
        CONSTRAINT FK_Notes_Categories FOREIGN KEY (CategoryID) REFERENCES Categories(CategoryID)
    );
    
    CREATE INDEX IX_Notes_UserID ON Notes(UserID);
    CREATE INDEX IX_Notes_Status ON Notes(Status);
    CREATE INDEX IX_Notes_IsFavorite ON Notes(IsFavorite);
    CREATE INDEX IX_Notes_IsArchived ON Notes(IsArchived);
    CREATE INDEX IX_Notes_UserID_IsArchived ON Notes(UserID, IsArchived);
    
    PRINT '✓ Created table: Notes with indexes';
END
ELSE
BEGIN
    IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Notes') AND name = 'HtmlContent')
    BEGIN
        ALTER TABLE Notes ADD HtmlContent NVARCHAR(MAX);
        PRINT '✓ Added column HtmlContent';
    END
    
    ALTER TABLE Notes ALTER COLUMN Title NVARCHAR(1000) NOT NULL;
    ALTER TABLE Notes ALTER COLUMN Summary NVARCHAR(2000);
    PRINT '✓ Updated Title and Summary column sizes';
END
GO

-- Tags Table
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

-- NoteTags Table
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

-- NoteVersions Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'NoteVersions')
BEGIN
    CREATE TABLE NoteVersions (
        VersionID INT PRIMARY KEY IDENTITY(1,1),
        NoteID INT NOT NULL,
        Title NVARCHAR(200) NOT NULL,
        HtmlContent NVARCHAR(MAX),
        PlainTextContent NVARCHAR(MAX),
        VersionNumber INT NOT NULL DEFAULT 1,
        CreatedAt DATETIME NOT NULL DEFAULT GETDATE(),
        CreatedBy INT NULL,
        ChangeDescription NVARCHAR(500),
        CONSTRAINT FK_NoteVersions_Notes FOREIGN KEY (NoteID) REFERENCES Notes(NoteID) ON DELETE CASCADE,
        CONSTRAINT FK_NoteVersions_Users FOREIGN KEY (CreatedBy) REFERENCES [User](user_id) ON DELETE NO ACTION
    );
    
    CREATE INDEX IX_NoteVersions_NoteID ON NoteVersions(NoteID);
    CREATE INDEX IX_NoteVersions_CreatedAt ON NoteVersions(CreatedAt DESC);
    PRINT '✓ Created table: NoteVersions';
END
GO

-- AutoSaveSnapshots Table
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'AutoSaveSnapshots')
BEGIN
    CREATE TABLE AutoSaveSnapshots (
        SnapshotID INT PRIMARY KEY IDENTITY(1,1),
        NoteID INT NOT NULL,
        Title NVARCHAR(200),
        HtmlContent NVARCHAR(MAX),
        SavedAt DATETIME NOT NULL DEFAULT GETDATE(),
        UserID INT NOT NULL,
        CONSTRAINT FK_AutoSave_Notes FOREIGN KEY (NoteID) REFERENCES Notes(NoteID) ON DELETE CASCADE,
        CONSTRAINT FK_AutoSave_Users FOREIGN KEY (UserID) REFERENCES [User](user_id) ON DELETE CASCADE
    );
    
    CREATE INDEX IX_AutoSave_NoteID ON AutoSaveSnapshots(NoteID);
    PRINT '✓ Created table: AutoSaveSnapshots';
END
GO

-- STORED PROCEDURES - CRUD

IF OBJECT_ID('sp_GetNotesByUser', 'P') IS NOT NULL DROP PROCEDURE sp_GetNotesByUser;
IF OBJECT_ID('sp_GetNoteById', 'P') IS NOT NULL DROP PROCEDURE sp_GetNoteById;
IF OBJECT_ID('sp_CreateNote', 'P') IS NOT NULL DROP PROCEDURE sp_CreateNote;
IF OBJECT_ID('sp_UpdateNote', 'P') IS NOT NULL DROP PROCEDURE sp_UpdateNote;
IF OBJECT_ID('sp_DeleteNote', 'P') IS NOT NULL DROP PROCEDURE sp_DeleteNote;
IF OBJECT_ID('sp_RestoreNote', 'P') IS NOT NULL DROP PROCEDURE sp_RestoreNote;
IF OBJECT_ID('sp_SearchNotes', 'P') IS NOT NULL DROP PROCEDURE sp_SearchNotes;
GO

CREATE PROCEDURE sp_GetNotesByUser @UserID INT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT n.NoteID, n.UserID, n.CategoryID, n.Title, n.Content, n.HtmlContent, 
           n.Summary, n.Status, n.IsFavorite, n.IsArchived, n.Color, 
           n.ViewCount, n.CreatedAt, n.UpdatedAt, c.Name AS CategoryName
    FROM Notes n
    LEFT JOIN Categories c ON n.CategoryID = c.CategoryID
    WHERE n.UserID = @UserID AND n.IsArchived = 0
    ORDER BY n.UpdatedAt DESC;
END
GO

CREATE PROCEDURE sp_GetNoteById @NoteID INT, @UserID INT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT NoteID, UserID, CategoryID, Title, Content, HtmlContent, Summary,
           Status, IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt
    FROM Notes 
    WHERE NoteID = @NoteID AND UserID = @UserID;
END
GO

CREATE PROCEDURE sp_CreateNote
    @UserID INT, @Title NVARCHAR(200), @Content NVARCHAR(MAX) = NULL,
    @HtmlContent NVARCHAR(MAX) = NULL, @Summary NVARCHAR(500) = NULL,
    @Status NVARCHAR(20) = 'REGULAR', @IsFavorite BIT = 0,
    @Color NVARCHAR(30) = '#ffffff', @CategoryID INT = NULL
AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO Notes (UserID, CategoryID, Title, Content, HtmlContent, Summary,
                       Status, IsFavorite, IsArchived, Color, ViewCount, CreatedAt, UpdatedAt)
    VALUES (@UserID, @CategoryID, @Title, @Content, @HtmlContent, @Summary,
            @Status, @IsFavorite, 0, @Color, 0, GETDATE(), GETDATE());
    SELECT SCOPE_IDENTITY() AS NewNoteID;
END
GO

CREATE PROCEDURE sp_UpdateNote
    @NoteID INT, @UserID INT, @Title NVARCHAR(200), @Content NVARCHAR(MAX) = NULL,
    @HtmlContent NVARCHAR(MAX) = NULL, @Summary NVARCHAR(500) = NULL,
    @Status NVARCHAR(20) = NULL, @IsFavorite BIT = NULL,
    @Color NVARCHAR(30) = NULL, @CategoryID INT = NULL
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
    WHERE NoteID = @NoteID AND UserID = @UserID;
    SELECT @@ROWCOUNT AS AffectedRows;
END
GO

CREATE PROCEDURE sp_DeleteNote @NoteID INT, @UserID INT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Notes SET IsArchived = 1, UpdatedAt = GETDATE()
    WHERE NoteID = @NoteID AND UserID = @UserID;
    SELECT @@ROWCOUNT AS AffectedRows;
END
GO

CREATE PROCEDURE sp_RestoreNote @NoteID INT, @UserID INT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Notes SET IsArchived = 0, UpdatedAt = GETDATE()
    WHERE NoteID = @NoteID AND UserID = @UserID;
    SELECT @@ROWCOUNT AS AffectedRows;
END
GO

CREATE PROCEDURE sp_SearchNotes @UserID INT, @Keyword NVARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT n.NoteID, n.UserID, n.CategoryID, n.Title, n.Content, n.HtmlContent,
           n.Summary, n.Status, n.IsFavorite, n.IsArchived, n.Color,
           n.ViewCount, n.CreatedAt, n.UpdatedAt, c.Name AS CategoryName
    FROM Notes n
    LEFT JOIN Categories c ON n.CategoryID = c.CategoryID
    WHERE n.UserID = @UserID AND n.IsArchived = 0
      AND (n.Title LIKE '%' + @Keyword + '%' OR n.Content LIKE '%' + @Keyword + '%'
           OR n.HtmlContent LIKE '%' + @Keyword + '%')
    ORDER BY n.UpdatedAt DESC;
END
GO

PRINT '✓ Created CRUD procedures';
GO

-- STORED PROCEDURES - VERSION HISTORY

IF OBJECT_ID('sp_CreateNoteVersion', 'P') IS NOT NULL DROP PROCEDURE sp_CreateNoteVersion;
IF OBJECT_ID('sp_GetNoteVersionHistory', 'P') IS NOT NULL DROP PROCEDURE sp_GetNoteVersionHistory;
IF OBJECT_ID('sp_RollbackToVersion', 'P') IS NOT NULL DROP PROCEDURE sp_RollbackToVersion;
IF OBJECT_ID('sp_CleanupOldSnapshots', 'P') IS NOT NULL DROP PROCEDURE sp_CleanupOldSnapshots;
GO

CREATE PROCEDURE sp_CreateNoteVersion
    @NoteID INT, @Title NVARCHAR(200), @HtmlContent NVARCHAR(MAX),
    @UserID INT, @ChangeDescription NVARCHAR(500) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    DECLARE @NextVersion INT;
    SELECT @NextVersion = ISNULL(MAX(VersionNumber), 0) + 1 FROM NoteVersions WHERE NoteID = @NoteID;
    
    DECLARE @PlainText NVARCHAR(MAX) = @HtmlContent;
    SET @PlainText = REPLACE(@PlainText, '<p>', '');
    SET @PlainText = REPLACE(@PlainText, '</p>', CHAR(10));
    SET @PlainText = REPLACE(@PlainText, '<br>', CHAR(10));
    
    INSERT INTO NoteVersions (NoteID, Title, HtmlContent, PlainTextContent, VersionNumber, CreatedBy, ChangeDescription)
    VALUES (@NoteID, @Title, @HtmlContent, @PlainText, @NextVersion, @UserID, @ChangeDescription);
    
    DELETE FROM AutoSaveSnapshots WHERE NoteID = @NoteID AND UserID = @UserID;
    SELECT SCOPE_IDENTITY() AS NewVersionID, @NextVersion AS VersionNumber;
END
GO

CREATE PROCEDURE sp_GetNoteVersionHistory @NoteID INT, @MaxVersions INT = 50
AS
BEGIN
    SET NOCOUNT ON;
    SELECT TOP (@MaxVersions) v.VersionID, v.NoteID, v.Title, v.VersionNumber,
           v.CreatedAt, v.CreatedBy, v.ChangeDescription, u.username AS CreatedByUsername,
           LEN(v.HtmlContent) AS ContentLength
    FROM NoteVersions v
    LEFT JOIN [User] u ON v.CreatedBy = u.user_id
    WHERE v.NoteID = @NoteID
    ORDER BY v.VersionNumber DESC;
END
GO

CREATE PROCEDURE sp_RollbackToVersion @NoteID INT, @VersionID INT, @UserID INT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;
    
    DECLARE @Title NVARCHAR(200), @HtmlContent NVARCHAR(MAX);
    SELECT @Title = Title, @HtmlContent = HtmlContent
    FROM NoteVersions WHERE VersionID = @VersionID AND NoteID = @NoteID;
    
    IF @Title IS NULL BEGIN ROLLBACK; RAISERROR('Version không tồn tại', 16, 1); RETURN; END
    
    UPDATE Notes SET Title = @Title, HtmlContent = @HtmlContent, Content = @Title, UpdatedAt = GETDATE()
    WHERE NoteID = @NoteID AND UserID = @UserID;
    
    EXEC sp_CreateNoteVersion @NoteID, @Title, @HtmlContent, @UserID, 'Rolled back to earlier version';
    COMMIT;
END
GO

CREATE PROCEDURE sp_CleanupOldSnapshots
AS
BEGIN
    DELETE FROM AutoSaveSnapshots WHERE SavedAt < DATEADD(DAY, -7, GETDATE());
    PRINT 'Đã xóa ' + CAST(@@ROWCOUNT AS VARCHAR) + ' auto-save snapshots cũ';
END
GO

PRINT '✓ Created version history procedures';
GO

-- VIEW

IF OBJECT_ID('vw_NotesWithVersions', 'V') IS NOT NULL DROP VIEW vw_NotesWithVersions;
GO

CREATE VIEW vw_NotesWithVersions AS
SELECT n.NoteID, n.UserID, n.Title, n.HtmlContent, n.Content, n.CreatedAt, n.UpdatedAt,
       n.IsFavorite, n.Status, n.Color, n.CategoryID, n.IsArchived,
       COUNT(v.VersionID) AS VersionCount, MAX(v.VersionNumber) AS LatestVersion
FROM Notes n
LEFT JOIN NoteVersions v ON n.NoteID = v.NoteID
GROUP BY n.NoteID, n.UserID, n.Title, n.HtmlContent, n.Content, n.CreatedAt, 
         n.UpdatedAt, n.IsFavorite, n.Status, n.Color, n.CategoryID, n.IsArchived;
GO

PRINT '✓ Created view: vw_NotesWithVersions';
GO

-- SAMPLE DATA

-- Users (demo: abc123, admin: admin123, dat09: 221761)
IF NOT EXISTS (SELECT * FROM [User] WHERE username = 'demo')
    INSERT INTO [User] (username, password_hash, email, full_name)
    VALUES ('demo', 'e99a18c428cb38d5f260853678922e03', 'demo@smartnotebook.com', 'Demo User');

IF NOT EXISTS (SELECT * FROM [User] WHERE username = 'admin')
    INSERT INTO [User] (username, password_hash, email, full_name)
    VALUES ('admin', '0192023a7bbd73250516f069df18b500', 'admin@smartnotebook.com', 'Administrator');

IF NOT EXISTS (SELECT * FROM [User] WHERE username = 'dat09')
    INSERT INTO [User] (username, password_hash, email, full_name)
    VALUES ('dat09', '7e4cbfdfef9bfb85aca0e3afdf2f7158', 'dat09@smartnotebook.com', 'Dat 09');
GO

-- Categories for demo user
DECLARE @DemoID INT = (SELECT user_id FROM [User] WHERE username = 'demo');
IF @DemoID IS NOT NULL AND NOT EXISTS (SELECT * FROM Categories WHERE UserID = @DemoID)
BEGIN
    INSERT INTO Categories (UserID, Name, Color, Icon, SortOrder) VALUES
    (@DemoID, 'Personal', '#60a5fa', '📝', 1),
    (@DemoID, 'Work', '#f59e0b', '💼', 2),
    (@DemoID, 'Ideas', '#a78bfa', '💡', 3),
    (@DemoID, 'To-do', '#34d399', '✅', 4);
END
GO

-- Categories for dat09 user
DECLARE @Dat09ID INT = (SELECT user_id FROM [User] WHERE username = 'dat09');
IF @Dat09ID IS NOT NULL AND NOT EXISTS (SELECT * FROM Categories WHERE UserID = @Dat09ID)
BEGIN
    INSERT INTO Categories (UserID, Name, Color, Icon, SortOrder) VALUES
    (@Dat09ID, N'Học tập', '#3b82f6', '📚', 1),
    (@Dat09ID, N'Công việc', '#ef4444', '💻', 2),
    (@Dat09ID, N'Dự án', '#8b5cf6', '🚀', 3),
    (@Dat09ID, N'Cá nhân', '#10b981', '🏠', 4),
    (@Dat09ID, N'Ý tưởng', '#f59e0b', '💡', 5);
END
GO

PRINT '✓ Sample data created';
GO

-- Sample Notes for demo user
DECLARE @DemoID2 INT = (SELECT user_id FROM [User] WHERE username = 'demo');
DECLARE @WorkCat INT = (SELECT CategoryID FROM Categories WHERE UserID = @DemoID2 AND Name = 'Work');
DECLARE @IdeasCat INT = (SELECT CategoryID FROM Categories WHERE UserID = @DemoID2 AND Name = 'Ideas');

IF @DemoID2 IS NOT NULL AND NOT EXISTS (SELECT * FROM Notes WHERE UserID = @DemoID2)
BEGIN
    INSERT INTO Notes (UserID, CategoryID, Title, Content, Summary, Status, IsFavorite, Color) VALUES
    (@DemoID2, @WorkCat, 'Project Sprint Planning', 
     'Sprint goals: Complete login module, Implement CRUD operations, Add search functionality. Tasks: Backend API - 3 days, Frontend UI - 2 days, Testing - 2 days.',
     'Sprint planning with milestones and deadlines',
     'URGENT', 1, '#fef3c7'),
    
    (@DemoID2, @IdeasCat, 'AI Feature Ideas',
     'AI features: Auto-tagging, Smart search, Sentiment analysis, Voice notes, Cloud sync.',
     'Ideas for AI enhancements',
     'IDEAS', 1, '#ede9fe'),
    
    (@DemoID2, @WorkCat, 'Meeting Notes',
     'Q4 roadmap discussion. Key points: UI redesign, Performance optimization, Mobile app development.',
     'Q4 planning meeting summary',
     'REGULAR', 0, '#dbeafe');
END
GO

-- Sample Notes for dat09 user
DECLARE @Dat09ID2 INT = (SELECT user_id FROM [User] WHERE username = 'dat09');
DECLARE @HocTapCat INT = (SELECT CategoryID FROM Categories WHERE UserID = @Dat09ID2 AND Name = N'Học tập');
DECLARE @DuAnCat INT = (SELECT CategoryID FROM Categories WHERE UserID = @Dat09ID2 AND Name = N'Dự án');

IF @Dat09ID2 IS NOT NULL AND NOT EXISTS (SELECT * FROM Notes WHERE UserID = @Dat09ID2)
BEGIN
    INSERT INTO Notes (UserID, CategoryID, Title, Content, Summary, Status, IsFavorite, Color) VALUES
    (@Dat09ID2, @HocTapCat, N'Ôn tập Java OOP',
     N'OOP: Encapsulation, Inheritance, Polymorphism, Abstraction. Collections: ArrayList, HashMap. Multi-threading: Thread, Runnable, Synchronized.',
     N'Java OOP concepts review',
     'URGENT', 1, '#dbeafe'),
    
    (@Dat09ID2, @DuAnCat, N'SmartNotebook Ideas',
     N'Features: Dark mode, Rich text editor, Cloud sync, Export to PDF, Collaboration, AI summarization.',
     N'Enhancement ideas for SmartNotebook',
     'IDEAS', 1, '#ede9fe'),
    
    (@Dat09ID2, @HocTapCat, N'SQL Server Notes',
     N'DDL: CREATE, ALTER, DROP. DML: SELECT, INSERT, UPDATE, DELETE. Functions: GETDATE(), COALESCE(), LEN(). Joins: INNER, LEFT, RIGHT.',
     N'SQL Server quick reference',
     'REGULAR', 1, '#e0e7ff');
END
GO

PRINT '✓ Sample notes created';
GO

-- ======================================================================
-- SUMMARY
-- ======================================================================

PRINT '';
PRINT '====================================================';
PRINT '   SMART NOTEBOOK DATABASE - SETUP COMPLETE!';
PRINT '====================================================';
PRINT '';
PRINT 'Demo Accounts:';
PRINT '  demo   | abc123';
PRINT '  admin  | admin123';
PRINT '  dat09  | 221761';
PRINT '====================================================';
GO

-- ======================================================================
-- TRUY XUẤT DỮ LIỆU CÁC BẢNG
-- ======================================================================

PRINT '';
PRINT '====================================================';
PRINT '          TRUY XUẤT DỮ LIỆU CÁC BẢNG';
PRINT '====================================================';
GO

-- 1. Bảng Users
PRINT '---------- 1. BẢNG [USER] ----------';
SELECT 
    user_id AS ID,
    username AS [Username],
    email AS [Email],
    full_name AS [Full Name],
    CASE WHEN is_active = 1 THEN 'Active' ELSE 'Inactive' END AS [Status],
    FORMAT(created_at, 'dd/MM/yyyy HH:mm') AS [Created At]
FROM [User]
ORDER BY user_id;
GO

-- 2. Bảng Categories
PRINT '---------- 2. BẢNG CATEGORIES ----------';
SELECT 
    c.CategoryID AS ID,
    u.username AS [Owner],
    c.Name AS [Category Name],
    c.Icon,
    c.Color,
    c.SortOrder AS [Order],
    FORMAT(c.CreatedAt, 'dd/MM/yyyy') AS [Created At]
FROM Categories c
JOIN [User] u ON c.UserID = u.user_id
ORDER BY u.username, c.SortOrder;
GO

-- 3. Bảng Notes
PRINT '---------- 3. BẢNG NOTES ----------';
SELECT 
    n.NoteID AS ID,
    u.username AS [Owner],
    c.Name AS [Category],
    n.Title,
    LEFT(COALESCE(n.Content, ''), 50) + CASE WHEN LEN(COALESCE(n.Content, '')) > 50 THEN '...' ELSE '' END AS [Preview],
    n.Status,
    CASE WHEN n.IsFavorite = 1 THEN '⭐' ELSE '' END AS [Fav],
    CASE WHEN n.IsArchived = 1 THEN '🗑️' ELSE '' END AS [Archived],
    FORMAT(n.UpdatedAt, 'dd/MM/yyyy') AS [Updated]
FROM Notes n
JOIN [User] u ON n.UserID = u.user_id
LEFT JOIN Categories c ON n.CategoryID = c.CategoryID
ORDER BY u.username, n.UpdatedAt DESC;
GO

-- 4. Bảng Tags
PRINT '---------- 4. BẢNG TAGS ----------';
SELECT 
    t.TagID AS ID,
    u.username AS [Owner],
    t.Name AS [Tag Name],
    t.Color,
    (SELECT COUNT(*) FROM NoteTags nt WHERE nt.TagID = t.TagID) AS [Usage Count],
    FORMAT(t.CreatedAt, 'dd/MM/yyyy') AS [Created]
FROM Tags t
JOIN [User] u ON t.UserID = u.user_id
ORDER BY u.username, t.Name;
GO

-- 5. Bảng NoteTags
PRINT '---------- 5. BẢNG NOTETAGS ----------';
SELECT 
    nt.NoteID,
    n.Title AS [Note Title],
    t.Name AS [Tag Name],
    u.username AS [Owner]
FROM NoteTags nt
JOIN Notes n ON nt.NoteID = n.NoteID
JOIN Tags t ON nt.TagID = t.TagID
JOIN [User] u ON n.UserID = u.user_id
ORDER BY u.username, n.Title;
GO

-- 6. Bảng NoteVersions
PRINT '---------- 6. BẢNG NOTEVERSIONS ----------';
SELECT 
    v.VersionID,
    v.NoteID,
    n.Title AS [Current Title],
    v.VersionNumber AS [Ver],
    u.username AS [Created By],
    FORMAT(v.CreatedAt, 'dd/MM/yyyy HH:mm') AS [Created At]
FROM NoteVersions v
JOIN Notes n ON v.NoteID = n.NoteID
LEFT JOIN [User] u ON v.CreatedBy = u.user_id
ORDER BY v.NoteID, v.VersionNumber DESC;
GO

-- 7. Bảng AutoSaveSnapshots
PRINT '---------- 7. BẢNG AUTOSAVESNAPSHOTS ----------';
SELECT 
    s.SnapshotID,
    s.NoteID,
    n.Title AS [Note Title],
    u.username AS [User],
    FORMAT(s.SavedAt, 'dd/MM/yyyy HH:mm') AS [Saved At]
FROM AutoSaveSnapshots s
JOIN Notes n ON s.NoteID = n.NoteID
JOIN [User] u ON s.UserID = u.user_id
ORDER BY s.SavedAt DESC;
GO

-- ======================================================================
-- THỐNG KÊ TỔNG QUAN
-- ======================================================================

PRINT '';
PRINT '---------- 8. THỐNG KÊ TỔNG QUAN ----------';
SELECT 'Users' AS [Table], COUNT(*) AS [Count] FROM [User]
UNION ALL SELECT 'Categories', COUNT(*) FROM Categories
UNION ALL SELECT 'Notes', COUNT(*) FROM Notes
UNION ALL SELECT 'Tags', COUNT(*) FROM Tags
UNION ALL SELECT 'NoteTags', COUNT(*) FROM NoteTags
UNION ALL SELECT 'NoteVersions', COUNT(*) FROM NoteVersions
UNION ALL SELECT 'AutoSaveSnapshots', COUNT(*) FROM AutoSaveSnapshots
ORDER BY [Table];
GO

PRINT '';
PRINT '====================================================';
PRINT '           ✅ HOÀN THÀNH SETUP DATABASE!';
PRINT '====================================================';
GO
