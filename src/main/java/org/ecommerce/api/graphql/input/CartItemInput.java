package org.ecommerce.api.graphql.input;

import org.ecommerce.api.dto.request.CartItemRequest;

/** Bound from the GraphQL {@code CartItemInput} mutation input type via {@code @Argument}. */
public class CartItemInput {

    private Long productId;
    private int  quantity = 1;

    public CartItemRequest toRequest() {
        CartItemRequest req = new CartItemRequest();
        req.setProductId(productId);
        req.setQuantity(quantity);
        return req;
    }

    public Long getProductId()          { return productId; }
    public void setProductId(Long v)    { this.productId = v; }

    public int  getQuantity()           { return quantity; }
    public void setQuantity(int v)      { this.quantity = v; }
}
