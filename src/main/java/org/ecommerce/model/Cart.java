package org.ecommerce.model;

import java.time.LocalDateTime;
import java.util.List;

public class Cart {

    private long          cartId;
    private long          userId;
    private String        userFullName;   // denormalised
    private boolean       active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CartItem> items;         // populated on demand

    public Cart() {}

    public long          getCartId()                        { return cartId; }
    public void          setCartId(long cartId)             { this.cartId = cartId; }

    public long          getUserId()                        { return userId; }
    public void          setUserId(long userId)             { this.userId = userId; }

    public String        getUserFullName()                  { return userFullName; }
    public void          setUserFullName(String s)          { this.userFullName = s; }

    public boolean       isActive()                         { return active; }
    public void          setActive(boolean active)          { this.active = active; }

    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void          setCreatedAt(LocalDateTime t)      { this.createdAt = t; }

    public LocalDateTime getUpdatedAt()                     { return updatedAt; }
    public void          setUpdatedAt(LocalDateTime t)      { this.updatedAt = t; }

    public List<CartItem> getItems()                        { return items; }
    public void           setItems(List<CartItem> items)    { this.items = items; }

    /** Convenience: total item count across all cart lines. */
    public int getTotalItems() {
        if (items == null) return 0;
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }
}
