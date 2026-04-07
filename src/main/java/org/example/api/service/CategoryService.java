package org.example.api.service;

import org.example.api.dto.PagedResponse;
import org.example.api.dto.request.CategoryRequest;
import org.example.api.entity.CategoryEntity;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    PagedResponse<CategoryEntity> findAll(String keyword, Boolean active, Pageable pageable);

    CategoryEntity findById(int id);

    CategoryEntity create(CategoryRequest request);

    CategoryEntity update(int id, CategoryRequest request);

    void delete(int id);
}
