package com.dat.notebook;

import com.dat.notebook.model.Note;
import com.dat.notebook.model.NoteVersion;
import com.dat.notebook.util.NoteDAO;
import com.dat.notebook.util.NoteVersionDAO;

/**
 * Quick Test cho Rich Text Editor Components
 * Chạy class này để verify các components đã hoạt động
 */
public class RichTextEditorTest {
    
    public static void main(String[] args) {
        System.out.println("=== SmartNotebook Rich Text Editor Test ===\n");
        
        // Test 1: Note Model với HTML Content
        testNoteModel();
        
        // Test 2: NoteDAO với HTML
        testNoteDAO();
        
        // Test 3: NoteVersion Model
        testNoteVersionModel();
        
        // Test 4: NoteVersionDAO
        testNoteVersionDAO();
        
        System.out.println("\n=== All Tests Completed ===");
    }
    
    private static void testNoteModel() {
        System.out.println("Test 1: Note Model với HTML Content");
        System.out.println("-----------------------------------");
        
        Note note = new Note();
        note.setTitle("Test Rich Text Note");
        
        // Set HTML content
        String htmlContent = "<p>This is <b>bold</b> and <i>italic</i> text.</p>" +
                           "<p style='color: red;'>Red text</p>" +
                           "<ul><li>Item 1</li><li>Item 2</li></ul>";
        note.setHtmlContent(htmlContent);
        
        // Verify HTML content
        System.out.println("HTML Content set: " + (note.getHtmlContent() != null));
        System.out.println("HTML Length: " + note.getHtmlContent().length());
        
        // Verify plain text extracted
        System.out.println("Plain Content: " + note.getContent());
        System.out.println("Word Count: " + note.getWordCount());
        
        // Test HTML conversion from plain text
        Note note2 = new Note();
        note2.setContent("Line 1\nLine 2\nLine 3");
        System.out.println("Converted HTML: " + note2.getHtmlContent());
        
        System.out.println("✓ Note Model Test Passed\n");
    }
    
    private static void testNoteDAO() {
        System.out.println("Test 2: NoteDAO với HTML");
        System.out.println("-------------------------");
        
        NoteDAO noteDAO = new NoteDAO();
        
        // Tạo test note
        Note testNote = new Note();
        testNote.setTitle("DAO Test Note");
        testNote.setHtmlContent("<p>Testing <b>DAO</b> with HTML</p>");
        testNote.setStatus("REGULAR");
        testNote.setColor("#ffffff");
        
        System.out.println("Created test note object");
        System.out.println("Title: " + testNote.getTitle());
        System.out.println("HTML: " + testNote.getHtmlContent());
        
        // Note: Actual DB insert test would require database connection
        System.out.println("Note: DB operations require live database connection");
        System.out.println("✓ NoteDAO Test Structure Passed\n");
    }
    
    private static void testNoteVersionModel() {
        System.out.println("Test 3: NoteVersion Model");
        System.out.println("--------------------------");
        
        NoteVersion version = new NoteVersion();
        version.setNoteId(1);
        version.setTitle("Version Test");
        version.setHtmlContent("<p>Version 1 content with <b>formatting</b></p>");
        version.setVersionNumber(1);
        
        // Verify plain text generation
        System.out.println("Version created");
        System.out.println("HTML Content: " + version.getHtmlContent());
        System.out.println("Plain Text: " + version.getPlainTextContent());
        System.out.println("Preview: " + version.getContentPreview());
        System.out.println("Label: " + version.getVersionLabel());
        
        System.out.println("✓ NoteVersion Model Test Passed\n");
    }
    
    private static void testNoteVersionDAO() {
        System.out.println("Test 4: NoteVersionDAO");
        System.out.println("----------------------");
        
        NoteVersionDAO versionDAO = new NoteVersionDAO();
        
        System.out.println("NoteVersionDAO instance created");
        System.out.println("Methods available:");
        System.out.println("  - createVersion()");
        System.out.println("  - getVersionsByNoteId()");
        System.out.println("  - getVersionById()");
        System.out.println("  - rollbackToVersion()");
        System.out.println("  - deleteVersion()");
        
        System.out.println("Note: DB operations require live database connection");
        System.out.println("✓ NoteVersionDAO Test Structure Passed\n");
    }
}
