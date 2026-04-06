package org.example.model;

import java.math.BigDecimal;

public class OrderItem {

    private long       orderItemId;
    private long       orderId;
    private long       productId;
    private String     productName;  // denormalised
    private int        quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;   // computed column: quantity * unit_price
    private String     itemStatus;   // pending|shipped|delivered|returned

    public OrderItem() {}

    public long       getOrderItemId()                    { return orderItemId; }
    public void       setOrderItemId(long id)             { this.orderItemId = id; }

    public long       getOrderId()                        { return orderId; }
    public void       setOrderId(long orderId)            { this.orderId = orderId; }

    public long       getProductId()                      { return productId; }
    public void       setProductId(long productId)        { this.productId = productId; }

    public String     getProductName()                    { return productName; }
    public void       setProductName(String s)            { this.productName = s; }

    public int        getQuantity()                       { return quantity; }
    public void       setQuantity(int quantity)           { this.quantity = quantity; }

    public BigDecimal getUnitPrice()                      { return unitPrice; }
    public void       setUnitPrice(BigDecimal unitPrice)  { this.unitPrice = unitPrice; }

    public BigDecimal getTotalPrice()                     { return totalPrice; }
    public void       setTotalPrice(BigDecimal t)         { this.totalPrice = t; }

    public String     getItemStatus()                     { return itemStatus; }
    public void       setItemStatus(String s)             { this.itemStatus = s; }
}
