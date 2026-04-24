package org.ecommerce.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews",
       uniqueConstraints = @UniqueConstraint(name = "uq_user_product_order",
                                             columnNames = {"user_id", "product_id"}))
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "product_id", insertable = false, updatable = false)
    private Long productId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(name = "order_id", insertable = false, updatable = false)
    private Long orderId;

    @Column(name = "rating", nullable = false)
    private short rating;

    @Column(name = "title", length = 150)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_approved", nullable = false)
    private boolean approved;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public ReviewEntity() {}

    public Long getReviewId()                            { return reviewId; }
    public void setReviewId(Long reviewId)               { this.reviewId = reviewId; }

    public ProductEntity getProduct()                    { return product; }
    public void setProduct(ProductEntity product)        { this.product = product; }

    public Long getProductId()                           { return productId; }

    public UserEntity getUser()                          { return user; }
    public void setUser(UserEntity user)                 { this.user = user; }

    public Long getUserId()                              { return userId; }

    public OrderEntity getOrder()                        { return order; }
    public void setOrder(OrderEntity order)              { this.order = order; }

    public Long getOrderId()                             { return orderId; }

    public short getRating()                             { return rating; }
    public void setRating(short rating)                  { this.rating = rating; }

    public String getTitle()                             { return title; }
    public void setTitle(String title)                   { this.title = title; }

    public String getBody()                              { return body; }
    public void setBody(String body)                     { this.body = body; }

    public boolean isApproved()                          { return approved; }
    public void setApproved(boolean approved)            { this.approved = approved; }

    public LocalDateTime getCreatedAt()                  { return createdAt; }
    public void setCreatedAt(LocalDateTime t)            { this.createdAt = t; }
}
