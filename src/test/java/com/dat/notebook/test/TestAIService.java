package com.dat.notebook.test;

import com.dat.notebook.model.Note;
import com.dat.notebook.service.AIService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TestAIService - Test nhanh các chức năng AI
 * 
 * Chạy file này để test AIService KHÔNG CẦN UI
 * 
 * @author SmartNotebook Team
 */
public class TestAIService {

    public static void main(String[] args) {
        System.out.println("=== TEST AI SERVICE ===\n");
        
        // 1. Khởi tạo service
        AIService aiService = AIService.getInstance();
        System.out.println("✓ AIService initialized\n");
        
        // 2. Tạo note mẫu
        Note testNote = createSampleNote();
        System.out.println("✓ Sample note created");
        System.out.println("Title: " + testNote.getTitle());
        System.out.println("Content length: " + testNote.getContent().length() + " chars\n");
        
        // 3. Test Tóm tắt
        System.out.println("--- TEST 1: TÓM TẮT ---");
        String summary = aiService.summarizeNote(testNote.getContent());
        System.out.println("Result:");
        System.out.println(summary);
        System.out.println();
        
        // 4. Test Gợi ý tiêu đề
        System.out.println("--- TEST 2: GỢI Ý TIÊU ĐỀ ---");
        String title = aiService.suggestTitle(testNote.getContent());
        System.out.println("Suggested title: " + title);
        System.out.println();
        
        // 5. Test Gợi ý tags
        System.out.println("--- TEST 3: GỢI Ý TAGS ---");
        List<String> tags = aiService.suggestTags(testNote.getContent());
        System.out.println("Suggested tags: " + aiService.formatTags(tags));
        System.out.println();
        
        // 6. Test Phân tích toàn diện
        System.out.println("--- TEST 4: PHÂN TÍCH TOÀN DIỆN ---");
        AIService.AIResult result = aiService.analyzeNote(testNote.getContent());
        System.out.println("Summary:");
        System.out.println(result.getSummary());
        System.out.println("\nTitle: " + result.getSuggestedTitle());
        System.out.println("Tags: " + aiService.formatTags(result.getSuggestedTags()));
        System.out.println();
        
        System.out.println("=== ALL TESTS COMPLETED ===");
    }
    
    private static Note createSampleNote() {
        Note note = new Note();
        note.setId(999);
        note.setTitle("Sample Note for AI Testing");
        note.setContent(
            "Hôm nay tôi đã học về JavaFX và kiến trúc MVC. " +
            "JavaFX là một framework UI hiện đại cho Java desktop applications. " +
            "Kiến trúc MVC giúp tách biệt logic và UI, làm code dễ bảo trì hơn. " +
            "\n\n" +
            "Một số điểm quan trọng:\n" +
            "1. Model - Chứa dữ liệu và logic nghiệp vụ\n" +
            "2. View - Hiển thị UI cho người dùng\n" +
            "3. Controller - Xử lý tương tác giữa Model và View\n" +
            "\n" +
            "Việc áp dụng đúng MVC sẽ giúp dự án dễ mở rộng và test. " +
            "Tôi cần nhớ nguyên tắc: Service không được biết UI tồn tại!"
        );
        note.setStatus("REGULAR");
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        note.setUserId(1);
        
        return note;
    }
}
