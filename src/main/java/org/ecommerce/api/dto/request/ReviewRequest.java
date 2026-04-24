package org.ecommerce.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Payload for submitting a product review")
public class ReviewRequest {

    @Schema(description = "Product being reviewed", example = "5")
    @NotNull(message = "Product ID is required")
    private Long productId;

    @Schema(description = "User submitting the review", example = "1")
    @NotNull(message = "User ID is required")
    private Long userId;

    @Schema(description = "Related order ID (optional)", example = "3")
    private Long orderId;

    @Schema(description = "Star rating from 1 (worst) to 5 (best)", example = "4")
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Short rating;

    @Schema(description = "Short review headline", example = "Great product!", maxLength = 150)
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @Schema(description = "Full review text", example = "I have been using this for 3 months and it works perfectly.")
    private String body;

    public Long  getProductId()             { return productId; }
    public void  setProductId(Long v)       { this.productId = v; }

    public Long  getUserId()                { return userId; }
    public void  setUserId(Long v)          { this.userId = v; }

    public Long  getOrderId()               { return orderId; }
    public void  setOrderId(Long v)         { this.orderId = v; }

    public Short getRating()                { return rating; }
    public void  setRating(Short v)         { this.rating = v; }

    public String getTitle()                { return title; }
    public void   setTitle(String v)        { this.title = v; }

    public String getBody()                 { return body; }
    public void   setBody(String v)         { this.body = v; }
}
