package org.ecommerce.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for opening a new shopping cart")
public class CartRequest {

    @Schema(description = "ID of the user who owns this cart", example = "1")
    @NotNull(message = "User ID is required")
    private Long userId;

    public Long getUserId()             { return userId; }
    public void setUserId(Long userId)  { this.userId = userId; }
}
