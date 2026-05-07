package org.ecommerce.api.graphql.input;

import org.ecommerce.api.dto.request.PaymentRequest;

import java.math.BigDecimal;

/** Bound from the GraphQL {@code PaymentInput} mutation input type via {@code @Argument}. */
public class PaymentInput {

    private Long   orderId;
    private String paymentMethod;
    private String transactionId;
    private double amount;

    public PaymentRequest toRequest() {
        PaymentRequest req = new PaymentRequest();
        req.setOrderId(orderId);
        req.setPaymentMethod(paymentMethod);
        req.setTransactionId(transactionId);
        req.setAmount(BigDecimal.valueOf(amount));
        return req;
    }

    public Long   getOrderId()                { return orderId; }
    public void   setOrderId(Long v)          { this.orderId = v; }

    public String getPaymentMethod()          { return paymentMethod; }
    public void   setPaymentMethod(String v)  { this.paymentMethod = v; }

    public String getTransactionId()          { return transactionId; }
    public void   setTransactionId(String v)  { this.transactionId = v; }

    public double getAmount()                 { return amount; }
    public void   setAmount(double v)         { this.amount = v; }
}
