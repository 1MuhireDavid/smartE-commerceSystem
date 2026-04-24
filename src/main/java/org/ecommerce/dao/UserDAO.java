package org.ecommerce.dao;

import org.ecommerce.config.DatabaseConfig;
import org.ecommerce.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private Connection conn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public User insert(User user) throws SQLException {
        String sql = """
                INSERT INTO users (username, email, password_hash, full_name, phone, address, role, is_active)
                VALUES (?,?,?,?,?,?,?,?)
                RETURNING user_id, created_at, updated_at
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getAddress());
            ps.setString(7, user.getRole() == null ? "customer" : user.getRole());
            ps.setBoolean(8, user.isActive());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setUserId(rs.getLong("user_id"));
                    user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return user;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY full_name";
        List<User> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<User> search(String keyword, String role) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM users WHERE 1=1");
        if (keyword != null && !keyword.isBlank())
            sql.append(" AND (lower(full_name) LIKE lower(?) OR lower(email) LIKE lower(?)" +
                       " OR lower(username) LIKE lower(?))");
        if (role != null && !role.isBlank())
            sql.append(" AND role = ?");
        sql.append(" ORDER BY full_name LIMIT 500");

        try (PreparedStatement ps = conn().prepareStatement(sql.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            if (role != null && !role.isBlank())
                ps.setString(idx, role);
            List<User> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        }
    }

    public User findByUsername(String username) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM users WHERE lower(username) = lower(?) LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public User findById(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM users WHERE user_id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public List<User> findBySellers() throws SQLException {
        String sql = "SELECT * FROM users WHERE role IN ('seller','admin') AND is_active = TRUE ORDER BY full_name";
        List<User> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public boolean existsByUsername(String username) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT 1 FROM users WHERE lower(username) = lower(?) LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT 1 FROM users WHERE lower(email) = lower(?) LIMIT 1")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean existsByUsernameExcluding(String username, long excludeId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT 1 FROM users WHERE lower(username) = lower(?) AND user_id <> ? LIMIT 1")) {
            ps.setString(1, username);
            ps.setLong(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public boolean existsByEmailExcluding(String email, long excludeId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT 1 FROM users WHERE lower(email) = lower(?) AND user_id <> ? LIMIT 1")) {
            ps.setString(1, email);
            ps.setLong(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public boolean update(User user) throws SQLException {
        String sql = """
                UPDATE users
                SET    username = ?, email = ?, full_name = ?,
                       phone = ?, address = ?, role = ?, is_active = ?
                WHERE  user_id = ?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getAddress());
            ps.setString(6, user.getRole());
            ps.setBoolean(7, user.isActive());
            ps.setLong(8, user.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updatePassword(long userId, String passwordHash) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE users SET password_hash = ? WHERE user_id = ?")) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public boolean delete(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM users WHERE user_id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private User map(ResultSet rs) throws SQLException {
        Timestamp upd = rs.getTimestamp("updated_at");
        return new User(
            rs.getLong("user_id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("full_name"),
            rs.getString("phone"),
            rs.getString("address"),
            rs.getString("role"),
            rs.getBoolean("is_active"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            upd == null ? null : upd.toLocalDateTime()
        );
    }
}
