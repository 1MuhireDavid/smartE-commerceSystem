package org.ecommerce.service;

import org.ecommerce.cache.InMemoryCache;
import org.ecommerce.dao.PaymentDAO;
import org.ecommerce.model.Payment;

import java.sql.SQLException;
import java.util.List;

public class PaymentService {

    private final PaymentDAO dao = new PaymentDAO();

    private final InMemoryCache<String, List<Payment>> cache = new InMemoryCache<>();
    private static final String ALL_KEY    = "payments:all";
    private static final String SEARCH_PFX = "payments:search:";

    public static final List<String> METHODS  = List.of("card", "paypal", "mobile_money", "cash");
    public static final List<String> STATUSES = List.of("pending", "completed", "failed", "refunded");

    // ── Create ────────────────────────────────────────────────────────────────

    public Payment record(Payment payment) throws SQLException {
        Payment saved = dao.insert(payment);
        cache.clear();
        return saved;
    }

    // ── Read (cached) ─────────────────────────────────────────────────────────

    public List<Payment> getAll() throws SQLException {
        List<Payment> cached = cache.get(ALL_KEY);
        if (cached != null) return cached;
        List<Payment> fresh = dao.findAll();
        cache.put(ALL_KEY, fresh);
        return fresh;
    }

    public List<Payment> search(String keyword, String status) throws SQLException {
        String key = SEARCH_PFX + normalize(keyword) + ":" + normalize(status);
        List<Payment> cached = cache.get(key);
        if (cached != null) return cached;
        List<Payment> results = dao.search(keyword, status);
        cache.put(key, results);
        return results;
    }

    public List<Payment> getByOrderId(long orderId) throws SQLException {
        return dao.findByOrderId(orderId);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void updateStatus(long paymentId, String status) throws SQLException {
        dao.updateStatus(paymentId, status);
        cache.clear();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(long paymentId) throws SQLException {
        dao.delete(paymentId);
        cache.clear();
    }

    private String normalize(String s) { return s == null ? "" : s.trim().toLowerCase(); }
}
