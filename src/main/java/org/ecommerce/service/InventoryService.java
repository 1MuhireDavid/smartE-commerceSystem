package org.ecommerce.service;

import org.ecommerce.cache.InMemoryCache;
import org.ecommerce.dao.InventoryDAO;
import org.ecommerce.model.Inventory;

import java.sql.SQLException;
import java.util.List;

public class InventoryService {

    private final InventoryDAO dao = new InventoryDAO();

    private static final InMemoryCache<String, List<Inventory>> cache = new InMemoryCache<>();
    private static final String ALL_KEY    = "inventory:all";
    private static final String LOW_KEY    = "inventory:low";
    private static final String SEARCH_PFX = "inventory:search:";

    // ── Read (cached) ─────────────────────────────────────────────────────────

    public List<Inventory> getAll() throws SQLException {
        List<Inventory> cached = cache.get(ALL_KEY);
        if (cached != null) return cached;
        List<Inventory> fresh = dao.findAll();
        cache.put(ALL_KEY, fresh);
        return fresh;
    }

    public List<Inventory> getLowStock() throws SQLException {
        List<Inventory> cached = cache.get(LOW_KEY);
        if (cached != null) return cached;
        List<Inventory> fresh = dao.findLowStock();
        cache.put(LOW_KEY, fresh);
        return fresh;
    }

    public List<Inventory> search(String kw) throws SQLException {
        String key = SEARCH_PFX + (kw == null ? "" : kw.trim().toLowerCase());
        List<Inventory> cached = cache.get(key);
        if (cached != null) return cached;
        List<Inventory> fresh = dao.search(kw);
        cache.put(key, fresh);
        return fresh;
    }


    public int countLowStock() throws SQLException {
        return dao.countLowStock();
    }

    // ── Update (invalidates cache) ────────────────────────────────────────────

    public void updateStock(long productId, int qty, int reorderLevel) throws SQLException {
        if (qty < 0)          throw new IllegalArgumentException("Quantity cannot be negative.");
        if (reorderLevel < 0) throw new IllegalArgumentException("Reorder level cannot be negative.");
        dao.updateStock(productId, qty, reorderLevel);
        cache.clear();
        ActivityLogService.get().log(null, "stock_updated",
                "product_id", productId, "qty", qty, "reorder_level", reorderLevel);
    }

    public void adjustStock(long productId, int delta) throws SQLException {
        dao.adjustStock(productId, delta);
        cache.clear();
        ActivityLogService.get().log(null, "stock_adjusted",
                "product_id", productId, "delta", delta);
    }

    public void reserveStock(long productId, int qty) throws SQLException {
        int updated = dao.reserveStock(productId, qty);
        if (updated == 0)
            throw new IllegalStateException("Insufficient available stock for product ID " + productId);
        cache.clear();
    }

    public void releaseReservation(long productId, int qty) throws SQLException {
        dao.releaseReservation(productId, qty);
        cache.clear();
        ActivityLogService.get().log(null, "stock_released",
                "product_id", productId, "qty", qty);
    }

    public void fulfillReservation(long productId, int qty) throws SQLException {
        dao.fulfillReservation(productId, qty);
        cache.clear();
        ActivityLogService.get().log(null, "stock_fulfilled",
                "product_id", productId, "qty", qty);
    }

    public void clearCache()   { cache.clear(); }
}
