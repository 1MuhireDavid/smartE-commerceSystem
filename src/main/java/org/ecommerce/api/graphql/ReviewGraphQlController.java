package org.ecommerce.api.graphql;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.entity.ProductEntity;
import org.ecommerce.api.entity.ReviewEntity;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.graphql.input.ReviewFilter;
import org.ecommerce.api.graphql.input.ReviewInput;
import org.ecommerce.api.repository.ProductRepository;
import org.ecommerce.api.repository.UserRepository;
import org.ecommerce.api.service.ReviewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL resolver for Review queries, mutations, and nested field resolvers.
 *
 * <p>Because OSIV is disabled, nested field resolvers load related entities
 * using scalar FK columns rather than traversing lazy associations.
 */
@Controller
@Transactional(readOnly = true)
public class ReviewGraphQlController {

    private final ReviewService     reviewService;
    private final ProductRepository productRepository;
    private final UserRepository    userRepository;

    public ReviewGraphQlController(ReviewService reviewService,
                                   ProductRepository productRepository,
                                   UserRepository userRepository) {
        this.reviewService     = reviewService;
        this.productRepository = productRepository;
        this.userRepository    = userRepository;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public ReviewEntity review(@Argument Long id) {
        return reviewService.findById(id);
    }

    @QueryMapping
    public PagedResponse<ReviewEntity> reviews(
            @Argument ReviewFilter filter,
            @Argument int page,
            @Argument int size) {

        Long    productId = filter != null ? filter.getProductId() : null;
        Boolean approved  = filter != null ? filter.getApproved()  : null;

        return reviewService.findAll(productId, approved, PageRequest.of(page, size));
    }

    // ── Nested field resolvers ─────────────────────────────────────────────────

    /**
     * Uses findByIdWithAssociations so Product sub-fields (seller, category,
     * inventory) are already initialized when ProductGraphQlController resolves them.
     */
    @SchemaMapping(typeName = "Review", field = "product")
    public ProductEntity product(ReviewEntity review) {
        Long pid = review.getProductId();
        return pid != null
                ? productRepository.findByIdWithAssociations(pid).orElse(null)
                : null;
    }

    @SchemaMapping(typeName = "Review", field = "user")
    public UserEntity user(ReviewEntity review) {
        Long uid = review.getUserId();
        return uid != null ? userRepository.findById(uid).orElse(null) : null;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    @Transactional
    public ReviewEntity createReview(@Argument ReviewInput input) {
        return reviewService.create(input.toRequest());
    }

    @MutationMapping
    @Transactional
    public ReviewEntity approveReview(@Argument Long id) {
        return reviewService.approve(id);
    }

    @MutationMapping
    @Transactional
    public boolean deleteReview(@Argument Long id) {
        reviewService.delete(id);
        return true;
    }
}
