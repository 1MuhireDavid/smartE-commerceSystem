package org.example.api.repository;

import org.example.api.entity.CategoryEntity;
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
          AND (:keyword IS NULL
               OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY c.displayOrder ASC, c.name ASC
        """)
    Page<CategoryEntity> search(
            @Param("keyword") String keyword,
            @Param("active")  Boolean active,
            Pageable pageable);
}
