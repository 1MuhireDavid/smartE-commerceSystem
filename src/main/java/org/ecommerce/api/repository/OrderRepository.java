package org.ecommerce.api.repository;

import org.ecommerce.api.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    @Query("""
        SELECT o FROM OrderEntity o
        WHERE (:userId IS NULL OR o.userId   = :userId)
          AND (:status IS NULL OR o.status   = :status)
        """)
    Page<OrderEntity> search(
            @Param("userId") Long userId,
            @Param("status") String status,
            Pageable pageable);
}
