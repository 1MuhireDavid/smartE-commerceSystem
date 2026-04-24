package org.ecommerce.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CartItem {

    private long          cartItemId;
    private long          cartId;
    private long          productId;
    private String        productName;    // denormalised
    private int           quantity;
    private BigDecimal    unitPrice;
    private LocalDateTime addedAt;

    public CartItem() {}

    public long          getCartItemId()                       { return cartItemId; }
    public void          setCartItemId(long cartItemId)        { this.cartItemId = cartItemId; }

    public long          getCartId()                           { return cartId; }
    public void          setCartId(long cartId)                { this.cartId = cartId; }

    public long          getProductId()                        { return productId; }
    public void          setProductId(long productId)          { this.productId = productId; }

    public String        getProductName()                      { return productName; }
    public void          setProductName(String s)              { this.productName = s; }

    public int           getQuantity()                         { return quantity; }
    public void          setQuantity(int quantity)             { this.quantity = quantity; }

    public BigDecimal    getUnitPrice()                        { return unitPrice; }
    public void          setUnitPrice(BigDecimal unitPrice)    { this.unitPrice = unitPrice; }


    public void          setAddedAt(LocalDateTime t)           { this.addedAt = t; }

    public BigDecimal    getLineTotal() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
