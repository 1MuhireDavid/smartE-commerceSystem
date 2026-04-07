package org.example.api.repository;

import org.example.api.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsBySlug(String slug);

    /**
     * Single-product fetch with all associations eagerly loaded.
     * Used by GraphQL resolvers to avoid LazyInitializationException.
     */
    @Query("""
        SELECT p FROM ProductEntity p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.seller
        LEFT JOIN FETCH p.inventory
        WHERE p.productId = :id
        """)
    Optional<ProductEntity> findByIdWithAssociations(@Param("id") Long id);

    /**
     * Filtered + paginated product search.
     * Supports: keyword (name ILIKE), categoryId, status, sellerId.
     * Sorting is handled by the Pageable sort parameter (e.g. basePrice,asc).
     */
    @Query("""
        SELECT p FROM ProductEntity p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.seller
        LEFT JOIN FETCH p.inventory
        WHERE (:keyword    IS NULL
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
          AND (:status     IS NULL OR p.status = :status)
          AND (:sellerId   IS NULL OR p.seller.userId = :sellerId)
        """)
    Page<ProductEntity> search(
            @Param("keyword")    String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("status")     String status,
            @Param("sellerId")   Long sellerId,
            Pageable pageable);

    /**
     * Count query required when using JOIN FETCH with pagination.
     */
    @Query("""
        SELECT COUNT(p) FROM ProductEntity p
        WHERE (:keyword    IS NULL
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR p.category.categoryId = :categoryId)
          AND (:status     IS NULL OR p.status = :status)
          AND (:sellerId   IS NULL OR p.seller.userId = :sellerId)
        """)
    long countSearch(
            @Param("keyword")    String keyword,
            @Param("categoryId") Integer categoryId,
            @Param("status")     String status,
            @Param("sellerId")   Long sellerId);
}
