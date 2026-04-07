package org.example.api.service;

import org.example.api.dto.PagedResponse;
import org.example.api.dto.request.ProductRequest;
import org.example.api.entity.ProductEntity;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    PagedResponse<ProductEntity> findAll(
            String keyword, Integer categoryId, String status, Long sellerId, Pageable pageable);

    ProductEntity findById(long id);

    ProductEntity create(ProductRequest request);

    ProductEntity update(long id, ProductRequest request);

    void delete(long id);
}
