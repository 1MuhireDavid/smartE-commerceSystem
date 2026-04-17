package org.ecommerce.dao;

import org.ecommerce.config.DatabaseConfig;
import org.ecommerce.model.Cart;
import org.ecommerce.model.CartItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartDAO {

    private Connection conn() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    // ── Cart CRUD ─────────────────────────────────────────────────────────────

    public List<Cart> findAll() throws SQLException {
        String sql = """
                SELECT c.cart_id, c.user_id, u.full_name AS user_full_name,
                       c.is_active, c.created_at, c.updated_at
                FROM   carts c
                JOIN   users u ON u.user_id = c.user_id
                ORDER  BY c.updated_at DESC
                LIMIT  500
                """;
        List<Cart> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapCart(rs));
        }
        return list;
    }

    public Cart findById(long cartId) throws SQLException {
        String sql = """
                SELECT c.cart_id, c.user_id, u.full_name AS user_full_name,
                       c.is_active, c.created_at, c.updated_at
                FROM   carts c
                JOIN   users u ON u.user_id = c.user_id
                WHERE  c.cart_id = ?
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cart cart = mapCart(rs);
                    cart.setItems(findItemsByCartId(cartId));
                    return cart;
                }
            }
        }
        return null;
    }

    public List<Cart> findByUserId(long userId) throws SQLException {
        String sql = """
                SELECT c.cart_id, c.user_id, u.full_name AS user_full_name,
                       c.is_active, c.created_at, c.updated_at
                FROM   carts c
                JOIN   users u ON u.user_id = c.user_id
                WHERE  c.user_id = ?
                ORDER  BY c.is_active DESC, c.updated_at DESC
                """;
        List<Cart> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCart(rs));
            }
        }
        return list;
    }

    public Cart insert(long userId) throws SQLException {
        String sql = """
                INSERT INTO carts (user_id, is_active)
                VALUES (?, TRUE)
                RETURNING cart_id, created_at, updated_at
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cart cart = new Cart();
                    cart.setCartId(rs.getLong("cart_id"));
                    cart.setUserId(userId);
                    cart.setActive(true);
                    cart.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    cart.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    return cart;
                }
            }
        }
        return null;
    }

    public boolean deactivate(long cartId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE carts SET is_active = FALSE WHERE cart_id = ?")) {
            ps.setInt(1, (int) cartId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(long cartId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM carts WHERE cart_id = ?")) {
            ps.setLong(1, cartId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── CartItem CRUD ─────────────────────────────────────────────────────────

    public List<CartItem> findItemsByCartId(long cartId) throws SQLException {
        String sql = """
                SELECT ci.cart_item_id, ci.cart_id, ci.product_id,
                       p.name AS product_name, ci.quantity, ci.unit_price, ci.added_at
                FROM   cart_items ci
                JOIN   products   p ON p.product_id = ci.product_id
                WHERE  ci.cart_id = ?
                ORDER  BY ci.added_at
                """;
        List<CartItem> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, cartId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapItem(rs));
            }
        }
        return list;
    }

    public CartItem addItem(CartItem item) throws SQLException {
        String sql = """
                INSERT INTO cart_items (cart_id, product_id, quantity, unit_price)
                VALUES (?,?,?,?)
                ON CONFLICT (cart_id, product_id)
                DO UPDATE SET quantity = cart_items.quantity + EXCLUDED.quantity
                RETURNING cart_item_id, added_at
                """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, item.getCartId());
            ps.setLong(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitPrice());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    item.setCartItemId(rs.getLong("cart_item_id"));
                    item.setAddedAt(rs.getTimestamp("added_at").toLocalDateTime());
                }
            }
        }
        return item;
    }

    public boolean updateItemQty(long cartItemId, int newQty) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "UPDATE cart_items SET quantity = ? WHERE cart_item_id = ?")) {
            ps.setInt(1, newQty);
            ps.setLong(2, cartItemId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean removeItem(long cartItemId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM cart_items WHERE cart_item_id = ?")) {
            ps.setLong(1, cartItemId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Cart mapCart(ResultSet rs) throws SQLException {
        Cart c = new Cart();
        c.setCartId(rs.getLong("cart_id"));
        c.setUserId(rs.getLong("user_id"));
        c.setUserFullName(rs.getString("user_full_name"));
        c.setActive(rs.getBoolean("is_active"));
        c.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp upd = rs.getTimestamp("updated_at");
        if (upd != null) c.setUpdatedAt(upd.toLocalDateTime());
        return c;
    }

    private CartItem mapItem(ResultSet rs) throws SQLException {
        CartItem i = new CartItem();
        i.setCartItemId(rs.getLong("cart_item_id"));
        i.setCartId(rs.getLong("cart_id"));
        i.setProductId(rs.getLong("product_id"));
        i.setProductName(rs.getString("product_name"));
        i.setQuantity(rs.getInt("quantity"));
        i.setUnitPrice(rs.getBigDecimal("unit_price"));
        Timestamp t = rs.getTimestamp("added_at");
        if (t != null) i.setAddedAt(t.toLocalDateTime());
        return i;
    }
}
