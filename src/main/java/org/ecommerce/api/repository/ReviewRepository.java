package org.ecommerce.api.repository;

import org.ecommerce.api.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    boolean existsByProduct_ProductIdAndUser_UserId(Long productId, Long userId);

    // JOIN FETCH eliminates N+1: product + user loaded in the same SELECT instead of
    // N individual lazy-load queries per page.  ManyToOne fetches are safe with Pageable
    // (no HHH90003004 in-memory pagination warning — that only affects collection fetches).
    // countQuery is required because Spring Data cannot derive COUNT from a FETCH-join query.
    @Query(value = """
        SELECT r FROM ReviewEntity r
        LEFT JOIN FETCH r.product
        LEFT JOIN FETCH r.user
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
