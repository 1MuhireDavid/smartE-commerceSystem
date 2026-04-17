package org.ecommerce.api.graphql.input;

import org.ecommerce.api.dto.request.ProductRequest;

import java.math.BigDecimal;

/** Bound from the GraphQL {@code ProductInput} mutation input type via {@code @Argument}. */
public class ProductInput {

    private String  name;
    private String  slug;
    private String  description;
    private Double  basePrice;
    private Double  discountPrice;
    private Long    sellerId;
    private Integer categoryId;
    private String  status        = "draft";
    private int     stockQuantity = 0;
    private int     reorderLevel  = 10;

    /** Converts this GraphQL input into the DTO expected by the service layer. */
    public ProductRequest toRequest() {
        ProductRequest req = new ProductRequest();
        req.setName(name);
        req.setSlug(slug);
        req.setDescription(description);
        req.setBasePrice(basePrice != null ? BigDecimal.valueOf(basePrice) : null);
        req.setDiscountPrice(discountPrice != null ? BigDecimal.valueOf(discountPrice) : null);
        req.setSellerId(sellerId);
        req.setCategoryId(categoryId);
        req.setStatus(status);
        req.setStockQuantity(stockQuantity);
        req.setReorderLevel(reorderLevel);
        return req;
    }

    public String  getName()                      { return name; }
    public void    setName(String v)              { this.name = v; }

    public String  getSlug()                      { return slug; }
    public void    setSlug(String v)              { this.slug = v; }

    public String  getDescription()               { return description; }
    public void    setDescription(String v)       { this.description = v; }

    public Double  getBasePrice()                 { return basePrice; }
    public void    setBasePrice(Double v)         { this.basePrice = v; }

    public Double  getDiscountPrice()             { return discountPrice; }
    public void    setDiscountPrice(Double v)     { this.discountPrice = v; }

    public Long    getSellerId()                  { return sellerId; }
    public void    setSellerId(Long v)            { this.sellerId = v; }

    public Integer getCategoryId()                { return categoryId; }
    public void    setCategoryId(Integer v)       { this.categoryId = v; }

    public String  getStatus()                    { return status; }
    public void    setStatus(String v)            { this.status = v; }

    public int     getStockQuantity()             { return stockQuantity; }
    public void    setStockQuantity(int v)        { this.stockQuantity = v; }

    public int     getReorderLevel()              { return reorderLevel; }
    public void    setReorderLevel(int v)         { this.reorderLevel = v; }
}
