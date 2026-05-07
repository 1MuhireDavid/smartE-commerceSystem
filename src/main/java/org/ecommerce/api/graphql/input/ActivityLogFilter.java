package org.ecommerce.api.graphql.input;

/** Bound from the GraphQL {@code ActivityLogFilter} input type via {@code @Argument}. */
public class ActivityLogFilter {

    private Long   userId;
    private String eventType;

    public Long   getUserId()               { return userId; }
    public void   setUserId(Long v)         { this.userId = v; }

    public String getEventType()            { return eventType; }
    public void   setEventType(String v)    { this.eventType = v; }
}
