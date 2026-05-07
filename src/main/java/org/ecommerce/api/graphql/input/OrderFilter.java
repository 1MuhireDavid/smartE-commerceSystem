package org.ecommerce.api.graphql.input;

/** Bound from the GraphQL {@code OrderFilter} input type via {@code @Argument}. */
public class OrderFilter {

    private Long   userId;
    private String status;

    public Long   getUserId()          { return userId; }
    public void   setUserId(Long v)    { this.userId = v; }

    public String getStatus()          { return status; }
    public void   setStatus(String v)  { this.status = v; }
}
