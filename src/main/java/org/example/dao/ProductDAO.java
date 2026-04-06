package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    private static final String BASE_SELECT = """
            SELECT p.product_id, p.seller_id, u.full_name AS seller_name,
                   p.category_id, c.name AS category_name,
                   p.name, p.slug, p.description,
                   p.base_price, p.discount_price, p.status,
                   p.avg_rating, p.review_count,
                   COALESCE(i.qty_in_stock,  0) AS qty_in_stock,
                   COALESCE(i.reserved_qty,  0) AS reserved_qty,
                   COALESCE(i.reorder_level,10) AS reorder_level,
                   p.created_at, p.updated_at
            FROM   products p
            LEFT JOIN users       u ON u.user_id    = p.seller_id
            LEFT JOIN categories  c ON c.category_id = p.category_id
            LEFT JOIN inventory   i ON i.product_id  = p.product_id
            """;

    private Connection conn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public Product insert(Product product) throws SQLException {
        String sql = """
                INSERT INTO products
                    (seller_id, category_id, name, slug, description,
                     base_price, discount_price, status)
                VALUES (?,?,?,?,?,?,?,?)
                RETURNING product_id, created_at, updated_at
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, product.getSellerId());
            setNullableInt(ps, 2, product.getCategoryId());
            ps.setString(3, product.getName());
            ps.setString(4, slugify(product.getName()));
            ps.setString(5, product.getDescription());
            ps.setBigDecimal(6, product.getBasePrice());
            ps.setBigDecimal(7, product.getDiscountPrice());
            ps.setString(8, product.getStatus() == null ? "draft" : product.getStatus());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    product.setId(rs.getLong("product_id"));
                    product.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    product.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        // Create matching inventory row
        String invSql = "INSERT INTO inventory (product_id, qty_in_stock, reserved_qty, reorder_level) " +
                        "VALUES (?,?,0,10) ON CONFLICT (product_id) DO NOTHING";
        try (PreparedStatement ps = conn().prepareStatement(invSql)) {
            ps.setLong(1, product.getId());
            ps.setInt(2, product.getStockQuantity());
            ps.executeUpdate();
        }
        return product;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<Product> findAll() throws SQLException {
        String sql = BASE_SELECT + " WHERE p.status <> 'draft' ORDER BY p.name";
        List<Product> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Product> findAllIncludingDraft() throws SQLException {
        String sql = BASE_SELECT + " ORDER BY p.name";
        List<Product> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Product> search(String keyword, int categoryId) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT + " WHERE 1=1");
        if (keyword != null && !keyword.isBlank())
            // Uses idx_products_name_fts (GIN) — faster than LIKE for text search
            sql.append(" AND to_tsvector('english', p.name) @@ websearch_to_tsquery('english', ?)");
        if (categoryId > 0)
            sql.append(" AND p.category_id = ?");
        sql.append(" ORDER BY p.name");

        try (PreparedStatement ps = conn().prepareStatement(sql.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.isBlank())
                ps.setString(idx++, keyword.trim());
            if (categoryId > 0)
                ps.setInt(idx, categoryId);
            List<Product> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        }
    }

    public List<Product> findPage(int pageSize, int offset) throws SQLException {
        String sql = BASE_SELECT + " ORDER BY p.name LIMIT ? OFFSET ?";
        List<Product> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public Product findById(long id) throws SQLException {
        String sql = BASE_SELECT + " WHERE p.product_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public int count() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM products")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public boolean update(Product product) throws SQLException {
        String sql = """
                UPDATE products
                SET    seller_id = ?, category_id = ?, name = ?, slug = ?,
                       description = ?, base_price = ?, discount_price = ?, status = ?
                WHERE  product_id = ?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, product.getSellerId());
            setNullableInt(ps, 2, product.getCategoryId());
            ps.setString(3, product.getName());
            ps.setString(4, slugify(product.getName()));
            ps.setString(5, product.getDescription());
            ps.setBigDecimal(6, product.getBasePrice());
            ps.setBigDecimal(7, product.getDiscountPrice());
            ps.setString(8, product.getStatus());
            ps.setLong(9, product.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public boolean delete(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM products WHERE product_id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Index management (for benchmarking) ──────────────────────────────────

    public void dropNameIndex() throws SQLException {
        conn().createStatement()
              .execute("DROP INDEX IF EXISTS idx_products_name_btree");
    }

    public void createNameIndex() throws SQLException {
        conn().createStatement()
              .execute("CREATE INDEX IF NOT EXISTS idx_products_name_btree " +
                       "ON products (lower(name))");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setNullableInt(PreparedStatement ps, int idx, int value)
            throws SQLException {
        if (value > 0) ps.setInt(idx, value);
        else           ps.setNull(idx, Types.INTEGER);
    }

    public static String slugify(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9\\s-]", "")
                   .replaceAll("\\s+", "-")
                   .replaceAll("-+", "-")
                   .replaceAll("^-|-$", "");
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("product_id"));
        p.setSellerId(rs.getLong("seller_id"));
        p.setSellerName(rs.getString("seller_name"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setName(rs.getString("name"));
        p.setSlug(rs.getString("slug"));
        p.setDescription(rs.getString("description"));
        p.setBasePrice(rs.getBigDecimal("base_price"));
        p.setDiscountPrice(rs.getBigDecimal("discount_price"));
        p.setStatus(rs.getString("status"));
        p.setAvgRating(rs.getBigDecimal("avg_rating"));
        p.setReviewCount(rs.getInt("review_count"));
        p.setStockQuantity(rs.getInt("qty_in_stock"));
        p.setReservedQty(rs.getInt("reserved_qty"));
        p.setReorderLevel(rs.getInt("reorder_level"));
        p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp upd = rs.getTimestamp("updated_at");
        if (upd != null) p.setUpdatedAt(upd.toLocalDateTime());
        return p;
    }
}
