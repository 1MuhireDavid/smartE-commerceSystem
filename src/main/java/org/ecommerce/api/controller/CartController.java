package org.ecommerce.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.ecommerce.api.dto.ApiResponse;
import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.CartItemRequest;
import org.ecommerce.api.dto.request.CartRequest;
import org.ecommerce.api.entity.CartEntity;
import org.ecommerce.api.entity.CartItemEntity;
import org.ecommerce.api.service.CartService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Carts", description = "Shopping cart management")
@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "List all carts")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CartEntity>>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        int clampedSize = Math.min(Math.max(size, 1), 100);
        PagedResponse<CartEntity> data = cartService.findAll(PageRequest.of(page, clampedSize));
        return ResponseEntity.ok(ApiResponse.success("Carts retrieved successfully", data));
    }

    @Operation(summary = "Get cart by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CartEntity>> getById(
            @Parameter(description = "Cart ID", example = "1") @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(cartService.findById(id)));
    }

    @Operation(summary = "Get active cart for a user")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<CartEntity>> getActiveByUser(
            @Parameter(description = "User ID", example = "1") @PathVariable long userId) {
        return ResponseEntity.ok(ApiResponse.success(cartService.findActiveByUserId(userId)));
    }

    @Operation(summary = "Create a new cart for a user")
    @PostMapping
    public ResponseEntity<ApiResponse<CartEntity>> create(@Valid @RequestBody CartRequest request) {
        CartEntity created = cartService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cart created successfully", created));
    }

    @Operation(summary = "List items in a cart")
    @GetMapping("/{id}/items")
    public ResponseEntity<ApiResponse<List<CartItemEntity>>> getItems(
            @Parameter(description = "Cart ID", example = "1") @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(cartService.getItems(id)));
    }

    @Operation(summary = "Add a product to a cart",
               description = "If the product is already in the cart, its quantity is increased instead of adding a duplicate.")
    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse<CartItemEntity>> addItem(
            @Parameter(description = "Cart ID", example = "1") @PathVariable long id,
            @Valid @RequestBody CartItemRequest request) {
        CartItemEntity item = cartService.addItem(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Item added to cart", item));
    }

    @Operation(summary = "Update the quantity of a cart item")
    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<ApiResponse<CartItemEntity>> updateItem(
            @Parameter(description = "Cart ID", example = "1")     @PathVariable long id,
            @Parameter(description = "Cart item ID", example = "1") @PathVariable long itemId,
            @Valid @RequestBody CartItemRequest request) {
        CartItemEntity updated = cartService.updateItem(id, itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", updated));
    }

    @Operation(summary = "Remove an item from a cart")
    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @Parameter(description = "Cart ID", example = "1")     @PathVariable long id,
            @Parameter(description = "Cart item ID", example = "1") @PathVariable long itemId) {
        cartService.removeItem(id, itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", null));
    }
}
