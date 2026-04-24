package org.ecommerce.api.repository;

import org.ecommerce.api.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Integer> {

    boolean existsBySlug(String slug);

    @Query("""
        SELECT c FROM CategoryEntity c
        WHERE (:active IS NULL OR c.active = :active)
          AND (:pattern IS NULL
               OR LOWER(c.name) LIKE :pattern)
        ORDER BY c.displayOrder ASC, c.name ASC
        """)
    Page<CategoryEntity> search(
            @Param("pattern") String pattern,
            @Param("active")  Boolean active,
            Pageable pageable);
}
