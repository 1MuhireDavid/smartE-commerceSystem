package org.ecommerce.model;

import java.time.LocalDateTime;

/**
 * Represents one row in the activity_logs table.
 *
 * <p>The {@code eventData} field stores a raw JSONB string.  Different
 * event types carry completely different payload shapes — that is precisely
 * why this table uses PostgreSQL JSONB (NoSQL) rather than fixed columns.
 *
 * <p>Examples:
 * <pre>
 *   page_view   → {"page":"product","product_id":1,"duration_sec":45}
 *   search      → {"query":"macbook","results_count":2,"response_ms":12}
 *   order_placed→ {"order_number":"ORD-001","total":34.99,"payment_method":"card"}
 * </pre>
 */
public class ActivityLog {

    private long          logId;
    private Long          userId;       // nullable — system events have no user
    private String        userName;     // denormalised from JOIN
    private String        eventType;
    private String        eventData;    // raw JSONB string from PostgreSQL
    private LocalDateTime loggedAt;

    public ActivityLog() {}

    // ── Getters / setters ─────────────────────────────────────────────────────

    public long          getLogId()                      { return logId; }
    public void          setLogId(long logId)            { this.logId = logId; }

    public Long          getUserId()                     { return userId; }
    public void          setUserId(Long userId)          { this.userId = userId; }


    public void          setUserName(String n)           { this.userName = n; }

    public String        getEventType()                  { return eventType; }
    public void          setEventType(String t)          { this.eventType = t; }

    public String        getEventData()                  { return eventData; }
    public void          setEventData(String d)          { this.eventData = d; }

    public LocalDateTime getLoggedAt()                   { return loggedAt; }
    public void          setLoggedAt(LocalDateTime t)    { this.loggedAt = t; }

    /** Display-friendly user label ("system" when userId is null). */
    public String        getDisplayUser() {
        if (userName != null && !userName.isBlank()) return userName;
        return userId == null ? "system" : "user #" + userId;
    }
}
