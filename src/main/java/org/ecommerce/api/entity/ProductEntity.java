package org.ecommerce.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private UserEntity seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 220)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "discount_price", precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private InventoryEntity inventory;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (status == null) status = "draft";
        if (avgRating == null) avgRating = BigDecimal.ZERO;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public ProductEntity() {}

    public Long getProductId()                          { return productId; }
    public void setProductId(Long id)                   { this.productId = id; }

    public UserEntity getSeller()                       { return seller; }
    public void setSeller(UserEntity seller)            { this.seller = seller; }

    public CategoryEntity getCategory()                 { return category; }
    public void setCategory(CategoryEntity category)    { this.category = category; }

    public String getName()                             { return name; }
    public void setName(String name)                    { this.name = name; }

    public String getSlug()                             { return slug; }
    public void setSlug(String slug)                    { this.slug = slug; }

    public String getDescription()                      { return description; }
    public void setDescription(String description)      { this.description = description; }

    public BigDecimal getBasePrice()                    { return basePrice; }
    public void setBasePrice(BigDecimal basePrice)      { this.basePrice = basePrice; }

    public BigDecimal getDiscountPrice()                { return discountPrice; }
    public void setDiscountPrice(BigDecimal price)      { this.discountPrice = price; }

    public String getStatus()                           { return status; }
    public void setStatus(String status)                { this.status = status; }

    public BigDecimal getAvgRating()                    { return avgRating; }
    public void setAvgRating(BigDecimal rating)         { this.avgRating = rating; }

    public int getReviewCount()                         { return reviewCount; }
    public void setReviewCount(int count)               { this.reviewCount = count; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(LocalDateTime t)           { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()                 { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)           { this.updatedAt = t; }

    public InventoryEntity getInventory()               { return inventory; }
    public void setInventory(InventoryEntity inventory) { this.inventory = inventory; }

    public BigDecimal getEffectivePrice() {
        return (discountPrice != null) ? discountPrice : basePrice;
    }
}
