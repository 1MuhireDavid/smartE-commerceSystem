package org.ecommerce.service;

import org.ecommerce.cache.InMemoryCache;
import org.ecommerce.dao.CategoryDAO;
import org.ecommerce.model.Category;

import java.sql.SQLException;
import java.util.List;

public class CategoryService {

    private final CategoryDAO dao   = new CategoryDAO();
    private final InMemoryCache<String, List<Category>> cache = new InMemoryCache<>();

    public Category add(Category category) throws SQLException, IllegalArgumentException {
        String slug = CategoryDAO.slugify(category.getName());
        if (dao.existsBySlug(slug))
            throw new IllegalArgumentException(
                "A category named \"" + category.getName() + "\" already exists.");
        category.setSlug(slug);
        Category saved = dao.insert(category);
        cache.clear();
        return saved;
    }

    public List<Category> getAll() throws SQLException {
        List<Category> cached = cache.get("categories:all");
        if (cached != null) return cached;
        List<Category> fresh = dao.findAll();
        cache.put("categories:all", fresh);
        return fresh;
    }

    public List<Category> getActive() throws SQLException {
        List<Category> cached = cache.get("categories:active");
        if (cached != null) return cached;
        List<Category> fresh = dao.findActive();
        cache.put("categories:active", fresh);
        return fresh;
    }

    public Category getById(int id) throws SQLException { return dao.findById(id); }

    public void update(Category category) throws SQLException, IllegalArgumentException {
        String slug = CategoryDAO.slugify(category.getName());
        if (dao.existsBySlugExcluding(slug, category.getId()))
            throw new IllegalArgumentException(
                "Another category named \"" + category.getName() + "\" already exists.");
        category.setSlug(slug);
        dao.update(category);
        cache.clear();
    }

    public void delete(int id) throws SQLException {
        dao.delete(id);
        cache.clear();
    }
}
