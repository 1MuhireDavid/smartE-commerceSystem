package org.ecommerce.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {

    private long       id;           // product_id
    private long       sellerId;
    private String     sellerName;   // denormalised for display
    private int        categoryId;
    private String     categoryName; // denormalised for display
    private String     name;
    private String     slug;
    private String     description;
    private BigDecimal basePrice;    // base_price column
    private BigDecimal discountPrice;
    private String     status;       // active | inactive | draft
    private BigDecimal avgRating;
    private int        reviewCount;
    private int        stockQuantity; // populated from inventory JOIN
    private int        reservedQty;
    private int        reorderLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Product() {}

    // Getters / Setters ────────────────────────────────────────────────────────

    public long       getId()                        { return id; }
    public void       setId(long id)                 { this.id = id; }

    public long       getSellerId()                  { return sellerId; }
    public void       setSellerId(long sellerId)     { this.sellerId = sellerId; }

    public void       setSellerName(String s)        { this.sellerName = s; }

    public int        getCategoryId()                { return categoryId; }
    public void       setCategoryId(int categoryId)  { this.categoryId = categoryId; }

    public String     getCategoryName()              { return categoryName; }
    public void       setCategoryName(String s)      { this.categoryName = s; }

    public String     getName()                      { return name; }
    public void       setName(String name)           { this.name = name; }

    public String     getSlug()                      { return slug; }
    public void       setSlug(String slug)           { this.slug = slug; }

    public String     getDescription()               { return description; }
    public void       setDescription(String d)       { this.description = d; }

    public BigDecimal getBasePrice()                 { return basePrice; }
    public void       setBasePrice(BigDecimal p)     { this.basePrice = p; }

    /** Convenience alias used by existing TableView column bindings. */
    public BigDecimal getPrice()                     { return basePrice; }

    public BigDecimal getDiscountPrice()             { return discountPrice; }
    public void       setDiscountPrice(BigDecimal p) { this.discountPrice = p; }

    /** The effective selling price (discount_price if set, otherwise base_price). */
    public BigDecimal getEffectivePrice() {
        return (discountPrice != null) ? discountPrice : basePrice;
    }

    public String     getStatus()                    { return status; }
    public void       setStatus(String status)       { this.status = status; }

    public void       setAvgRating(BigDecimal r)     { this.avgRating = r; }

    public void       setReviewCount(int n)          { this.reviewCount = n; }

    public int        getStockQuantity()             { return stockQuantity; }
    public void       setStockQuantity(int q)        { this.stockQuantity = q; }

    public void       setReservedQty(int q)          { this.reservedQty = q; }

    public void       setReorderLevel(int l)         { this.reorderLevel = l; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void       setCreatedAt(LocalDateTime t)  { this.createdAt = t; }

    public void       setUpdatedAt(LocalDateTime t)  { this.updatedAt = t; }

    @Override public String toString() { return name; }
}
