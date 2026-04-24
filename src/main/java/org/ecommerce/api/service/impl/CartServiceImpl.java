package org.ecommerce.api.service.impl;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.CartItemRequest;
import org.ecommerce.api.dto.request.CartRequest;
import org.ecommerce.api.entity.CartEntity;
import org.ecommerce.api.entity.CartItemEntity;
import org.ecommerce.api.entity.ProductEntity;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.repository.CartItemRepository;
import org.ecommerce.api.repository.CartRepository;
import org.ecommerce.api.repository.ProductRepository;
import org.ecommerce.api.repository.UserRepository;
import org.ecommerce.api.service.CartService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {

    private final CartRepository     cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository     userRepository;
    private final ProductRepository  productRepository;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository) {
        this.cartRepository     = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository     = userRepository;
        this.productRepository  = productRepository;
    }

    @Override
    public PagedResponse<CartEntity> findAll(Pageable pageable) {
        return PagedResponse.of(cartRepository.findAll(pageable));
    }

    @Override
    public CartEntity findById(long id) {
        return cartRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cart not found with id: " + id));
    }

    @Override
    public CartEntity findActiveByUserId(long userId) {
        return cartRepository.findByUser_UserIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No active cart found for user: " + userId));
    }

    @Override
    @Transactional
    public CartEntity create(CartRequest request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + request.getUserId()));

        cartRepository.findByUser_UserIdAndActiveTrue(request.getUserId()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User already has an active cart with id: " + existing.getCartId());
        });

        CartEntity cart = new CartEntity();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    @Override
    public List<CartItemEntity> getItems(long cartId) {
        findById(cartId);
        return cartItemRepository.findByCart_CartId(cartId);
    }

    @Override
    @Transactional
    public CartItemEntity addItem(long cartId, CartItemRequest request) {
        CartEntity cart = findById(cartId);

        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found with id: " + request.getProductId()));

        // if the product is already in the cart, just increase quantity
        return cartItemRepository
                .findByCart_CartIdAndProduct_ProductId(cartId, request.getProductId())
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + request.getQuantity());
                    return cartItemRepository.save(existing);
                })
                .orElseGet(() -> {
                    CartItemEntity item = new CartItemEntity();
                    item.setCart(cart);
                    item.setProduct(product);
                    item.setQuantity(request.getQuantity());
                    item.setUnitPrice(product.getEffectivePrice());
                    return cartItemRepository.save(item);
                });
    }

    @Override
    @Transactional
    public CartItemEntity updateItem(long cartId, long itemId, CartItemRequest request) {
        findById(cartId);
        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cart item not found with id: " + itemId));

        if (!item.getCart().getCartId().equals(cartId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Item " + itemId + " does not belong to cart " + cartId);
        }

        item.setQuantity(request.getQuantity());
        return cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void removeItem(long cartId, long itemId) {
        findById(cartId);
        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cart item not found with id: " + itemId));

        if (!item.getCart().getCartId().equals(cartId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Item " + itemId + " does not belong to cart " + cartId);
        }

        cartItemRepository.delete(item);
    }
}
