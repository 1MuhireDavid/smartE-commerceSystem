package org.ecommerce.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.ecommerce.api.dto.ApiResponse;
import org.ecommerce.api.dto.OrderStatsDto;
import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.OrderRequest;
import org.ecommerce.api.entity.OrderEntity;
import org.ecommerce.api.entity.OrderItemEntity;
import org.ecommerce.api.service.OrderService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Orders", description = "Order placement and status management")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "List orders", description = "Filterable by user and status, sorted newest-first.")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrderEntity>>> list(
            @Parameter(description = "Filter by user ID", example = "1")
            @RequestParam(required = false) Long userId,

            @Parameter(description = "Filter by order status",
                       schema = @Schema(allowableValues = {"pending", "processing", "completed", "cancelled"}))
            @RequestParam(required = false) String status,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        int clampedSize = Math.min(Math.max(size, 1), 100);
        PagedResponse<OrderEntity> data = orderService.findAll(
                userId, status,
                PageRequest.of(page, clampedSize, Sort.by("orderedAt").descending()));

        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", data));
    }

    @Operation(summary = "Get order by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderEntity>> getById(
            @Parameter(description = "Order ID", example = "1") @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.findById(id)));
    }

    @Operation(summary = "Get line items for an order")
    @GetMapping("/{id}/items")
    public ResponseEntity<ApiResponse<List<OrderItemEntity>>> getItems(
            @Parameter(description = "Order ID", example = "1") @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.findItems(id)));
    }

    @Operation(summary = "Place a new order",
               description = "Calculates subtotal from current product prices and validates the discount amount.")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderEntity>> create(@Valid @RequestBody OrderRequest request) {
        OrderEntity created = orderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", created));
    }

    @Operation(
            summary     = "Order statistics",
            description = "Aggregate order counts and revenue grouped by status, plus total confirmed (paid) revenue.")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<OrderStatsDto>> getStats() {
        return ResponseEntity.ok(
                ApiResponse.success("Order statistics retrieved", orderService.getStats()));
    }

    @Operation(summary = "Update order status",
               description = "Allowed values: pending → processing → completed | cancelled")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderEntity>> updateStatus(
            @Parameter(description = "Order ID", example = "1") @PathVariable long id,
            @Parameter(description = "New status",
                       schema = @Schema(allowableValues = {"pending", "processing", "completed", "cancelled"}))
            @RequestParam String status) {
        OrderEntity updated = orderService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", updated));
    }
}
