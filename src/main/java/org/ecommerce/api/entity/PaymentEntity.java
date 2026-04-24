package org.ecommerce.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "order_id", insertable = false, updatable = false)
    private Long orderId;

    @Column(name = "payment_method", nullable = false, length = 15)
    private String paymentMethod;

    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    void onCreate() {
        if (status == null) status = "pending";
    }

    public PaymentEntity() {}

    public Long getPaymentId()                           { return paymentId; }
    public void setPaymentId(Long paymentId)             { this.paymentId = paymentId; }

    public OrderEntity getOrder()                        { return order; }
    public void setOrder(OrderEntity order)              { this.order = order; }

    public Long getOrderId()                             { return orderId; }

    public String getPaymentMethod()                     { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod)   { this.paymentMethod = paymentMethod; }

    public String getTransactionId()                     { return transactionId; }
    public void setTransactionId(String transactionId)   { this.transactionId = transactionId; }

    public BigDecimal getAmount()                        { return amount; }
    public void setAmount(BigDecimal amount)             { this.amount = amount; }

    public String getStatus()                            { return status; }
    public void setStatus(String status)                 { this.status = status; }

    public LocalDateTime getPaidAt()                     { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt)          { this.paidAt = paidAt; }
}
