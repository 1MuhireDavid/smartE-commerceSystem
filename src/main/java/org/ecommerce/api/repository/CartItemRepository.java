package org.ecommerce.api.repository;

import org.ecommerce.api.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {

    @Query("SELECT ci FROM CartItemEntity ci LEFT JOIN FETCH ci.product WHERE ci.cart.cartId = :cartId")
    List<CartItemEntity> findByCart_CartId(@Param("cartId") Long cartId);

    Optional<CartItemEntity> findByCart_CartIdAndProduct_ProductId(Long cartId, Long productId);
}
