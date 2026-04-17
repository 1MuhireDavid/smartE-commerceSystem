package org.ecommerce.dao;

import org.ecommerce.config.DatabaseConfig;
import org.ecommerce.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    private Connection conn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public Category insert(Category category) throws SQLException {
        String sql = """
                INSERT INTO categories (parent_id, name, slug, is_active, display_order)
                VALUES (?, ?, ?, ?, ?)
                RETURNING category_id, created_at
                """;
        // categories doesn't have created_at in schema — use current_timestamp fallback
        String sqlNoTs = """
                INSERT INTO categories (parent_id, name, slug, is_active, display_order)
                VALUES (?, ?, ?, ?, ?)
                RETURNING category_id
                """;
        try (PreparedStatement ps = conn().prepareStatement(sqlNoTs)) {
            setNullableParent(ps, 1, category.getParentId());
            ps.setString(2, category.getName());
            ps.setString(3, slugify(category.getName()));
            ps.setBoolean(4, category.isActive());
            ps.setInt(5, category.getDisplayOrder());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) category.setId(rs.getInt("category_id"));
            }
        }
        return category;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<Category> findAll() throws SQLException {
        String sql = """
                SELECT c.category_id, c.parent_id, p.name AS parent_name,
                       c.name, c.slug, c.is_active, c.display_order
                FROM   categories c
                LEFT JOIN categories p ON p.category_id = c.parent_id
                ORDER  BY c.display_order, c.name
                """;
        List<Category> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Category> findActive() throws SQLException {
        String sql = """
                SELECT c.category_id, c.parent_id, p.name AS parent_name,
                       c.name, c.slug, c.is_active, c.display_order
                FROM   categories c
                LEFT JOIN categories p ON p.category_id = c.parent_id
                WHERE  c.is_active = TRUE
                ORDER  BY c.display_order, c.name
                """;
        List<Category> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Category findById(int id) throws SQLException {
        String sql = """
                SELECT c.category_id, c.parent_id, p.name AS parent_name,
                       c.name, c.slug, c.is_active, c.display_order
                FROM   categories c
                LEFT JOIN categories p ON p.category_id = c.parent_id
                WHERE  c.category_id = ?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public boolean existsBySlug(String slug) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT 1 FROM categories WHERE slug = ? LIMIT 1")) {
            ps.setString(1, slug);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean existsBySlugExcluding(String slug, int excludeId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT 1 FROM categories WHERE slug = ? AND category_id <> ? LIMIT 1")) {
            ps.setString(1, slug);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public boolean update(Category category) throws SQLException {
        String sql = """
                UPDATE categories
                SET    parent_id = ?, name = ?, slug = ?,
                       is_active = ?, display_order = ?
                WHERE  category_id = ?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            setNullableParent(ps, 1, category.getParentId());
            ps.setString(2, category.getName());
            ps.setString(3, slugify(category.getName()));
            ps.setBoolean(4, category.isActive());
            ps.setInt(5, category.getDisplayOrder());
            ps.setInt(6, category.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public boolean delete(int id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM categories WHERE category_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setNullableParent(PreparedStatement ps, int idx, int parentId)
            throws SQLException {
        if (parentId > 0) ps.setInt(idx, parentId);
        else              ps.setNull(idx, Types.INTEGER);
    }

    public static String slugify(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9\\s-]", "")
                   .replaceAll("\\s+", "-")
                   .replaceAll("-+", "-");
    }

    private Category map(ResultSet rs) throws SQLException {
        return new Category(
            rs.getInt("category_id"),
            rs.getInt("parent_id"),
            rs.getString("parent_name"),
            rs.getString("name"),
            rs.getString("slug"),
            rs.getBoolean("is_active"),
            rs.getInt("display_order"),
            null  // categories table has no created_at column in this schema
        );
    }
}
