package org.ecommerce.api.graphql.input;

import org.ecommerce.api.dto.request.ActivityLogRequest;

/** Bound from the GraphQL {@code ActivityLogInput} mutation input type via {@code @Argument}. */
public class ActivityLogInput {

    private Long   userId;
    private String eventType;
    private String eventData;

    public ActivityLogRequest toRequest() {
        ActivityLogRequest req = new ActivityLogRequest();
        req.setUserId(userId);
        req.setEventType(eventType);
        req.setEventData(eventData);
        return req;
    }

    public Long   getUserId()               { return userId; }
    public void   setUserId(Long v)         { this.userId = v; }

    public String getEventType()            { return eventType; }
    public void   setEventType(String v)    { this.eventType = v; }

    public String getEventData()            { return eventData; }
    public void   setEventData(String v)    { this.eventData = v; }
}
