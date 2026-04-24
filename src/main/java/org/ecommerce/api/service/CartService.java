package org.ecommerce.api.service;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.CartItemRequest;
import org.ecommerce.api.dto.request.CartRequest;
import org.ecommerce.api.entity.CartEntity;
import org.ecommerce.api.entity.CartItemEntity;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CartService {

    PagedResponse<CartEntity> findAll(Pageable pageable);

    CartEntity findById(long id);

    CartEntity findActiveByUserId(long userId);

    CartEntity create(CartRequest request);

    List<CartItemEntity> getItems(long cartId);

    CartItemEntity addItem(long cartId, CartItemRequest request);

    CartItemEntity updateItem(long cartId, long itemId, CartItemRequest request);

    void removeItem(long cartId, long itemId);
}
