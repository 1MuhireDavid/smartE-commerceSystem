package org.ecommerce.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Order {

    private long       orderId;
    private long       userId;
    private String     userFullName;   // denormalised
    private Long       shippingAddrId;
    private String     orderNumber;
    private String     status;         // pending|processing|shipped|delivered|cancelled
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String     paymentStatus;  // unpaid|paid|refunded
    private LocalDateTime orderedAt;
    private LocalDateTime updatedAt;

    private List<OrderItem> items;     // populated on demand

    public Order() {}

    // Getters / Setters ────────────────────────────────────────────────────────

    public long       getOrderId()                       { return orderId; }
    public void       setOrderId(long orderId)           { this.orderId = orderId; }

    public long       getUserId()                        { return userId; }
    public void       setUserId(long userId)             { this.userId = userId; }

    public String     getUserFullName()                  { return userFullName; }
    public void       setUserFullName(String s)          { this.userFullName = s; }

    public Long       getShippingAddrId()                { return shippingAddrId; }
    public void       setShippingAddrId(Long id)         { this.shippingAddrId = id; }

    public String     getOrderNumber()                   { return orderNumber; }
    public void       setOrderNumber(String s)           { this.orderNumber = s; }

    public String     getStatus()                        { return status; }
    public void       setStatus(String status)           { this.status = status; }

    public BigDecimal getSubtotal()                      { return subtotal; }
    public void       setSubtotal(BigDecimal subtotal)   { this.subtotal = subtotal; }

    public BigDecimal getDiscountAmount()                { return discountAmount; }
    public void       setDiscountAmount(BigDecimal d)    { this.discountAmount = d; }

    public BigDecimal getTotalAmount()                   { return totalAmount; }
    public void       setTotalAmount(BigDecimal t)       { this.totalAmount = t; }

    public String     getPaymentStatus()                 { return paymentStatus; }
    public void       setPaymentStatus(String s)         { this.paymentStatus = s; }

    public LocalDateTime getOrderedAt()                  { return orderedAt; }
    public void       setOrderedAt(LocalDateTime t)      { this.orderedAt = t; }

    public LocalDateTime getUpdatedAt()                  { return updatedAt; }
    public void       setUpdatedAt(LocalDateTime t)      { this.updatedAt = t; }

    public List<OrderItem> getItems()                    { return items; }
    public void       setItems(List<OrderItem> items)    { this.items = items; }

    @Override public String toString() { return orderNumber; }
}
