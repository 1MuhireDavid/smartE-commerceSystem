package org.ecommerce.api.graphql.input;

import org.ecommerce.api.dto.request.ReviewRequest;

/** Bound from the GraphQL {@code ReviewInput} mutation input type via {@code @Argument}. */
public class ReviewInput {

    private Long   productId;
    private Long   userId;
    private Long   orderId;
    private int    rating;
    private String title;
    private String body;

    public ReviewRequest toRequest() {
        ReviewRequest req = new ReviewRequest();
        req.setProductId(productId);
        req.setUserId(userId);
        req.setOrderId(orderId);
        req.setRating((short) rating);
        req.setTitle(title);
        req.setBody(body);
        return req;
    }

    public Long   getProductId()          { return productId; }
    public void   setProductId(Long v)    { this.productId = v; }

    public Long   getUserId()             { return userId; }
    public void   setUserId(Long v)       { this.userId = v; }

    public Long   getOrderId()            { return orderId; }
    public void   setOrderId(Long v)      { this.orderId = v; }

    public int    getRating()             { return rating; }
    public void   setRating(int v)        { this.rating = v; }

    public String getTitle()              { return title; }
    public void   setTitle(String v)      { this.title = v; }

    public String getBody()               { return body; }
    public void   setBody(String v)       { this.body = v; }
}
