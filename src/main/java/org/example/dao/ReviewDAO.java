package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    private static final String BASE_SELECT = """
            SELECT r.review_id, r.product_id, p.name AS product_name,
                   r.user_id, u.full_name AS user_name,
                   r.order_id, r.rating, r.title, r.body, r.is_approved, r.created_at
            FROM   reviews r
            JOIN   products p ON p.product_id = r.product_id
            JOIN   users    u ON u.user_id    = r.user_id
            """;

    private Connection conn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public Review insert(Review review) throws SQLException {
        String sql = """
                INSERT INTO reviews (product_id, user_id, order_id, rating, title, body)
                VALUES (?,?,?,?,?,?)
                RETURNING review_id, created_at
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, review.getProductId());
            ps.setLong(2, review.getUserId());
            if (review.getOrderId() != null) ps.setLong(3, review.getOrderId());
            else                              ps.setNull(3, Types.BIGINT);
            ps.setInt(4, review.getRating());
            ps.setString(5, review.getTitle());
            ps.setString(6, review.getBody());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    review.setReviewId(rs.getLong("review_id"));
                    review.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
        }
        return review;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<Review> findByProductId(long productId) throws SQLException {
        String sql = BASE_SELECT + " WHERE r.product_id = ? ORDER BY r.created_at DESC";
        List<Review> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Review> findAll() throws SQLException {
        String sql = BASE_SELECT + " ORDER BY r.created_at DESC";
        List<Review> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // ── Update: approve ───────────────────────────────────────────────────────

    public boolean approve(long reviewId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE reviews SET is_approved = TRUE WHERE review_id = ?")) {
            ps.setLong(1, reviewId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public boolean delete(long reviewId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM reviews WHERE review_id = ?")) {
            ps.setLong(1, reviewId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Review map(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setReviewId(rs.getLong("review_id"));
        r.setProductId(rs.getLong("product_id"));
        r.setProductName(rs.getString("product_name"));
        r.setUserId(rs.getLong("user_id"));
        r.setUserName(rs.getString("user_name"));
        long oid = rs.getLong("order_id");
        if (!rs.wasNull()) r.setOrderId(oid);
        r.setRating(rs.getInt("rating"));
        r.setTitle(rs.getString("title"));
        r.setBody(rs.getString("body"));
        r.setApproved(rs.getBoolean("is_approved"));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return r;
    }
}
