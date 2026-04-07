package org.example.dao;

import org.example.config.DatabaseConfig;
import org.example.model.Order;
import org.example.model.OrderItem;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    private Connection conn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public Order insert(Order order) throws SQLException {
        String sql = """
                INSERT INTO orders
                    (user_id, order_number, status,
                     subtotal, discount_amount, total_amount, payment_status)
                VALUES (?,?,?,?,?,?,?)
                RETURNING order_id, ordered_at, updated_at
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, order.getUserId());
            ps.setString(2, order.getOrderNumber());
            ps.setString(3, order.getStatus() == null ? "pending" : order.getStatus());
            ps.setBigDecimal(4, order.getSubtotal());
            ps.setBigDecimal(5, order.getDiscountAmount() == null
                                 ? BigDecimal.ZERO : order.getDiscountAmount());
            ps.setBigDecimal(6, order.getTotalAmount());
            ps.setString(7, order.getPaymentStatus() == null ? "unpaid" : order.getPaymentStatus());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    order.setOrderId(rs.getLong("order_id"));
                    order.setOrderedAt(rs.getTimestamp("ordered_at").toLocalDateTime());
                    order.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
        }
        return order;
    }

    public OrderItem insertItem(OrderItem item) throws SQLException {
        String sql = """
                INSERT INTO order_items (order_id, product_id, quantity, unit_price, item_status)
                VALUES (?,?,?,?,?)
                RETURNING order_item_id, total_price
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, item.getOrderId());
            ps.setLong(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitPrice());
            ps.setString(5, item.getItemStatus() == null ? "pending" : item.getItemStatus());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    item.setOrderItemId(rs.getLong("order_item_id"));
                    item.setTotalPrice(rs.getBigDecimal("total_price"));
                }
            }
        }
        return item;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<Order> findAll() throws SQLException {
        String sql = """
                SELECT o.*, u.full_name AS user_full_name
                FROM   orders o
                JOIN   users u ON u.user_id = o.user_id
                ORDER  BY o.ordered_at DESC
                """;
        List<Order> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapOrder(rs));
        }
        return list;
    }

    public List<Order> findUnpaid() throws SQLException {
        String sql = """
                SELECT o.*, u.full_name AS user_full_name
                FROM   orders o
                JOIN   users u ON u.user_id = o.user_id
                WHERE  o.payment_status = 'unpaid'
                ORDER  BY o.ordered_at DESC
                """;
        List<Order> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapOrder(rs));
        }
        return list;
    }

    public List<Order> search(String keyword, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT o.*, u.full_name AS user_full_name
                FROM   orders o
                JOIN   users u ON u.user_id = o.user_id
                WHERE  1=1
                """);
        if (keyword != null && !keyword.isBlank())
            sql.append(" AND (lower(o.order_number) LIKE lower(?)" +
                       " OR lower(u.full_name) LIKE lower(?))");
        if (status != null && !status.isBlank())
            sql.append(" AND o.status = ?");
        sql.append(" ORDER BY o.ordered_at DESC LIMIT 500");

        try (PreparedStatement ps = conn().prepareStatement(sql.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            if (status != null && !status.isBlank())
                ps.setString(idx, status);
            List<Order> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapOrder(rs));
            }
            return list;
        }
    }

    public Order findById(long orderId) throws SQLException {
        String sql = """
                SELECT o.*, u.full_name AS user_full_name
                FROM   orders o
                JOIN   users u ON u.user_id = o.user_id
                WHERE  o.order_id = ?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setItems(findItemsByOrderId(orderId));
                    return order;
                }
            }
        }
        return null;
    }

    public List<OrderItem> findItemsByOrderId(long orderId) throws SQLException {
        String sql = """
                SELECT oi.*, p.name AS product_name
                FROM   order_items oi
                JOIN   products p ON p.product_id = oi.product_id
                WHERE  oi.order_id = ?
                ORDER  BY oi.order_item_id
                """;
        List<OrderItem> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapItem(rs));
            }
        }
        return list;
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public boolean delete(long orderId) throws SQLException {
        // order_items cascade via FK ON DELETE CASCADE
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM orders WHERE order_id = ?")) {
            ps.setLong(1, orderId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Update status ─────────────────────────────────────────────────────────

    public boolean updateStatus(long orderId, String newStatus) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE orders SET status = ? WHERE order_id = ?")) {
            ps.setString(1, newStatus);
            ps.setLong(2, orderId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updatePaymentStatus(long orderId, String paymentStatus) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE orders SET payment_status = ? WHERE order_id = ?")) {
            ps.setString(1, paymentStatus);
            ps.setLong(2, orderId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Report queries ────────────────────────────────────────────────────────

    /** Revenue and order count per status. */
    public List<Object[]> summaryByStatus() throws SQLException {
        String sql = """
                SELECT status, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS revenue
                FROM   orders
                GROUP  BY status
                ORDER  BY cnt DESC
                """;
        List<Object[]> rows = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                rows.add(new Object[]{
                    rs.getString("status"),
                    rs.getLong("cnt"),
                    rs.getBigDecimal("revenue")
                });
        }
        return rows;
    }

    /** Monthly revenue for paid orders. */
    public List<Object[]> monthlyRevenue() throws SQLException {
        String sql = """
                SELECT TO_CHAR(DATE_TRUNC('month', ordered_at), 'YYYY-MM') AS month,
                       COUNT(*)                   AS order_count,
                       COALESCE(SUM(total_amount),0) AS revenue
                FROM   orders
                WHERE  payment_status = 'paid'
                GROUP  BY DATE_TRUNC('month', ordered_at)
                ORDER  BY DATE_TRUNC('month', ordered_at) DESC
                LIMIT  12
                """;
        List<Object[]> rows = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                rows.add(new Object[]{
                    rs.getString("month"),
                    rs.getLong("order_count"),
                    rs.getBigDecimal("revenue")
                });
        }
        return rows;
    }

    /** Top N products by total revenue from order_items. */
    public List<Object[]> topProductsByRevenue(int limit) throws SQLException {
        String sql = """
                SELECT p.name,
                       SUM(oi.total_price)  AS revenue,
                       SUM(oi.quantity)     AS units_sold
                FROM   order_items oi
                JOIN   products    p  ON p.product_id = oi.product_id
                GROUP  BY p.product_id, p.name
                ORDER  BY revenue DESC
                LIMIT  ?
                """;
        List<Object[]> rows = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    rows.add(new Object[]{
                        rs.getString("name"),
                        rs.getBigDecimal("revenue"),
                        rs.getLong("units_sold")
                    });
            }
        }
        return rows;
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getLong("order_id"));
        o.setUserId(rs.getLong("user_id"));
        o.setUserFullName(rs.getString("user_full_name"));
        o.setOrderNumber(rs.getString("order_number"));
        o.setStatus(rs.getString("status"));
        o.setSubtotal(rs.getBigDecimal("subtotal"));
        o.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setPaymentStatus(rs.getString("payment_status"));
        o.setOrderedAt(rs.getTimestamp("ordered_at").toLocalDateTime());
        Timestamp upd = rs.getTimestamp("updated_at");
        if (upd != null) o.setUpdatedAt(upd.toLocalDateTime());
        return o;
    }

    private OrderItem mapItem(ResultSet rs) throws SQLException {
        OrderItem i = new OrderItem();
        i.setOrderItemId(rs.getLong("order_item_id"));
        i.setOrderId(rs.getLong("order_id"));
        i.setProductId(rs.getLong("product_id"));
        i.setProductName(rs.getString("product_name"));
        i.setQuantity(rs.getInt("quantity"));
        i.setUnitPrice(rs.getBigDecimal("unit_price"));
        i.setTotalPrice(rs.getBigDecimal("total_price"));
        i.setItemStatus(rs.getString("item_status"));
        return i;
    }
}
