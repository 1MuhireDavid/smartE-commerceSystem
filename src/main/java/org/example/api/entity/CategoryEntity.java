package org.example.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public CategoryEntity() {}

    public Integer getCategoryId()                     { return categoryId; }
    public void setCategoryId(Integer id)              { this.categoryId = id; }

    public String getName()                            { return name; }
    public void setName(String name)                   { this.name = name; }

    public String getSlug()                            { return slug; }
    public void setSlug(String slug)                   { this.slug = slug; }

    public boolean isActive()                          { return active; }
    public void setActive(boolean active)              { this.active = active; }

    public int getDisplayOrder()                       { return displayOrder; }
    public void setDisplayOrder(int displayOrder)      { this.displayOrder = displayOrder; }
}
