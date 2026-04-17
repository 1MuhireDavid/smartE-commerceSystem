package org.ecommerce.api.repository;

import org.ecommerce.api.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("""
        SELECT u FROM UserEntity u
        WHERE (:role IS NULL OR u.role = :role)
          AND (:active IS NULL OR u.active = :active)
          AND (:keyword IS NULL
               OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<UserEntity> search(
            @Param("keyword") String keyword,
            @Param("role")    String role,
            @Param("active")  Boolean active,
            Pageable pageable);
}
