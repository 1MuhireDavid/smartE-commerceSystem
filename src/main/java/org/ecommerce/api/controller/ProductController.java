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
import org.ecommerce.api.dto.request.ProductRequest;
import org.ecommerce.api.entity.ProductEntity;
import org.ecommerce.api.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Products", description = "Product catalog — browsing for customers, management for admins/sellers")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(
        summary     = "List products",
        description = """
            Returns a paginated, filterable, and sortable product catalog.

            **Filtering** — all parameters are optional and combinable:
            - `keyword` — case-insensitive partial match on product name
            - `categoryId` — filter by category
            - `status` — `active`, `inactive`, or `draft`
            - `sellerId` — filter by seller

            **Sorting** — pass `sortBy` + `sortDir`:
            - `sortBy`: `name`, `basePrice`, `avgRating`, `createdAt` (default)
            - `sortDir`: `asc` or `desc` (default)

            **Performance note**: the `name` field is backed by a B-tree index on `lower(name)`,
            keeping ILIKE searches O(log n). Results are cached client-side via HTTP cache headers.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid query parameter",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<PagedResponse<ProductEntity>>> list(
            @Parameter(description = "Partial product name search (case-insensitive)", example = "headphones")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Filter by category ID", example = "3")
            @RequestParam(required = false) Integer categoryId,

            @Parameter(description = "Filter by listing status",
                       schema = @Schema(allowableValues = {"active", "inactive", "draft"}))
            @RequestParam(required = false) String status,

            @Parameter(description = "Filter by seller (user) ID", example = "1")
            @RequestParam(required = false) Long sellerId,

            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size — clamped to 1–100", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Field to sort by",
                       schema = @Schema(allowableValues = {"name", "basePrice", "avgRating", "createdAt"}))
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction",
                       schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(defaultValue = "desc") String sortDir) {

        int clampedSize = Math.min(Math.max(size, 1), 100);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, clampedSize, sort);

        PagedResponse<ProductEntity> data =
                productService.findAll(keyword, categoryId, status, sellerId, pageable);

        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success("Products retrieved successfully", data));
    }

    @Operation(summary = "Get product by ID", description = "Returns a single product including inventory data.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<ProductEntity>> getById(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable long id) {
        ProductEntity product = productService.findById(id);
        return ResponseEntity.ok(org.ecommerce.api.dto.ApiResponse.success(product));
    }

    @Operation(
        summary     = "Create product",
        description = "Creates a new product and initialises its inventory record. "
                    + "Slug must be globally unique. "
                    + "Cross-field rule: discountPrice must be less than basePrice."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Product created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error (including discount > base price)",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "404", description = "Seller or category not found",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "Slug already in use",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    @PostMapping
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<ProductEntity>> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Product details", required = true)
            @Valid @RequestBody ProductRequest request) {
        ProductEntity created = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(org.ecommerce.api.dto.ApiResponse.success("Product created successfully", created));
    }

    @Operation(
        summary     = "Update product",
        description = "Replaces all mutable fields of an existing product. "
                    + "Inventory stock and reorder level are updated in the same transaction."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "Slug already in use",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<ProductEntity>> update(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable long id,
            @Valid @RequestBody ProductRequest request) {
        ProductEntity updated = productService.update(id, request);
        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success("Product updated successfully", updated));
    }

    @Operation(summary = "Delete product",
               description = "Permanently deletes a product and its inventory record (CASCADE).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<Void>> delete(
            @Parameter(description = "Product ID", required = true, example = "1")
            @PathVariable long id) {
        productService.delete(id);
        return ResponseEntity.ok(
                org.ecommerce.api.dto.ApiResponse.success("Product deleted successfully", null));
    }
}
