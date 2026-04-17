package org.ecommerce.api.service;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.ProductRequest;
import org.ecommerce.api.entity.ProductEntity;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    PagedResponse<ProductEntity> findAll(
            String keyword, Integer categoryId, String status, Long sellerId, Pageable pageable);

    ProductEntity findById(long id);

    ProductEntity create(ProductRequest request);

    ProductEntity update(long id, ProductRequest request);

    void delete(long id);
}
