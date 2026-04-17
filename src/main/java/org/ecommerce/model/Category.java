package org.ecommerce.model;

import java.time.LocalDateTime;

public class Category {

    private int    id;
    private int    parentId;       // 0 = top-level
    private String parentName;    // denormalised for display
    private String name;
    private String slug;
    private boolean isActive;
    private int    displayOrder;
    private LocalDateTime createdAt;

    public Category() {}

    public Category(int id, int parentId, String parentName, String name,
                    String slug, boolean isActive, int displayOrder,
                    LocalDateTime createdAt) {
        this.id           = id;
        this.parentId     = parentId;
        this.parentName   = parentName;
        this.name         = name;
        this.slug         = slug;
        this.isActive     = isActive;
        this.displayOrder = displayOrder;
        this.createdAt    = createdAt;
    }

    /** Convenience constructor for new categories (no id/timestamps yet). */
    public Category(String name, String slug) {
        this.name     = name;
        this.slug     = slug;
        this.isActive = true;
    }

    public int     getId()                       { return id; }
    public void    setId(int id)                 { this.id = id; }

    public int     getParentId()                 { return parentId; }
    public void    setParentId(int parentId)     { this.parentId = parentId; }

    public String  getParentName()               { return parentName; }
    public void    setParentName(String s)       { this.parentName = s; }

    public String  getName()                     { return name; }
    public void    setName(String name)          { this.name = name; }

    public String  getSlug()                     { return slug; }
    public void    setSlug(String slug)          { this.slug = slug; }

    public boolean isActive()                    { return isActive; }
    public void    setActive(boolean active)     { this.isActive = active; }

    public int     getDisplayOrder()             { return displayOrder; }
    public void    setDisplayOrder(int order)    { this.displayOrder = order; }

    public LocalDateTime getCreatedAt()          { return createdAt; }
    public void    setCreatedAt(LocalDateTime t) { this.createdAt = t; }

    @Override public String toString()           { return name; }
}
