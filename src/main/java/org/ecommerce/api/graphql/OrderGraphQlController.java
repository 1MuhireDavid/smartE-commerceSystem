package org.ecommerce.api.graphql;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.entity.OrderEntity;
import org.ecommerce.api.entity.OrderItemEntity;
import org.ecommerce.api.entity.ProductEntity;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.graphql.input.OrderFilter;
import org.ecommerce.api.graphql.input.OrderInput;
import org.ecommerce.api.repository.ProductRepository;
import org.ecommerce.api.repository.UserRepository;
import org.ecommerce.api.service.OrderService;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * GraphQL resolver for Order queries, mutations, and nested field resolvers.
 *
 * <p>Because OSIV is disabled, nested field resolvers load related entities
 * using scalar FK columns rather than traversing lazy associations.
 */
@Controller
@Transactional(readOnly = true)
public class OrderGraphQlController {

    private final OrderService      orderService;
    private final UserRepository    userRepository;
    private final ProductRepository productRepository;

    public OrderGraphQlController(OrderService orderService,
                                  UserRepository userRepository,
                                  ProductRepository productRepository) {
        this.orderService      = orderService;
        this.userRepository    = userRepository;
        this.productRepository = productRepository;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public OrderEntity order(@Argument Long id) {
        return orderService.findById(id);
    }

    @QueryMapping
    public PagedResponse<OrderEntity> orders(
            @Argument OrderFilter filter,
            @Argument int page,
            @Argument int size) {

        Long   userId = filter != null ? filter.getUserId() : null;
        String status = filter != null ? filter.getStatus() : null;

        return orderService.findAll(userId, status, PageRequest.of(page, size));
    }

    // ── Nested field resolvers ─────────────────────────────────────────────────

    @SchemaMapping(typeName = "Order", field = "user")
    public UserEntity user(OrderEntity order) {
        Long uid = order.getUserId();
        return uid != null ? userRepository.findById(uid).orElse(null) : null;
    }

    @SchemaMapping(typeName = "Order", field = "items")
    public List<OrderItemEntity> items(OrderEntity order) {
        return orderService.findItems(order.getOrderId());
    }

    /**
     * Uses findByIdWithAssociations so Product sub-fields (seller, category,
     * inventory) are already initialized when ProductGraphQlController resolves them.
     */
    @SchemaMapping(typeName = "OrderItem", field = "product")
    public ProductEntity product(OrderItemEntity item) {
        Long pid = item.getProductId();
        return pid != null
                ? productRepository.findByIdWithAssociations(pid).orElse(null)
                : null;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    @Transactional
    public OrderEntity createOrder(@Argument OrderInput input) {
        return orderService.create(input.toRequest());
    }

    @MutationMapping
    @Transactional
    public OrderEntity updateOrderStatus(@Argument Long id, @Argument String status) {
        return orderService.updateStatus(id, status);
    }
}
