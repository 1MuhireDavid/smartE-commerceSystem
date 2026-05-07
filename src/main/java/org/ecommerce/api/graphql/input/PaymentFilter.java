package org.ecommerce.api.graphql.input;

/** Bound from the GraphQL {@code PaymentFilter} input type via {@code @Argument}. */
public class PaymentFilter {

    private Long   orderId;
    private String status;

    public Long   getOrderId()         { return orderId; }
    public void   setOrderId(Long v)   { this.orderId = v; }

    public String getStatus()          { return status; }
    public void   setStatus(String v)  { this.status = v; }
}
