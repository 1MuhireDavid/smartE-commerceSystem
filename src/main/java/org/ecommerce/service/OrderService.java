package org.ecommerce.service;

import org.ecommerce.cache.InMemoryCache;
import org.ecommerce.config.DatabaseConfig;
import org.ecommerce.dao.OrderDAO;
import org.ecommerce.model.Order;
import org.ecommerce.model.OrderItem;

import java.sql.SQLException;
import java.util.List;

public class OrderService {

    private final OrderDAO dao = new OrderDAO();
    private final InventoryService inventoryService = new InventoryService();

    private final InMemoryCache<String, List<Order>> cache = new InMemoryCache<>();
    private static final String ALL_KEY    = "orders:all";
    private static final String SEARCH_PFX = "orders:search:";

    public static final List<String> ORDER_STATUSES =
        List.of("pending", "processing", "completed", "cancelled");

    // ── Create (transactional: order + items atomically) ──────────────────────

    public Order create(Order order, List<OrderItem> items) throws SQLException {
        DatabaseConfig db = DatabaseConfig.getInstance();
        db.beginTransaction();
        try {
            Order saved = dao.insert(order);
            for (OrderItem item : items) {
                item.setOrderId(saved.getOrderId());
                dao.insertItem(item);
                inventoryService.reserveStock(item.getProductId(), item.getQuantity());
            }
            db.commit();
            cache.clear();
            inventoryService.clearCache();
            ActivityLogService.get().log(saved.getUserId(), "order_placed",
                    "order_id", saved.getOrderId(),
                    "order_number", saved.getOrderNumber(),
                    "total", saved.getTotalAmount(),
                    "items", items.size());
            return saved;
        } catch (SQLException | IllegalStateException e) {
            db.rollback();
            if (e instanceof IllegalStateException)
                throw new SQLException(e.getMessage(), e);
            throw (SQLException) e;
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

    /** Always fetches fresh — no cache — feeds the payment form dropdown. */
    public List<Order> getUnpaid() throws SQLException {
        return dao.findUnpaid();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void updateStatus(long orderId, String newStatus) throws SQLException {
        DatabaseConfig db = DatabaseConfig.getInstance();
        db.beginTransaction();
        try {
            Order order = dao.findById(orderId);
            String prev = order.getStatus();

            dao.updateStatus(orderId, newStatus);

            boolean toCompleted = newStatus.equalsIgnoreCase("completed");
            boolean toCancelled = newStatus.equalsIgnoreCase("cancelled");
            boolean wasCompleted = prev.equalsIgnoreCase("completed");
            boolean wasCancelled = prev.equalsIgnoreCase("cancelled");

            if (toCompleted && !wasCompleted) {
                for (OrderItem item : order.getItems())
                    inventoryService.fulfillReservation(item.getProductId(), item.getQuantity());
            } else if (toCancelled && !wasCancelled) {
                for (OrderItem item : order.getItems())
                    inventoryService.releaseReservation(item.getProductId(), item.getQuantity());
            }

            db.commit();
            cache.clear();
            inventoryService.clearCache();
            ActivityLogService.get().log(null, "order_status_changed",
                    "order_id", orderId, "from", prev, "to", newStatus);
        } catch (SQLException e) {
            db.rollback();
            throw e;
        }
    }

    public void updatePaymentStatus(long orderId, String paymentStatus) throws SQLException {
        dao.updatePaymentStatus(orderId, paymentStatus);
        cache.clear();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(long orderId) throws SQLException {
        dao.delete(orderId);
        cache.clear();
        ActivityLogService.get().log(null, "order_deleted", "order_id", orderId);
    }

    public void clearCache()   { cache.clear(); }

    private String normalize(String s) { return s == null ? "" : s.trim().toLowerCase(); }
}
