package org.ecommerce.api.service;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.ReviewRequest;
import org.ecommerce.api.entity.ReviewEntity;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    PagedResponse<ReviewEntity> findAll(Long productId, Boolean approved, Pageable pageable);

    ReviewEntity findById(long id);

    ReviewEntity create(ReviewRequest request);

    ReviewEntity approve(long id);

    void delete(long id);
}
