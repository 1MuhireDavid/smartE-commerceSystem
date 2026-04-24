package org.ecommerce.api.repository;

import org.ecommerce.api.entity.PaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    @Query("""
        SELECT p FROM PaymentEntity p
        WHERE (:orderId IS NULL OR p.orderId  = :orderId)
          AND (:status  IS NULL OR p.status   = :status)
        """)
    Page<PaymentEntity> search(
            @Param("orderId") Long orderId,
            @Param("status")  String status,
            Pageable pageable);
}
