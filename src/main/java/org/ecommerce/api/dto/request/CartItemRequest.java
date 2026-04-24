package org.ecommerce.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for adding or updating an item in a cart")
public class CartItemRequest {

    @Schema(description = "Product to add", example = "5")
    @NotNull(message = "Product ID is required")
    private Long productId;

    @Schema(description = "Number of units", example = "2", defaultValue = "1")
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;

    public Long getProductId()              { return productId; }
    public void setProductId(Long v)        { this.productId = v; }

    public int  getQuantity()               { return quantity; }
    public void setQuantity(int v)          { this.quantity = v; }
}
