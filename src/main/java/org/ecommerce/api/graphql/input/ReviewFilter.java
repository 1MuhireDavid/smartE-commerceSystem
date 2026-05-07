package org.ecommerce.api.graphql.input;

/** Bound from the GraphQL {@code ReviewFilter} input type via {@code @Argument}. */
public class ReviewFilter {

    private Long    productId;
    private Boolean approved;

    public Long    getProductId()           { return productId; }
    public void    setProductId(Long v)     { this.productId = v; }

    public Boolean getApproved()            { return approved; }
    public void    setApproved(Boolean v)   { this.approved = v; }
}
