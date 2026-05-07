package org.ecommerce.api.graphql.input;

import org.ecommerce.api.dto.request.OrderRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/** Bound from the GraphQL {@code OrderInput} mutation input type via {@code @Argument}. */
public class OrderInput {

    private Long               userId;
    private List<OrderItemInput> items;
    private double             discountAmount = 0;

    public OrderRequest toRequest() {
        OrderRequest req = new OrderRequest();
        req.setUserId(userId);
        req.setItems(items.stream().map(OrderItemInput::toRequest).collect(Collectors.toList()));
        req.setDiscountAmount(BigDecimal.valueOf(discountAmount));
        return req;
    }

    public Long getUserId()                        { return userId; }
    public void setUserId(Long v)                  { this.userId = v; }

    public List<OrderItemInput> getItems()         { return items; }
    public void setItems(List<OrderItemInput> v)   { this.items = v; }

    public double getDiscountAmount()              { return discountAmount; }
    public void   setDiscountAmount(double v)      { this.discountAmount = v; }
}
