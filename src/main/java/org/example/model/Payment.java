package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {

    private long          paymentId;
    private long          orderId;
    private String        orderNumber;    // denormalised
    private String        paymentMethod;  // card|paypal|mobile_money|cash
    private String        transactionId;  // nullable
    private BigDecimal    amount;
    private String        status;         // pending|completed|failed|refunded
    private LocalDateTime paidAt;         // nullable

    public Payment() {}

    public long          getPaymentId()                        { return paymentId; }
    public void          setPaymentId(long paymentId)          { this.paymentId = paymentId; }

    public long          getOrderId()                          { return orderId; }
    public void          setOrderId(long orderId)              { this.orderId = orderId; }

    public String        getOrderNumber()                      { return orderNumber; }
    public void          setOrderNumber(String s)              { this.orderNumber = s; }

    public String        getPaymentMethod()                    { return paymentMethod; }
    public void          setPaymentMethod(String s)            { this.paymentMethod = s; }

    public String        getTransactionId()                    { return transactionId; }
    public void          setTransactionId(String s)            { this.transactionId = s; }

    public BigDecimal    getAmount()                           { return amount; }
    public void          setAmount(BigDecimal amount)          { this.amount = amount; }

    public String        getStatus()                           { return status; }
    public void          setStatus(String status)              { this.status = status; }

    public LocalDateTime getPaidAt()                           { return paidAt; }
    public void          setPaidAt(LocalDateTime t)            { this.paidAt = t; }
}
