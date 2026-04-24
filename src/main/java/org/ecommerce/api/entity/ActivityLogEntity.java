package org.ecommerce.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_data", nullable = false, columnDefinition = "jsonb")
    private String eventData;

    @Column(name = "logged_at", nullable = false, updatable = false)
    private LocalDateTime loggedAt;

    @PrePersist
    void onCreate() { loggedAt = LocalDateTime.now(); }

    public ActivityLogEntity() {}

    public Long getLogId()                               { return logId; }
    public void setLogId(Long logId)                     { this.logId = logId; }

    public UserEntity getUser()                          { return user; }
    public void setUser(UserEntity user)                 { this.user = user; }

    public Long getUserId()                              { return userId; }

    public String getEventType()                         { return eventType; }
    public void setEventType(String eventType)           { this.eventType = eventType; }

    public String getEventData()                         { return eventData; }
    public void setEventData(String eventData)           { this.eventData = eventData; }

    public LocalDateTime getLoggedAt()                   { return loggedAt; }
    public void setLoggedAt(LocalDateTime t)             { this.loggedAt = t; }
}
