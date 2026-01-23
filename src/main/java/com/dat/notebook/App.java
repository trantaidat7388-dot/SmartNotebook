package com.dat.notebook;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * ========================================
 * SMART NOTEBOOK - SỔ TAY THÔNG MINH
 * ========================================
 * 
 * Ứng dụng Desktop quản lý ghi chú thông minh
 * Phát triển bằng Java + JavaFX + SQL Server
 * 
 * Tính năng Smart:
 * - Tự động tóm tắt nội dung
 * - Gợi ý tiêu đề thông minh
 * - Tìm kiếm nâng cao với highlight
 * - Trích xuất từ khóa tự động
 * 
 * @author SmartNotebook Team
 * @version 1.0
 * 
 * Kiến trúc:
 * - Model: User, Note, Tag, Category
 * - Repository: CRUD với database
 * - Service: Logic nghiệp vụ
 * - Controller: Xử lý UI
 * - Config: Cấu hình database
 * - Util: Các hàm tiện ích
 */
public class App extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load màn hình đăng nhập
        Parent root = FXMLLoader.load(getClass().getResource("/views/LoginView.fxml"));
        
        primaryStage.setTitle("SmartNotebook - Đăng nhập");
        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();
        
        System.out.println("===========================================");
        System.out.println("   SMART NOTEBOOK - SỔ TAY THÔNG MINH");
        System.out.println("   Version 1.0 - Java Desktop Application");
        System.out.println("===========================================");
    }
    
    @Override
    public void stop() throws Exception {
        // Cleanup khi đóng ứng dụng
        System.out.println("Đang đóng ứng dụng SmartNotebook...");
        super.stop();
    }

    public static void main(String[] args) {
        System.out.println("Khởi động SmartNotebook...");
        launch(args);
    }
}
