package org.ecommerce.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "carts")
public class CartEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long cartId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        active = true;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public CartEntity() {}

    public Long getCartId()                              { return cartId; }
    public void setCartId(Long cartId)                   { this.cartId = cartId; }

    public UserEntity getUser()                          { return user; }
    public void setUser(UserEntity user)                 { this.user = user; }

    public Long getUserId()                              { return userId; }

    public boolean isActive()                            { return active; }
    public void setActive(boolean active)                { this.active = active; }

    public LocalDateTime getCreatedAt()                  { return createdAt; }
    public void setCreatedAt(LocalDateTime t)            { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()                  { return updatedAt; }
    public void setUpdatedAt(LocalDateTime t)            { this.updatedAt = t; }
}
