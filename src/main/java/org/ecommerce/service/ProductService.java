package org.ecommerce.service;

import org.ecommerce.cache.InMemoryCache;
import org.ecommerce.dao.ProductDAO;
import org.ecommerce.model.Product;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

public class ProductService {

    private static final String ALL_KEY    = "products:all";
    private static final String SEARCH_PFX = "products:search:";

    private final ProductDAO       dao              = new ProductDAO();
    private final InventoryService inventoryService = new InventoryService();
    private final InMemoryCache<String, List<Product>> cache = new InMemoryCache<>();

    // ── Create ────────────────────────────────────────────────────────────────

    public Product add(Product product) throws SQLException {
        Product saved = dao.insert(product);
        cache.clear();
        return saved;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<Product> getAll() throws SQLException {
        List<Product> cached = cache.get(ALL_KEY);
        if (cached != null) return cached;
        List<Product> fresh = dao.findAllIncludingDraft();
        cache.put(ALL_KEY, fresh);
        return fresh;
    }

    public List<Product> search(String keyword, int categoryId) throws SQLException {
        String key = SEARCH_PFX + normalize(keyword) + ":" + categoryId;
        List<Product> cached = cache.get(key);
        if (cached != null) return cached;
        List<Product> results = dao.search(keyword, categoryId);
        cache.put(key, results);
        return results;
    }

    public Product getById(long id) throws SQLException { return dao.findById(id); }

    public int getTotalCount()                     throws SQLException { return dao.count(); }
    public List<Product> getPage(int sz, int off)  throws SQLException { return dao.findPage(sz, off); }

    // ── Sorting (in-memory) ───────────────────────────────────────────────────

    public List<Product> sortByName(List<Product> list, boolean asc) {
        Comparator<Product> cmp = Comparator.comparing(p -> p.getName().toLowerCase());
        if (!asc) cmp = cmp.reversed();
        return list.stream().sorted(cmp).toList();
    }

    public List<Product> sortByPrice(List<Product> list, boolean asc) {
        Comparator<Product> cmp = Comparator.comparing(Product::getEffectivePrice);
        if (!asc) cmp = cmp.reversed();
        return list.stream().sorted(cmp).toList();
    }

    public List<Product> sortByStock(List<Product> list, boolean asc) {
        Comparator<Product> cmp = Comparator.comparingInt(Product::getStockQuantity);
        if (!asc) cmp = cmp.reversed();
        return list.stream().sorted(cmp).toList();
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update(Product product) throws SQLException {
        dao.update(product);
        cache.clear();
        inventoryService.clearCache();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(long id) throws SQLException {
        dao.delete(id);
        cache.clear();
    }

    // ── Cache info ────────────────────────────────────────────────────────────

    public int  getCacheSize() { return cache.size(); }
    public void clearCache()   { cache.clear(); }

    private String normalize(String s) { return s == null ? "" : s.trim().toLowerCase(); }
}
