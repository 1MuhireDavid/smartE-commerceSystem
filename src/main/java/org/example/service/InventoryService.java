package org.example.service;

import org.example.cache.InMemoryCache;
import org.example.dao.InventoryDAO;
import org.example.model.Inventory;

import java.sql.SQLException;
import java.util.List;

public class InventoryService {

    private final InventoryDAO dao = new InventoryDAO();

    private final InMemoryCache<String, List<Inventory>> cache = new InMemoryCache<>();
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

    public Inventory getByProductId(long id) throws SQLException {
        return dao.findByProductId(id);
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
    }

    public void adjustStock(long productId, int delta) throws SQLException {
        dao.adjustStock(productId, delta);
        cache.clear();
    }

    // ── Cache info ────────────────────────────────────────────────────────────

    public int  getCacheSize() { return cache.size(); }
    public void clearCache()   { cache.clear(); }
}
