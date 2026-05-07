package org.ecommerce.api.service.impl;

import org.ecommerce.api.config.AsyncConfig;
import org.ecommerce.api.dto.OrderStatsDto;
import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.OrderItemRequest;
import org.ecommerce.api.dto.request.OrderRequest;
import org.ecommerce.api.entity.OrderEntity;
import org.ecommerce.api.entity.OrderItemEntity;
import org.ecommerce.api.entity.ProductEntity;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.repository.InventoryRepository;
import org.ecommerce.api.repository.OrderItemRepository;
import org.ecommerce.api.repository.OrderRepository;
import org.ecommerce.api.repository.ProductRepository;
import org.ecommerce.api.repository.UserRepository;
import org.ecommerce.api.service.OrderService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private static final Set<String> VALID_STATUSES =
            Set.of("pending", "processing", "completed", "cancelled");

    private final OrderRepository     orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository      userRepository;
    private final ProductRepository   productRepository;
    private final InventoryRepository inventoryRepository;
    private final Executor            taskExecutor;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemRepository orderItemRepository,
                            UserRepository userRepository,
                            ProductRepository productRepository,
                            InventoryRepository inventoryRepository,
                            @Qualifier(AsyncConfig.EXECUTOR_BEAN) Executor taskExecutor) {
        this.orderRepository     = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository      = userRepository;
        this.productRepository   = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.taskExecutor        = taskExecutor;
    }

    @Override
    public PagedResponse<OrderEntity> findAll(Long userId, String status, Pageable pageable) {
        return PagedResponse.of(orderRepository.search(userId, status, pageable));
    }

    @Override
    public OrderEntity findById(long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found with id: " + id));
    }

    @Override
    public List<OrderItemEntity> findItems(long orderId) {
        findById(orderId);
        return orderItemRepository.findByOrder_OrderId(orderId);
    }

    // Propagation.REQUIRED: joins the caller's transaction if one exists; starts a new one
    // otherwise. Isolation.READ_COMMITTED: each read sees only committed data from other
    // transactions, preventing dirty reads during stock checks and order writes.
    // rollbackFor = Exception.class: guarantees rollback on any exception — including checked
    // ones — so no partial order or inventory state is ever persisted on failure.
    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation   = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class)
    public OrderEntity create(OrderRequest request) {
        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + request.getUserId()));

        BigDecimal discount = request.getDiscountAmount() != null
                ? request.getDiscountAmount()
                : BigDecimal.ZERO;

        // Batch-load all products in one IN query instead of N individual SELECTs
        List<Long> productIds = request.getItems().stream()
                .map(OrderItemRequest::getProductId)
                .toList();
        Map<Long, ProductEntity> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductEntity::getProductId, p -> p));

        List<OrderItemEntity> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest line : request.getItems()) {
            ProductEntity product = Optional.ofNullable(productMap.get(line.getProductId()))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Product not found with id: " + line.getProductId()));

            // Atomic check-and-decrement: returns 0 when qty_in_stock < requested quantity.
            // Throwing here rolls back the entire transaction — no partial order or inventory
            // deduction survives.
            int updated = inventoryRepository.deductStock(line.getProductId(), line.getQuantity());
            if (updated == 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Insufficient stock for product id: " + line.getProductId());
            }

            BigDecimal unitPrice = product.getEffectivePrice();
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(line.getQuantity())));

            OrderItemEntity item = new OrderItemEntity();
            item.setProduct(product);
            item.setQuantity(line.getQuantity());
            item.setUnitPrice(unitPrice);
            lineItems.add(item);
        }

        BigDecimal totalAmount = subtotal.subtract(discount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Discount amount exceeds the order subtotal");
        }

        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setOrderNumber("ORD-" + Instant.now().toEpochMilli());
        order.setSubtotal(subtotal);
        order.setDiscountAmount(discount);
        order.setTotalAmount(totalAmount);
        orderRepository.save(order);

        // Set FK on all items then batch-insert with a single saveAll flush
        lineItems.forEach(item -> item.setOrder(order));
        orderItemRepository.saveAll(lineItems);

        return order;
    }

    @Override
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation   = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class)
    public OrderEntity updateStatus(long id, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status '" + status + "'. Allowed: " + VALID_STATUSES);
        }
        OrderEntity order = findById(id);
        order.setStatus(status);
        return orderRepository.save(order);
    }

    // The two aggregate queries are independent — run them on the task executor in parallel
    // to halve round-trip latency. Each query runs in its own Spring Data transaction on the
    // executor thread. REPEATABLE_READ across both queries is dropped as an acceptable
    // trade-off: stats are approximate by nature and don't require a single consistent snapshot.
    @Override
    public OrderStatsDto getStats() {
        CompletableFuture<List<Object[]>> statsFuture = CompletableFuture.supplyAsync(
                orderRepository::getStatsByStatus, taskExecutor);
        CompletableFuture<BigDecimal> revenueFuture = CompletableFuture.supplyAsync(
                orderRepository::sumPaidRevenue, taskExecutor);

        List<Object[]>  rows        = statsFuture.join();
        BigDecimal      paidRevenue = revenueFuture.join();

        List<OrderStatsDto.StatusCount> byStatus = rows.stream()
                .map(r -> new OrderStatsDto.StatusCount(
                        (String) r[0],
                        ((Number) r[1]).longValue(),
                        r[2] != null ? (BigDecimal) r[2] : BigDecimal.ZERO))
                .toList();

        return new OrderStatsDto(byStatus, paidRevenue);
    }
}
