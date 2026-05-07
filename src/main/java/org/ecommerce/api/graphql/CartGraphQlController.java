package org.ecommerce.api.graphql;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.entity.CartEntity;
import org.ecommerce.api.entity.CartItemEntity;
import org.ecommerce.api.entity.ProductEntity;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.graphql.input.CartInput;
import org.ecommerce.api.graphql.input.CartItemInput;
import org.ecommerce.api.repository.ProductRepository;
import org.ecommerce.api.repository.UserRepository;
import org.ecommerce.api.service.CartService;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * GraphQL resolver for Cart queries, mutations, and nested field resolvers.
 *
 * <p>Because OSIV is disabled, lazy associations on detached entities cannot be
 * accessed directly. Nested field resolvers use scalar FK columns (e.g. userId,
 * productId) to load related entities explicitly within their own transactions.
 */
@Controller
@Transactional(readOnly = true)
public class CartGraphQlController {

    private final CartService       cartService;
    private final UserRepository    userRepository;
    private final ProductRepository productRepository;

    public CartGraphQlController(CartService cartService,
                                 UserRepository userRepository,
                                 ProductRepository productRepository) {
        this.cartService       = cartService;
        this.userRepository    = userRepository;
        this.productRepository = productRepository;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public CartEntity cart(@Argument Long id) {
        return cartService.findById(id);
    }

    @QueryMapping
    public PagedResponse<CartEntity> carts(@Argument int page, @Argument int size) {
        return cartService.findAll(PageRequest.of(page, size));
    }

    @QueryMapping
    public CartEntity activeCart(@Argument Long userId) {
        try {
            return cartService.findActiveByUserId(userId);
        } catch (ResponseStatusException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) return null;
            throw e;
        }
    }

    // ── Nested field resolvers ─────────────────────────────────────────────────

    @SchemaMapping(typeName = "Cart", field = "user")
    public UserEntity user(CartEntity cart) {
        Long uid = cart.getUserId();
        return uid != null ? userRepository.findById(uid).orElse(null) : null;
    }

    @SchemaMapping(typeName = "Cart", field = "items")
    public List<CartItemEntity> items(CartEntity cart) {
        return cartService.getItems(cart.getCartId());
    }

    /**
     * Uses findByIdWithAssociations so the returned ProductEntity has its own
     * nested associations (seller, category, inventory) already initialized.
     * This prevents LazyInitializationException when ProductGraphQlController
     * resolves those sub-fields.
     */
    @SchemaMapping(typeName = "CartItem", field = "product")
    public ProductEntity product(CartItemEntity item) {
        Long pid = item.getProductId();
        return pid != null
                ? productRepository.findByIdWithAssociations(pid).orElse(null)
                : null;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    @Transactional
    public CartEntity createCart(@Argument CartInput input) {
        return cartService.create(input.toRequest());
    }

    @MutationMapping
    @Transactional
    public CartItemEntity addCartItem(@Argument Long cartId, @Argument CartItemInput input) {
        return cartService.addItem(cartId, input.toRequest());
    }

    @MutationMapping
    @Transactional
    public CartItemEntity updateCartItem(
            @Argument Long cartId,
            @Argument Long itemId,
            @Argument CartItemInput input) {
        return cartService.updateItem(cartId, itemId, input.toRequest());
    }

    @MutationMapping
    @Transactional
    public boolean removeCartItem(@Argument Long cartId, @Argument Long itemId) {
        cartService.removeItem(cartId, itemId);
        return true;
    }
}
