package org.ecommerce.dao;

import org.ecommerce.config.DatabaseConfig;
import org.ecommerce.model.Inventory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryDAO {

    private static final String BASE_SELECT = """
            SELECT i.inventory_id, i.product_id,
                   p.name AS product_name, c.name AS category_name,
                   i.qty_in_stock, i.reserved_qty, i.reorder_level, i.last_updated
            FROM   inventory i
            JOIN   products   p ON p.product_id  = i.product_id
            LEFT JOIN categories c ON c.category_id = p.category_id
            """;

    private Connection conn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<Inventory> findAll() throws SQLException {
        List<Inventory> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(BASE_SELECT + " ORDER BY p.name")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** All items at or below reorder level (low stock alert). */
    public List<Inventory> findLowStock() throws SQLException {
        String sql = BASE_SELECT +
                     " WHERE i.qty_in_stock <= i.reorder_level ORDER BY i.qty_in_stock";
        List<Inventory> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Inventory> search(String keyword) throws SQLException {
        String sql = BASE_SELECT + " WHERE lower(p.name) LIKE lower(?) ORDER BY p.name";
        List<Inventory> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, "%" + (keyword == null ? "" : keyword.trim()) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public Inventory findByProductId(long productId) throws SQLException {
        String sql = BASE_SELECT + " WHERE i.product_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public boolean updateStock(long productId, int qtyInStock, int reorderLevel)
            throws SQLException {
        String sql = """
                UPDATE inventory
                SET    qty_in_stock = ?, reorder_level = ?
                WHERE  product_id = ?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, qtyInStock);
            ps.setInt(2, reorderLevel);
            ps.setLong(3, productId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean adjustStock(long productId, int delta) throws SQLException {
        String sql = """
                UPDATE inventory
                SET    qty_in_stock = GREATEST(0, qty_in_stock + ?)
                WHERE  product_id = ?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, productId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Reserve stock for a pending order. Returns 0 if available qty is insufficient. */
    public int reserveStock(long productId, int qty) throws SQLException {
        String sql = """
                UPDATE inventory
                SET    reserved_qty = reserved_qty + ?
                WHERE  product_id = ?
                  AND  (qty_in_stock - reserved_qty) >= ?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setLong(2, productId);
            ps.setInt(3, qty);
            return ps.executeUpdate();
        }
    }

    /** Release a reservation when an order is cancelled. */
    public void releaseReservation(long productId, int qty) throws SQLException {
        String sql = """
                UPDATE inventory
                SET    reserved_qty = GREATEST(0, reserved_qty - ?)
                WHERE  product_id = ?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setLong(2, productId);
            ps.executeUpdate();
        }
    }

    /** Deduct from both qty_in_stock and reserved_qty when an order is completed. */
    public void fulfillReservation(long productId, int qty) throws SQLException {
        String sql = """
                UPDATE inventory
                SET    qty_in_stock = GREATEST(0, qty_in_stock - ?),
                       reserved_qty = GREATEST(0, reserved_qty - ?)
                WHERE  product_id = ?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, qty);
            ps.setLong(3, productId);
            ps.executeUpdate();
        }
    }

    // ── Report: low stock count ───────────────────────────────────────────────

    public int countLowStock() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) FROM inventory WHERE qty_in_stock <= reorder_level")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Inventory map(ResultSet rs) throws SQLException {
        Inventory inv = new Inventory();
        inv.setInventoryId(rs.getLong("inventory_id"));
        inv.setProductId(rs.getLong("product_id"));
        inv.setProductName(rs.getString("product_name"));
        inv.setCategoryName(rs.getString("category_name"));
        inv.setQtyInStock(rs.getInt("qty_in_stock"));
        inv.setReservedQty(rs.getInt("reserved_qty"));
        inv.setReorderLevel(rs.getInt("reorder_level"));
        Timestamp ts = rs.getTimestamp("last_updated");
        if (ts != null) inv.setLastUpdated(ts.toLocalDateTime());
        return inv;
    }
}
