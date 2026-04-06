package org.example.model;

import java.time.LocalDateTime;

/**
 * Represents a normalized product review (matches the reviews table in the schema).
 * A separate activity_logs table (JSONB) handles unstructured event data (NoSQL).
 */
public class Review {

    private long   reviewId;
    private long   productId;
    private String productName;  // denormalised for display
    private long   userId;
    private String userName;     // denormalised for display
    private Long   orderId;      // nullable
    private int    rating;       // 1–5
    private String title;
    private String body;
    private boolean isApproved;
    private LocalDateTime createdAt;

    public Review() {}

    public Review(long productId, long userId, Long orderId,
                  int rating, String title, String body) {
        this.productId = productId;
        this.userId    = userId;
        this.orderId   = orderId;
        this.rating    = rating;
        this.title     = title;
        this.body      = body;
    }

    public long    getReviewId()                    { return reviewId; }
    public void    setReviewId(long reviewId)       { this.reviewId = reviewId; }

    public long    getProductId()                   { return productId; }
    public void    setProductId(long productId)     { this.productId = productId; }

    public String  getProductName()                 { return productName; }
    public void    setProductName(String s)         { this.productName = s; }

    public long    getUserId()                      { return userId; }
    public void    setUserId(long userId)           { this.userId = userId; }

    public String  getUserName()                    { return userName; }
    public void    setUserName(String s)            { this.userName = s; }

    public Long    getOrderId()                     { return orderId; }
    public void    setOrderId(Long orderId)         { this.orderId = orderId; }

    public int     getRating()                      { return rating; }
    public void    setRating(int rating)            { this.rating = rating; }

    public String  getTitle()                       { return title; }
    public void    setTitle(String title)           { this.title = title; }

    public String  getBody()                        { return body; }
    public void    setBody(String body)             { this.body = body; }

    public boolean isApproved()                     { return isApproved; }
    public void    setApproved(boolean approved)    { this.isApproved = approved; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void    setCreatedAt(LocalDateTime t)    { this.createdAt = t; }

    /** Star string for display. */
    public String  getStars() {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }
}
