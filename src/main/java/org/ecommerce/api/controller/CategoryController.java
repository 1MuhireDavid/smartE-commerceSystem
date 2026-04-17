package org.ecommerce.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.CategoryRequest;
import org.ecommerce.api.entity.CategoryEntity;
import org.ecommerce.api.service.CategoryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Categories", description = "Administrator endpoints for product category management")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(
        summary     = "List categories",
        description = "Returns a paginated list of categories sorted by display order then name. "
                    + "Supports keyword and active-status filtering."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid query parameter",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<PagedResponse<CategoryEntity>>> list(
            @Parameter(description = "Partial match on category name")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean active,

            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("displayOrder").ascending().and(Sort.by("name").ascending()));

        PagedResponse<CategoryEntity> data = categoryService.findAll(keyword, active, pageable);
        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success("Categories retrieved successfully", data));
    }

    @Operation(summary = "Get category by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category found"),
        @ApiResponse(responseCode = "404", description = "Category not found",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<CategoryEntity>> getById(
            @Parameter(description = "Category ID", required = true, example = "1")
            @PathVariable int id) {
        CategoryEntity category = categoryService.findById(id);
        return ResponseEntity.ok(org.ecommerce.api.dto.ApiResponse.success(category));
    }

    @Operation(
        summary     = "Create category",
        description = "Creates a new category. Slug must be globally unique."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Category created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "Slug already in use",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @PostMapping
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<CategoryEntity>> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Category details", required = true)
            @Valid @RequestBody CategoryRequest request) {
        CategoryEntity created = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(org.ecommerce.api.dto.ApiResponse.success("Category created successfully", created));
    }

    @Operation(summary = "Update category", description = "Replaces all fields of an existing category.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "404", description = "Category not found",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "Slug already in use",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<CategoryEntity>> update(
            @Parameter(description = "Category ID", required = true, example = "1")
            @PathVariable int id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryEntity updated = categoryService.update(id, request);
        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success("Category updated successfully", updated));
    }

    @Operation(summary = "Delete category",
               description = "Deletes a category. Products in this category will have their "
                           + "category set to null (ON DELETE SET NULL).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Category deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Category not found",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<Void>> delete(
            @Parameter(description = "Category ID", required = true, example = "1")
            @PathVariable int id) {
        categoryService.delete(id);
        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success("Category deleted successfully", null));
    }
}
