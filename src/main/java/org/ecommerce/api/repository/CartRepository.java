package org.ecommerce.api.repository;

import org.ecommerce.api.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<CartEntity, Long> {

    Optional<CartEntity> findByUser_UserIdAndActiveTrue(Long userId);
}
