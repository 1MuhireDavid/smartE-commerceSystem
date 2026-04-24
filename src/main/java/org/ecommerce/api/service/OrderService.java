package org.ecommerce.api.service;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.OrderRequest;
import org.ecommerce.api.entity.OrderEntity;
import org.ecommerce.api.entity.OrderItemEntity;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    PagedResponse<OrderEntity> findAll(Long userId, String status, Pageable pageable);

    OrderEntity findById(long id);

    List<OrderItemEntity> findItems(long orderId);

    OrderEntity create(OrderRequest request);

    OrderEntity updateStatus(long id, String status);
}
