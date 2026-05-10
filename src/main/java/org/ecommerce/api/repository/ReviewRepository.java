package org.ecommerce.api.repository;

import org.ecommerce.api.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    boolean existsByProduct_ProductIdAndUser_UserId(Long productId, Long userId);

    // countQuery required: Spring Data cannot derive COUNT from a FETCH-join query.
    // ManyToOne FETCHes are safe with Pageable — HHH90003004 only triggers on collection fetches.
    @Query(value = """
        SELECT r FROM ReviewEntity r
        LEFT JOIN FETCH r.product
        LEFT JOIN FETCH r.user
        LEFT JOIN FETCH r.order
        WHERE (:productId IS NULL OR r.productId = :productId)
          AND (:approved  IS NULL OR r.approved  = :approved)
        """,
        countQuery = """
        SELECT COUNT(r) FROM ReviewEntity r
        WHERE (:productId IS NULL OR r.productId = :productId)
          AND (:approved  IS NULL OR r.approved  = :approved)
        """)
    Page<ReviewEntity> search(
            @Param("productId") Long productId,
            @Param("approved")  Boolean approved,
            Pageable pageable);
}
