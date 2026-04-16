package org.example.api.service.impl;

import org.example.api.dto.PagedResponse;
import org.example.api.dto.request.CategoryRequest;
import org.example.api.entity.CategoryEntity;
import org.example.api.repository.CategoryRepository;
import org.example.api.service.CategoryService;
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
        Page<CategoryEntity> page = categoryRepository.search(keyword, active, pageable);
        return PagedResponse.of(page);
    }

    @Override
    public CategoryEntity findById(int id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found with id: " + id));
    }

    @Override
    @Transactional
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
    public void delete(int id) {
        CategoryEntity category = findById(id);
        categoryRepository.delete(category);
    }
}
