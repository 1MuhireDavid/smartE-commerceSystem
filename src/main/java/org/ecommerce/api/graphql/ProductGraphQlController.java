package org.ecommerce.api.graphql;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.entity.CategoryEntity;
import org.ecommerce.api.entity.InventoryEntity;
import org.ecommerce.api.entity.ProductEntity;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.graphql.input.ProductFilter;
import org.ecommerce.api.graphql.input.ProductInput;
import org.ecommerce.api.repository.ProductRepository;
import org.ecommerce.api.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * GraphQL resolver for Product queries, mutations, and nested field resolvers
 * ({@code Product.seller}, {@code Product.category}, {@code Product.inventory}).
 *
 * <p>Uses {@code findByIdWithAssociations} for single-product fetches so that
 * all JOIN FETCH associations are loaded before the transaction closes, preventing
 * LazyInitializationException when GraphQL accesses nested fields.
 */
@Controller
@Transactional(readOnly = true)
public class ProductGraphQlController {

    private final ProductService    productService;
    private final ProductRepository productRepository;

    public ProductGraphQlController(ProductService productService,
                                    ProductRepository productRepository) {
        this.productService    = productService;
        this.productRepository = productRepository;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public ProductEntity product(@Argument Long id) {
        return productRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found with id: " + id));
    }

    @QueryMapping
    public PagedResponse<ProductEntity> products(
            @Argument ProductFilter filter,
            @Argument int page,
            @Argument int size,
            @Argument String sortBy,
            @Argument String sortDir) {

        String  keyword    = filter != null ? filter.getKeyword()    : null;
        Integer categoryId = filter != null ? filter.getCategoryId() : null;
        String  status     = filter != null ? filter.getStatus()     : null;
        Long    sellerId   = filter != null ? filter.getSellerId()   : null;

        int clampedSize = Math.min(Math.max(size, 1), 100);

        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return productService.findAll(keyword, categoryId, status, sellerId,
                PageRequest.of(page, clampedSize, sort));
    }

    // ── Nested field resolvers ─────────────────────────────────────────────────
    //
    // These are called once per Product in the result set.
    // For products returned by the `products` query the associations are already
    // loaded via JOIN FETCH, so no extra SQL is fired here.

    @SchemaMapping(typeName = "Product", field = "seller")
    public UserEntity seller(ProductEntity product) {
        return product.getSeller();
    }

    @SchemaMapping(typeName = "Product", field = "category")
    public CategoryEntity category(ProductEntity product) {
        return product.getCategory();
    }

    @SchemaMapping(typeName = "Product", field = "inventory")
    public InventoryEntity inventory(ProductEntity product) {
        return product.getInventory();
    }

    /**
     * Computes {@code effectivePrice} — the discount price when set, otherwise the base price.
     * Exposed as a virtual field in the schema; not stored in the DB.
     */
    @SchemaMapping(typeName = "Product", field = "effectivePrice")
    public Double effectivePrice(ProductEntity product) {
        var price = product.getEffectivePrice();
        return price != null ? price.doubleValue() : null;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    @Transactional
    public ProductEntity createProduct(@Argument ProductInput input) {
        return productService.create(input.toRequest());
    }

    @MutationMapping
    @Transactional
    public ProductEntity updateProduct(@Argument Long id, @Argument ProductInput input) {
        return productService.update(id, input.toRequest());
    }

    @MutationMapping
    @Transactional
    public boolean deleteProduct(@Argument Long id) {
        productService.delete(id);
        return true;
    }
}
