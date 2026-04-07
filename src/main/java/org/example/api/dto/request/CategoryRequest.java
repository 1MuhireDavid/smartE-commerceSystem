package org.example.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.example.api.validation.ValidSlug;

@Schema(description = "Payload for creating or updating a category")
public class CategoryRequest {

    @Schema(description = "Category display name", example = "Electronics", maxLength = 100)
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Schema(description = "URL-friendly slug (lowercase letters, digits, single hyphens)",
            example = "electronics", maxLength = 120)
    @NotBlank(message = "Slug is required")
    @Size(max = 120, message = "Slug must not exceed 120 characters")
    @ValidSlug
    private String slug;

    @Schema(description = "ID of the parent category (null for top-level)", example = "null")
    private Integer parentId;

    @Schema(description = "Whether the category is active", defaultValue = "true")
    private boolean active = true;

    @Schema(description = "Ordering position within the same level", example = "0", defaultValue = "0")
    @Min(value = 0, message = "Display order must be non-negative")
    private int displayOrder = 0;

    public String  getName()                  { return name; }
    public void    setName(String v)          { this.name = v; }

    public String  getSlug()                  { return slug; }
    public void    setSlug(String v)          { this.slug = v; }

    public Integer getParentId()              { return parentId; }
    public void    setParentId(Integer v)     { this.parentId = v; }

    public boolean isActive()                 { return active; }
    public void    setActive(boolean v)       { this.active = v; }

    public int     getDisplayOrder()          { return displayOrder; }
    public void    setDisplayOrder(int v)     { this.displayOrder = v; }
}
