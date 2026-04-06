package org.example.service;

import org.example.cache.InMemoryCache;
import org.example.config.DatabaseConfig;
import org.example.dao.OrderDAO;
import org.example.model.Order;
import org.example.model.OrderItem;

import java.sql.SQLException;
import java.util.List;

public class OrderService {

    private final OrderDAO dao = new OrderDAO();

    private final InMemoryCache<String, List<Order>> cache = new InMemoryCache<>();
    private static final String ALL_KEY    = "orders:all";
    private static final String SEARCH_PFX = "orders:search:";

    public static final List<String> ORDER_STATUSES =
        List.of("pending", "processing", "shipped", "delivered", "cancelled");

    // ── Create (transactional: order + items atomically) ──────────────────────

    public Order create(Order order, List<OrderItem> items) throws SQLException {
        DatabaseConfig db = DatabaseConfig.getInstance();
        db.beginTransaction();
        try {
            Order saved = dao.insert(order);
            for (OrderItem item : items) {
                item.setOrderId(saved.getOrderId());
                dao.insertItem(item);
            }
            db.commit();
            cache.clear();
            return saved;
        } catch (SQLException e) {
            db.rollback();
            throw e;
        }
    }

    // ── Read (cached) ─────────────────────────────────────────────────────────

    public List<Order> getAll() throws SQLException {
        List<Order> cached = cache.get(ALL_KEY);
        if (cached != null) return cached;
        List<Order> fresh = dao.findAll();
        cache.put(ALL_KEY, fresh);
        return fresh;
    }

    public List<Order> search(String keyword, String status) throws SQLException {
        String key = SEARCH_PFX + normalize(keyword) + ":" + normalize(status);
        List<Order> cached = cache.get(key);
        if (cached != null) return cached;
        List<Order> results = dao.search(keyword, status);
        cache.put(key, results);
        return results;
    }

    public Order getById(long orderId) throws SQLException {
        return dao.findById(orderId);
    }

    public List<OrderItem> getItemsForOrder(long orderId) throws SQLException {
        return dao.findItemsByOrderId(orderId);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void updateStatus(long orderId, String newStatus) throws SQLException {
        dao.updateStatus(orderId, newStatus);
        cache.clear();
    }

    public void updatePaymentStatus(long orderId, String paymentStatus) throws SQLException {
        dao.updatePaymentStatus(orderId, paymentStatus);
        cache.clear();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(long orderId) throws SQLException {
        dao.delete(orderId);
        cache.clear();
    }

    // ── Report (always fresh — no cache) ─────────────────────────────────────

    public List<Object[]> getSummaryByStatus()           throws SQLException { return dao.summaryByStatus(); }
    public List<Object[]> getMonthlyRevenue()            throws SQLException { return dao.monthlyRevenue(); }
    public List<Object[]> getTopProductsByRevenue(int n) throws SQLException { return dao.topProductsByRevenue(n); }

    // ── Cache info ────────────────────────────────────────────────────────────

    public int  getCacheSize() { return cache.size(); }
    public void clearCache()   { cache.clear(); }

    private String normalize(String s) { return s == null ? "" : s.trim().toLowerCase(); }
}
