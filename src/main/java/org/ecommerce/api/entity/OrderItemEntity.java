package org.ecommerce.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "order_id", insertable = false, updatable = false)
    private Long orderId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "product_id", insertable = false, updatable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", insertable = false, updatable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "item_status", nullable = false, length = 10)
    private String itemStatus;

    @PrePersist
    void onCreate() {
        if (itemStatus == null) itemStatus = "pending";
    }

    public OrderItemEntity() {}

    public Long getOrderItemId()                         { return orderItemId; }
    public void setOrderItemId(Long orderItemId)         { this.orderItemId = orderItemId; }

    public OrderEntity getOrder()                        { return order; }
    public void setOrder(OrderEntity order)              { this.order = order; }

    public Long getOrderId()                             { return orderId; }

    public ProductEntity getProduct()                    { return product; }
    public void setProduct(ProductEntity product)        { this.product = product; }

    public Long getProductId()                           { return productId; }

    public int getQuantity()                             { return quantity; }
    public void setQuantity(int quantity)                { this.quantity = quantity; }

    public BigDecimal getUnitPrice()                     { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice)       { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice()                    { return totalPrice; }

    public String getItemStatus()                        { return itemStatus; }
    public void setItemStatus(String itemStatus)         { this.itemStatus = itemStatus; }
}
