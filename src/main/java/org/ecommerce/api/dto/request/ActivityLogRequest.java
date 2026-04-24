package org.ecommerce.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for recording a user activity event")
public class ActivityLogRequest {

    @Schema(description = "ID of the user who triggered the event (null for anonymous)", example = "1")
    private Long userId;

    @Schema(description = "Event category", example = "add_to_cart", maxLength = 50)
    @NotBlank(message = "Event type is required")
    @Size(max = 50, message = "Event type must not exceed 50 characters")
    private String eventType;

    @Schema(description = "Event payload as a JSON string",
            example = "{\"productId\": 5, \"quantity\": 2}")
    @NotBlank(message = "Event data is required")
    private String eventData;

    public Long   getUserId()               { return userId; }
    public void   setUserId(Long v)         { this.userId = v; }

    public String getEventType()            { return eventType; }
    public void   setEventType(String v)    { this.eventType = v; }

    public String getEventData()            { return eventData; }
    public void   setEventData(String v)    { this.eventData = v; }
}
