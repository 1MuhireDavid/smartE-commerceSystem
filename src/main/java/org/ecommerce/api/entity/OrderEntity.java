package org.ecommerce.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @Column(name = "status", nullable = false, length = 15)
    private String status;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_status", nullable = false, length = 10)
    private String paymentStatus;

    @Column(name = "ordered_at", nullable = false, updatable = false)
    private LocalDateTime orderedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        orderedAt = updatedAt = LocalDateTime.now();
        if (status == null)        status = "pending";
        if (paymentStatus == null) paymentStatus = "unpaid";
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public OrderEntity() {}

    public Long getOrderId()                             { return orderId; }
    public void setOrderId(Long orderId)                 { this.orderId = orderId; }

    public UserEntity getUser()                          { return user; }
    public void setUser(UserEntity user)                 { this.user = user; }

    public Long getUserId()                              { return userId; }

    public String getOrderNumber()                       { return orderNumber; }
    public void setOrderNumber(String orderNumber)       { this.orderNumber = orderNumber; }

    public String getStatus()                            { return status; }
    public void setStatus(String status)                 { this.status = status; }

    public BigDecimal getSubtotal()                      { return subtotal; }
    public void setSubtotal(BigDecimal subtotal)         { this.subtotal = subtotal; }

    public BigDecimal getDiscountAmount()                { return discountAmount; }
    public void setDiscountAmount(BigDecimal amount)     { this.discountAmount = amount; }

    public BigDecimal getTotalAmount()                   { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount)   { this.totalAmount = totalAmount; }

    public String getPaymentStatus()                     { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus)   { this.paymentStatus = paymentStatus; }

    public LocalDateTime getOrderedAt()                  { return orderedAt; }
    public void setOrderedAt(LocalDateTime t)            { this.orderedAt = t; }

    public LocalDateTime getUpdatedAt()                  { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)            { this.updatedAt = t; }
}
