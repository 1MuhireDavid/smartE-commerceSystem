package org.ecommerce.api.repository;

import org.ecommerce.api.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    boolean existsByProduct_ProductIdAndUser_UserId(Long productId, Long userId);

    @Query("""
        SELECT r FROM ReviewEntity r
        WHERE (:productId IS NULL OR r.productId  = :productId)
          AND (:approved  IS NULL OR r.approved   = :approved)
        """)
    Page<ReviewEntity> search(
            @Param("productId") Long productId,
            @Param("approved")  Boolean approved,
            Pageable pageable);
}
