package org.ecommerce.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.ecommerce.api.validation.ValidEnum;

import java.math.BigDecimal;

@Schema(description = "Payload for recording a payment against an order")
public class PaymentRequest {

    @Schema(description = "Order being paid", example = "1")
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @Schema(description = "Payment method",
            allowableValues = {"card", "paypal", "mobile_money", "cash"}, example = "card")
    @NotBlank(message = "Payment method is required")
    @ValidEnum(allowed = {"card", "paypal", "mobile_money", "cash"})
    private String paymentMethod;

    @Schema(description = "External transaction reference (optional)", example = "TXN-ABC123")
    private String transactionId;

    @Schema(description = "Amount paid", example = "129.99")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than zero")
    private BigDecimal amount;

    public Long      getOrderId()                      { return orderId; }
    public void      setOrderId(Long v)                { this.orderId = v; }

    public String    getPaymentMethod()                { return paymentMethod; }
    public void      setPaymentMethod(String v)        { this.paymentMethod = v; }

    public String    getTransactionId()                { return transactionId; }
    public void      setTransactionId(String v)        { this.transactionId = v; }

    public BigDecimal getAmount()                      { return amount; }
    public void       setAmount(BigDecimal v)          { this.amount = v; }
}
