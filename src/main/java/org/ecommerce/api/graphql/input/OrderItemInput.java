package org.ecommerce.api.graphql.input;

import org.ecommerce.api.dto.request.OrderItemRequest;

/** Bound from the GraphQL {@code OrderItemInput} mutation input type via {@code @Argument}. */
public class OrderItemInput {

    private Long productId;
    private int  quantity;

    public OrderItemRequest toRequest() {
        OrderItemRequest req = new OrderItemRequest();
        req.setProductId(productId);
        req.setQuantity(quantity);
        return req;
    }

    public Long getProductId()          { return productId; }
    public void setProductId(Long v)    { this.productId = v; }

    public int  getQuantity()           { return quantity; }
    public void setQuantity(int v)      { this.quantity = v; }
}
