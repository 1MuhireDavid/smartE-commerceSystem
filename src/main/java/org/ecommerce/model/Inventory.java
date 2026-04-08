package org.ecommerce.model;

import java.time.LocalDateTime;

public class Inventory {

    private long   inventoryId;
    private long   productId;
    private String productName;   // denormalised
    private String categoryName;  // denormalised
    private int    qtyInStock;
    private int    reservedQty;
    private int    reorderLevel;
    private LocalDateTime lastUpdated;

    public Inventory() {}

    public void   setInventoryId(long id)               { this.inventoryId = id; }

    public long   getProductId()                        { return productId; }
    public void   setProductId(long productId)          { this.productId = productId; }

    public String getProductName()                      { return productName; }
    public void   setProductName(String s)              { this.productName = s; }

    public String getCategoryName()                     { return categoryName; }
    public void   setCategoryName(String s)             { this.categoryName = s; }

    public int    getQtyInStock()                       { return qtyInStock; }
    public void   setQtyInStock(int qty)                { this.qtyInStock = qty; }

    public int    getReservedQty()                      { return reservedQty; }
    public void   setReservedQty(int qty)               { this.reservedQty = qty; }

    /** Available stock = qty_in_stock − reserved_qty */
    public int    getAvailableQty()                     { return qtyInStock - reservedQty; }

    public int    getReorderLevel()                     { return reorderLevel; }
    public void   setReorderLevel(int level)            { this.reorderLevel = level; }

    public boolean isLowStock()                         { return qtyInStock <= reorderLevel; }

    public LocalDateTime getLastUpdated()               { return lastUpdated; }
    public void   setLastUpdated(LocalDateTime t)       { this.lastUpdated = t; }
}
