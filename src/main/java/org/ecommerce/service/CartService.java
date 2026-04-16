package org.ecommerce.service;

import org.ecommerce.cache.InMemoryCache;
import org.ecommerce.dao.CartDAO;
import org.ecommerce.model.Cart;
import org.ecommerce.model.CartItem;

import java.sql.SQLException;
import java.util.List;

public class CartService {

    private final CartDAO dao = new CartDAO();

    private final InMemoryCache<String, List<Cart>> cache = new InMemoryCache<>();
    private static final String ALL_KEY = "carts:all";

    // ── Read (cached) ─────────────────────────────────────────────────────────

    public List<Cart> getAll() throws SQLException {
        List<Cart> cached = cache.get(ALL_KEY);
        if (cached != null) return cached;
        List<Cart> fresh = dao.findAll();
        cache.put(ALL_KEY, fresh);
        return fresh;
    }

    public Cart getById(long cartId) throws SQLException {
        return dao.findById(cartId);
    }

    public List<Cart> getByUserId(long userId) throws SQLException {
        return dao.findByUserId(userId);
    }

    // ── Create ────────────────────────────────────────────────────────────────

    public Cart createCart(long userId) throws SQLException {
        Cart cart = dao.insert(userId);
        cache.clear();
        return cart;
    }

    // ── Cart items ────────────────────────────────────────────────────────────

    public List<CartItem> getItems(long cartId) throws SQLException {
        return dao.findItemsByCartId(cartId);
    }

    public CartItem addItem(CartItem item) throws SQLException {
        CartItem saved = dao.addItem(item);
        cache.clear();
        return saved;
    }

    public void updateItemQty(long cartItemId, int newQty) throws SQLException {
        if (newQty <= 0) throw new IllegalArgumentException("Quantity must be at least 1.");
        dao.updateItemQty(cartItemId, newQty);
        cache.clear();
    }

    public void removeItem(long cartItemId) throws SQLException {
        dao.removeItem(cartItemId);
        cache.clear();
    }

    // ── Deactivate / Delete ───────────────────────────────────────────────────

    public void deactivate(long cartId) throws SQLException {
        dao.deactivate(cartId);
        cache.clear();
    }

    public void delete(long cartId) throws SQLException {
        dao.delete(cartId);
        cache.clear();
    }
}
