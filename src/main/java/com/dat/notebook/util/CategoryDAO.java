package com.dat.notebook.util;

import com.dat.notebook.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho Category
 * Phiên bản tạm: lưu trong memory (chưa kết nối database)
 */
public class CategoryDAO {

    // Lưu tạm trong memory
    private static List<Category> categories = new ArrayList<>();
    private static int nextId = 1;

    static {
        // Dữ liệu mẫu
        categories.add(new Category(nextId++, "Công việc", "#4CAF50"));
        categories.add(new Category(nextId++, "Cá nhân", "#2196F3"));
        categories.add(new Category(nextId++, "Học tập", "#FF9800"));
    }

    /**
     * Lấy tất cả danh mục
     */
    public List<Category> getAllCategories() {
        try (Connection conn = DBConnection.tryGetConnection()) {
            if (conn == null) {
                return new ArrayList<>(categories);
            }

            String sql = "SELECT id, name, color FROM Categories ORDER BY id ASC";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                List<Category> result = new ArrayList<>();
                while (rs.next()) {
                    Category category = new Category();
                    category.setId(rs.getInt("id"));
                    category.setName(rs.getString("name"));
                    category.setColor(rs.getString("color"));
                    result.add(category);
                }
                return result;
            }
        } catch (SQLException e) {
            return new ArrayList<>(categories);
        }
    }

    /**
     * Lấy danh mục theo ID
     */
    public Category getCategoryById(int id) {
        try (Connection conn = DBConnection.tryGetConnection()) {
            if (conn == null) {
                for (Category cat : categories) {
                    if (cat.getId() == id) {
                        return cat;
                    }
                }
                return null;
            }

            String sql = "SELECT id, name, color FROM Categories WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    Category category = new Category();
                    category.setId(rs.getInt("id"));
                    category.setName(rs.getString("name"));
                    category.setColor(rs.getString("color"));
                    return category;
                }
            }
        } catch (SQLException e) {
            for (Category cat : categories) {
                if (cat.getId() == id) {
                    return cat;
                }
            }
            return null;
        }
    }

    /**
     * Thêm mới danh mục
     */
    public boolean insertCategory(Category category) {
        try (Connection conn = DBConnection.tryGetConnection()) {
            if (conn == null) {
                category.setId(nextId++);
                return categories.add(category);
            }

            String sql = "INSERT INTO Categories (name, color) VALUES (?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, category.getName());
                ps.setString(2, category.getColor());
                int affected = ps.executeUpdate();
                if (affected <= 0) {
                    return false;
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        category.setId(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            category.setId(nextId++);
            return categories.add(category);
        }
    }

    /**
     * Cập nhật danh mục
     */
    public boolean updateCategory(Category updatedCat) {
        try (Connection conn = DBConnection.tryGetConnection()) {
            if (conn == null) {
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).getId() == updatedCat.getId()) {
                        categories.set(i, updatedCat);
                        return true;
                    }
                }
                return false;
            }

            String sql = "UPDATE Categories SET name = ?, color = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, updatedCat.getName());
                ps.setString(2, updatedCat.getColor());
                ps.setInt(3, updatedCat.getId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId() == updatedCat.getId()) {
                    categories.set(i, updatedCat);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Xóa danh mục
     */
    public boolean deleteCategory(int id) {
        try (Connection conn = DBConnection.tryGetConnection()) {
            if (conn == null) {
                return categories.removeIf(cat -> cat.getId() == id);
            }
            String sql = "DELETE FROM Categories WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            return categories.removeIf(cat -> cat.getId() == id);
        }
    }
}
