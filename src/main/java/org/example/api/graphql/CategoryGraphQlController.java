package org.example.api.graphql;

import org.example.api.dto.PagedResponse;
import org.example.api.entity.CategoryEntity;
import org.example.api.graphql.input.CategoryFilter;
import org.example.api.graphql.input.CategoryInput;
import org.example.api.service.CategoryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL resolver for Category queries, mutations, and the self-referential
 * {@code Category.parent} field.
 */
@Controller
@Transactional(readOnly = true)
public class CategoryGraphQlController {

    private final CategoryService categoryService;

    public CategoryGraphQlController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public CategoryEntity category(@Argument Integer id) {
        return categoryService.findById(id);
    }

    @QueryMapping
    public PagedResponse<CategoryEntity> categories(
            @Argument CategoryFilter filter,
            @Argument int page,
            @Argument int size) {

        String  keyword = filter != null ? filter.getKeyword() : null;
        Boolean active  = filter != null ? filter.getActive()  : null;

        Sort sort = Sort.by("displayOrder").ascending().and(Sort.by("name").ascending());
        return categoryService.findAll(keyword, active, PageRequest.of(page, size, sort));
    }

    // ── Field resolver: Category.parent ──────────────────────────────────────

    /**
     * Resolves the parent category lazily.
     * Spring GraphQL calls this once per {@code Category} object that has a non-null parent.
     */
    @SchemaMapping(typeName = "Category", field = "parent")
    public CategoryEntity parent(CategoryEntity category) {
        CategoryEntity parent = category.getParent();
        if (parent == null) return null;
        // Re-fetch to ensure the entity is fully loaded inside this transaction
        return categoryService.findById(parent.getCategoryId());
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    @Transactional
    public CategoryEntity createCategory(@Argument CategoryInput input) {
        return categoryService.create(input.toRequest());
    }

    @MutationMapping
    @Transactional
    public CategoryEntity updateCategory(@Argument Integer id, @Argument CategoryInput input) {
        return categoryService.update(id, input.toRequest());
    }

    @MutationMapping
    @Transactional
    public boolean deleteCategory(@Argument Integer id) {
        categoryService.delete(id);
        return true;
    }
}
