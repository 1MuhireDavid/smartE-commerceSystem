package org.example.api.graphql.input;

import org.example.api.dto.request.CategoryRequest;

/** Bound from the GraphQL {@code CategoryInput} mutation input type via {@code @Argument}. */
public class CategoryInput {

    private String  name;
    private String  slug;
    private boolean active       = true;
    private int     displayOrder = 0;

    /** Converts this GraphQL input into the DTO expected by the service layer. */
    public CategoryRequest toRequest() {
        CategoryRequest req = new CategoryRequest();
        req.setName(name);
        req.setSlug(slug);
        req.setActive(active);
        req.setDisplayOrder(displayOrder);
        return req;
    }

    public String  getName()                  { return name; }
    public void    setName(String v)          { this.name = v; }

    public String  getSlug()                  { return slug; }
    public void    setSlug(String v)          { this.slug = v; }

    public boolean isActive()                 { return active; }
    public void    setActive(boolean v)       { this.active = v; }

    public int     getDisplayOrder()          { return displayOrder; }
    public void    setDisplayOrder(int v)     { this.displayOrder = v; }
}
