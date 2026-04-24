package org.ecommerce.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.ecommerce.api.dto.ApiResponse;
import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.PaymentRequest;
import org.ecommerce.api.entity.PaymentEntity;
import org.ecommerce.api.service.PaymentService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payments", description = "Payment recording and status transitions")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "List payments", description = "Filterable by order and payment status.")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PaymentEntity>>> list(
            @Parameter(description = "Filter by order ID", example = "1")
            @RequestParam(required = false) Long orderId,

            @Parameter(description = "Filter by payment status",
                       schema = @Schema(allowableValues = {"pending", "completed", "failed", "refunded"}))
            @RequestParam(required = false) String status,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        int clampedSize = Math.min(Math.max(size, 1), 100);
        PagedResponse<PaymentEntity> data = paymentService.findAll(
                orderId, status,
                PageRequest.of(page, clampedSize, Sort.by("paymentId").descending()));

        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", data));
    }

    @Operation(summary = "Get payment by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentEntity>> getById(
            @Parameter(description = "Payment ID", example = "1") @PathVariable long id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.findById(id)));
    }

    @Operation(summary = "Record a new payment against an order")
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentEntity>> create(@Valid @RequestBody PaymentRequest request) {
        PaymentEntity created = paymentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded successfully", created));
    }

    @Operation(summary = "Update payment status",
               description = "Setting status to 'completed' also records the paidAt timestamp.")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PaymentEntity>> updateStatus(
            @Parameter(description = "Payment ID", example = "1") @PathVariable long id,
            @Parameter(description = "New status",
                       schema = @Schema(allowableValues = {"pending", "completed", "failed", "refunded"}))
            @RequestParam String status) {
        PaymentEntity updated = paymentService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Payment status updated", updated));
    }
}
