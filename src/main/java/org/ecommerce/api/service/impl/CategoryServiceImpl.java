package org.ecommerce.api.service.impl;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.CategoryRequest;
import org.ecommerce.api.entity.CategoryEntity;
import org.ecommerce.api.repository.CategoryRepository;
import org.ecommerce.api.service.CategoryService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public PagedResponse<CategoryEntity> findAll(String keyword, Boolean active, Pageable pageable) {
        String pattern = keyword != null ? ("%" + keyword.toLowerCase() + "%") : null;
        Page<CategoryEntity> page = categoryRepository.search(pattern, active, pageable);
        return PagedResponse.of(page);
    }

    @Override
    @Cacheable(value = "categories", key = "#id")
    public CategoryEntity findById(int id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found with id: " + id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryEntity create(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Slug already in use: " + request.getSlug());
        }

        CategoryEntity category = new CategoryEntity();
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setActive(request.isActive());
        category.setDisplayOrder(request.getDisplayOrder());

        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", key = "#id")
    public CategoryEntity update(int id, CategoryRequest request) {
        CategoryEntity category = findById(id);

        if (!category.getSlug().equals(request.getSlug())
                && categoryRepository.existsBySlug(request.getSlug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Slug already in use: " + request.getSlug());
        }

        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setActive(request.isActive());
        category.setDisplayOrder(request.getDisplayOrder());

        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", key = "#id")
    public void delete(int id) {
        CategoryEntity category = findById(id);
        categoryRepository.delete(category);
    }
}
