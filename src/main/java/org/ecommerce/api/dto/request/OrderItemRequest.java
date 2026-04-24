package org.ecommerce.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "One line item within an order")
public class OrderItemRequest {

    @Schema(description = "Product ID", example = "5")
    @NotNull(message = "Product ID is required")
    private Long productId;

    @Schema(description = "Number of units to order", example = "2")
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    public Long getProductId()          { return productId; }
    public void setProductId(Long v)    { this.productId = v; }

    public int  getQuantity()           { return quantity; }
    public void setQuantity(int v)      { this.quantity = v; }
}
