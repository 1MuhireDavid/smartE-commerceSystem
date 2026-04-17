package org.ecommerce.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.ecommerce.api.validation.ValidDiscount;
import org.ecommerce.api.validation.ValidEnum;
import org.ecommerce.api.validation.ValidSlug;

import java.math.BigDecimal;

@ValidDiscount
@Schema(description = "Payload for creating or updating a product")
public class ProductRequest {

    @Schema(description = "Product display name", example = "Wireless Headphones", maxLength = 200)
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @Schema(description = "URL-friendly slug (lowercase letters, digits, single hyphens)",
            example = "wireless-headphones", maxLength = 220)
    @NotBlank(message = "Slug is required")
    @Size(max = 220, message = "Slug must not exceed 220 characters")
    @ValidSlug
    private String slug;

    @Schema(description = "Full product description", example = "Premium over-ear headphones with ANC")
    private String description;

    @Schema(description = "Base selling price (>= 0)", example = "129.99")
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Base price must be non-negative")
    private BigDecimal basePrice;

    @Schema(description = "Optional discounted price — must be less than base price", example = "99.99")
    @DecimalMin(value = "0.00", inclusive = true, message = "Discount price must be non-negative")
    private BigDecimal discountPrice;

    @Schema(description = "ID of the seller (must reference an existing user with seller/admin role)",
            example = "1")
    @NotNull(message = "Seller ID is required")
    private Long sellerId;

    @Schema(description = "ID of the product category (optional)", example = "3")
    private Integer categoryId;

    @Schema(description = "Listing status", example = "draft",
            allowableValues = {"active", "inactive", "draft"}, defaultValue = "draft")
    @ValidEnum(allowed = {"active", "inactive", "draft"})
    private String status = "draft";

    @Schema(description = "Initial stock quantity (>= 0)", example = "50", defaultValue = "0")
    @Min(value = 0, message = "Stock quantity must be non-negative")
    private int stockQuantity = 0;

    @Schema(description = "Reorder threshold; triggers low-stock alerts", example = "10", defaultValue = "10")
    @Min(value = 0, message = "Reorder level must be non-negative")
    private int reorderLevel = 10;

    public String     getName()                      { return name; }
    public void       setName(String v)              { this.name = v; }

    public String     getSlug()                      { return slug; }
    public void       setSlug(String v)              { this.slug = v; }

    public String     getDescription()               { return description; }
    public void       setDescription(String v)       { this.description = v; }

    public BigDecimal getBasePrice()                 { return basePrice; }
    public void       setBasePrice(BigDecimal v)     { this.basePrice = v; }

    public BigDecimal getDiscountPrice()             { return discountPrice; }
    public void       setDiscountPrice(BigDecimal v) { this.discountPrice = v; }

    public Long       getSellerId()                  { return sellerId; }
    public void       setSellerId(Long v)            { this.sellerId = v; }

    public Integer    getCategoryId()                { return categoryId; }
    public void       setCategoryId(Integer v)       { this.categoryId = v; }

    public String     getStatus()                    { return status; }
    public void       setStatus(String v)            { this.status = v; }

    public int        getStockQuantity()             { return stockQuantity; }
    public void       setStockQuantity(int v)        { this.stockQuantity = v; }

    public int        getReorderLevel()              { return reorderLevel; }
    public void       setReorderLevel(int v)         { this.reorderLevel = v; }
}
