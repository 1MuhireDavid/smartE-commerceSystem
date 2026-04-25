package org.ecommerce.api.repository;

import org.ecommerce.api.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsBySlug(String slug);

    // Native SQL full-text search using the GIN index on products(name, description).
    // JPQL has no to_tsvector / plainto_tsquery / @@ syntax, so native is required.
    // The countQuery is mandatory — Spring Data cannot auto-derive a count from SELECT * native queries.
    @Query(value = """
            SELECT p.* FROM products p
             WHERE (:query      IS NULL
                    OR to_tsvector('english', p.name || ' ' || COALESCE(p.description, ''))
                       @@ plainto_tsquery('english', :query))
               AND (:categoryId IS NULL OR p.category_id = :categoryId)
               AND (:status     IS NULL OR p.status       = :status)
               AND (:sellerId   IS NULL OR p.seller_id    = :sellerId)
            """,
            countQuery = """
            SELECT COUNT(*) FROM products p
             WHERE (:query      IS NULL
                    OR to_tsvector('english', p.name || ' ' || COALESCE(p.description, ''))
                       @@ plainto_tsquery('english', :query))
               AND (:categoryId IS NULL OR p.category_id = :categoryId)
               AND (:status     IS NULL OR p.status       = :status)
               AND (:sellerId   IS NULL OR p.seller_id    = :sellerId)
            """,
            nativeQuery = true)
    Page<ProductEntity> searchFts(
            @Param("query")      String query,
            @Param("categoryId") Integer categoryId,
            @Param("status")     String status,
            @Param("sellerId")   Long sellerId,
            Pageable pageable);

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

}
