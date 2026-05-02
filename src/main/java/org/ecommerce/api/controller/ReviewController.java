package org.ecommerce.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.ecommerce.api.dto.ApiResponseDto;
import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.ReviewRequest;
import org.ecommerce.api.entity.ReviewEntity;
import org.ecommerce.api.service.ReviewService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Reviews", description = "Product reviews — submission and moderation")
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "List reviews", description = "Filterable by product and approval status.")
    @GetMapping
    public ResponseEntity<ApiResponseDto<PagedResponse<ReviewEntity>>> list(
            @Parameter(description = "Filter by product ID", example = "5")
            @RequestParam(required = false) Long productId,

            @Parameter(description = "Filter by approval status", example = "true")
            @RequestParam(required = false) Boolean approved,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        int clampedSize = Math.min(Math.max(size, 1), 100);
        PagedResponse<ReviewEntity> data = reviewService.findAll(
                productId, approved,
                PageRequest.of(page, clampedSize, Sort.by("createdAt").descending()));

        return ResponseEntity.ok(ApiResponseDto.success("Reviews retrieved successfully", data));
    }

    @Operation(summary = "Get review by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ReviewEntity>> getById(
            @Parameter(description = "Review ID", example = "1") @PathVariable long id) {
        return ResponseEntity.ok(ApiResponseDto.success(reviewService.findById(id)));
    }

    @Operation(summary = "Submit a product review",
               description = "Each user may only review a given product once.")
    @PostMapping
    public ResponseEntity<ApiResponseDto<ReviewEntity>> create(@Valid @RequestBody ReviewRequest request) {
        ReviewEntity created = reviewService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success("Review submitted successfully", created));
    }

    @Operation(summary = "Approve a review",
               description = "Marks the review as approved so it becomes publicly visible.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponseDto<ReviewEntity>> approve(
            @Parameter(description = "Review ID", example = "1") @PathVariable long id) {
        ReviewEntity approved = reviewService.approve(id);
        return ResponseEntity.ok(ApiResponseDto.success("Review approved", approved));
    }

    @Operation(summary = "Delete a review")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> delete(
            @Parameter(description = "Review ID", example = "1") @PathVariable long id) {
        reviewService.delete(id);
        return ResponseEntity.ok(ApiResponseDto.success("Review deleted", null));
    }
}
