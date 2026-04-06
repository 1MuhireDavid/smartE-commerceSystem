package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Payment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    private static final String BASE_SELECT = """
            SELECT p.payment_id, p.order_id, o.order_number,
                   p.payment_method, p.transaction_id,
                   p.amount, p.status, p.paid_at
            FROM   payments p
            JOIN   orders   o ON o.order_id = p.order_id
            """;

    private Connection conn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public Payment insert(Payment payment) throws SQLException {
        String sql = """
                INSERT INTO payments
                    (order_id, payment_method, transaction_id, amount, status, paid_at)
                VALUES (?,?,?,?,?,?)
                RETURNING payment_id
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, payment.getOrderId());
            ps.setString(2, payment.getPaymentMethod());
            if (payment.getTransactionId() != null && !payment.getTransactionId().isBlank())
                ps.setString(3, payment.getTransactionId());
            else
                ps.setNull(3, Types.VARCHAR);
            ps.setBigDecimal(4, payment.getAmount());
            ps.setString(5, payment.getStatus() == null ? "pending" : payment.getStatus());
            if (payment.getPaidAt() != null)
                ps.setTimestamp(6, Timestamp.valueOf(payment.getPaidAt()));
            else
                ps.setNull(6, Types.TIMESTAMP);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) payment.setPaymentId(rs.getLong("payment_id"));
            }
        }
        return payment;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<Payment> findAll() throws SQLException {
        String sql = BASE_SELECT + " ORDER BY p.payment_id DESC LIMIT 500";
        List<Payment> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Payment> search(String keyword, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT + " WHERE 1=1");
        if (keyword != null && !keyword.isBlank())
            sql.append(" AND (lower(o.order_number) LIKE lower(?)" +
                       " OR lower(p.transaction_id) LIKE lower(?))");
        if (status != null && !status.isBlank())
            sql.append(" AND p.status = ?");
        sql.append(" ORDER BY p.payment_id DESC LIMIT 500");

        try (PreparedStatement ps = conn().prepareStatement(sql.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            if (status != null && !status.isBlank())
                ps.setString(idx, status);
            List<Payment> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        }
    }

    public List<Payment> findByOrderId(long orderId) throws SQLException {
        String sql = BASE_SELECT + " WHERE p.order_id = ? ORDER BY p.payment_id";
        List<Payment> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public boolean updateStatus(long paymentId, String status) throws SQLException {
        String sql = """
                UPDATE payments
                SET    status = ?,
                       paid_at = CASE WHEN ? = 'completed' THEN NOW() ELSE paid_at END
                WHERE  payment_id = ?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setLong(3, paymentId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public boolean delete(long paymentId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM payments WHERE payment_id = ?")) {
            ps.setLong(1, paymentId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getLong("payment_id"));
        p.setOrderId(rs.getLong("order_id"));
        p.setOrderNumber(rs.getString("order_number"));
        p.setPaymentMethod(rs.getString("payment_method"));
        p.setTransactionId(rs.getString("transaction_id"));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setStatus(rs.getString("status"));
        Timestamp paid = rs.getTimestamp("paid_at");
        if (paid != null) p.setPaidAt(paid.toLocalDateTime());
        return p;
    }
}
