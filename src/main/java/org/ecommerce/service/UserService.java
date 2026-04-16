package org.ecommerce.service;

import org.ecommerce.cache.InMemoryCache;
import org.ecommerce.dao.UserDAO;
import org.ecommerce.model.User;

import java.sql.SQLException;
import java.util.List;

public class UserService {

    private final UserDAO dao = new UserDAO();
    private final InMemoryCache<String, List<User>> cache = new InMemoryCache<>();

    // ── Create ────────────────────────────────────────────────────────────────

    public User add(User user) throws SQLException, IllegalArgumentException {
        if (dao.existsByUsername(user.getUsername()))
            throw new IllegalArgumentException("Username \"" + user.getUsername() + "\" is already taken.");
        if (dao.existsByEmail(user.getEmail()))
            throw new IllegalArgumentException("Email \"" + user.getEmail() + "\" is already registered.");
        User saved = dao.insert(user);
        cache.clear();
        return saved;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<User> getAll() throws SQLException {
        List<User> cached = cache.get("users:all");
        if (cached != null) return cached;
        List<User> fresh = dao.findAll();
        cache.put("users:all", fresh);
        return fresh;
    }

    public List<User> search(String keyword, String role) throws SQLException {
        String key = "users:search:" + (keyword == null ? "" : keyword.toLowerCase())
                     + ":" + (role == null ? "" : role);
        List<User> cached = cache.get(key);
        if (cached != null) return cached;
        List<User> result = dao.search(keyword, role);
        cache.put(key, result);
        return result;
    }

    public List<User> getSellers() throws SQLException {
        return dao.findBySellers();
    }

    public User getById(long id) throws SQLException {
        return dao.findById(id);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update(User user) throws SQLException, IllegalArgumentException {
        if (dao.existsByUsernameExcluding(user.getUsername(), user.getUserId()))
            throw new IllegalArgumentException("Username \"" + user.getUsername() + "\" is already taken.");
        if (dao.existsByEmailExcluding(user.getEmail(), user.getUserId()))
            throw new IllegalArgumentException("Email \"" + user.getEmail() + "\" is already registered.");
        dao.update(user);
        cache.clear();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void delete(long id) throws SQLException {
        dao.delete(id);
        cache.clear();
    }
}
