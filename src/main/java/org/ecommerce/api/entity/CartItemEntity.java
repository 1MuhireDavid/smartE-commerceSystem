package org.ecommerce.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items",
       uniqueConstraints = @UniqueConstraint(name = "uq_cart_product",
                                             columnNames = {"cart_id", "product_id"}))
public class CartItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long cartItemId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private CartEntity cart;

    @Column(name = "cart_id", insertable = false, updatable = false)
    private Long cartId;

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

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    void onCreate() {
        addedAt = LocalDateTime.now();
        if (quantity == 0) quantity = 1;
    }

    public CartItemEntity() {}

    public Long getCartItemId()                          { return cartItemId; }
    public void setCartItemId(Long cartItemId)           { this.cartItemId = cartItemId; }

    public CartEntity getCart()                          { return cart; }
    public void setCart(CartEntity cart)                 { this.cart = cart; }

    public Long getCartId()                              { return cartId; }

    public ProductEntity getProduct()                    { return product; }
    public void setProduct(ProductEntity product)        { this.product = product; }

    public Long getProductId()                           { return productId; }

    public int getQuantity()                             { return quantity; }
    public void setQuantity(int quantity)                { this.quantity = quantity; }

    public BigDecimal getUnitPrice()                     { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice)       { this.unitPrice = unitPrice; }

    public LocalDateTime getAddedAt()                    { return addedAt; }
    public void setAddedAt(LocalDateTime t)              { this.addedAt = t; }
}
