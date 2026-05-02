package org.ecommerce.api.graphql.input;

import org.ecommerce.api.dto.request.CartRequest;

/** Bound from the GraphQL {@code CartInput} mutation input type via {@code @Argument}. */
public class CartInput {

    private Long userId;

    public CartRequest toRequest() {
        CartRequest req = new CartRequest();
        req.setUserId(userId);
        return req;
    }

    public Long getUserId()          { return userId; }
    public void setUserId(Long v)    { this.userId = v; }
}
