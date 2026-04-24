package org.ecommerce.api.repository;

import org.ecommerce.api.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {

    List<CartItemEntity> findByCart_CartId(Long cartId);

    Optional<CartItemEntity> findByCart_CartIdAndProduct_ProductId(Long cartId, Long productId);
}
