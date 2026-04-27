package org.ecommerce.api.repository;

import org.ecommerce.api.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);

    @Query("""
        SELECT u FROM UserEntity u
        WHERE (:role IS NULL OR u.role = :role)
          AND (:active IS NULL OR u.active = :active)
          AND (:pattern IS NULL
               OR LOWER(u.fullName) LIKE :pattern
               OR LOWER(u.email)    LIKE :pattern
               OR LOWER(u.username) LIKE :pattern)
        """)
    Page<UserEntity> search(
            @Param("pattern") String pattern,
            @Param("role")    String role,
            @Param("active")  Boolean active,
            Pageable pageable);
}
