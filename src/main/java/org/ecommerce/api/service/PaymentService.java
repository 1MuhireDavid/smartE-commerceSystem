package org.ecommerce.api.service;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.PaymentRequest;
import org.ecommerce.api.entity.PaymentEntity;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    PagedResponse<PaymentEntity> findAll(Long orderId, String status, Pageable pageable);

    PaymentEntity findById(long id);

    PaymentEntity create(PaymentRequest request);

    PaymentEntity updateStatus(long id, String status);
}
