package org.ecommerce.api.repository;

import org.ecommerce.api.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findByOrder_OrderId(Long orderId);
}
