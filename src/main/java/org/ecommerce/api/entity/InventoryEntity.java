package org.ecommerce.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
public class InventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long inventoryId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private ProductEntity product;

    @Column(name = "qty_in_stock", nullable = false)
    private int qtyInStock;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    void touch() { lastUpdated = LocalDateTime.now(); }

    public InventoryEntity() {}

    public Long getInventoryId()                       { return inventoryId; }
    public void setInventoryId(Long id)                { this.inventoryId = id; }

    public ProductEntity getProduct()                  { return product; }
    public void setProduct(ProductEntity product)      { this.product = product; }

    public int getQtyInStock()                         { return qtyInStock; }
    public void setQtyInStock(int qty)                 { this.qtyInStock = qty; }

    public int getReservedQty()                        { return reservedQty; }
    public void setReservedQty(int qty)                { this.reservedQty = qty; }

    public int getReorderLevel()                       { return reorderLevel; }
    public void setReorderLevel(int level)             { this.reorderLevel = level; }

    public LocalDateTime getLastUpdated()              { return lastUpdated; }
    public void setLastUpdated(LocalDateTime t)        { this.lastUpdated = t; }
}
