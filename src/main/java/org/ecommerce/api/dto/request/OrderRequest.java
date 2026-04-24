package org.ecommerce.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Payload for placing a new order")
public class OrderRequest {

    @Schema(description = "ID of the user placing the order", example = "1")
    @NotNull(message = "User ID is required")
    private Long userId;

    @Schema(description = "Line items — must contain at least one product")
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @Schema(description = "Discount to subtract from subtotal (defaults to 0)", example = "10.00")
    @DecimalMin(value = "0.00", inclusive = true, message = "Discount amount must be non-negative")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    public Long                  getUserId()                          { return userId; }
    public void                  setUserId(Long v)                    { this.userId = v; }

    public List<OrderItemRequest> getItems()                          { return items; }
    public void                  setItems(List<OrderItemRequest> v)   { this.items = v; }

    public BigDecimal            getDiscountAmount()                  { return discountAmount; }
    public void                  setDiscountAmount(BigDecimal v)      { this.discountAmount = v; }
}
