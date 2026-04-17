package org.ecommerce.api.service;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.CategoryRequest;
import org.ecommerce.api.entity.CategoryEntity;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    PagedResponse<CategoryEntity> findAll(String keyword, Boolean active, Pageable pageable);

    CategoryEntity findById(int id);

    CategoryEntity create(CategoryRequest request);

    CategoryEntity update(int id, CategoryRequest request);

    void delete(int id);
}
