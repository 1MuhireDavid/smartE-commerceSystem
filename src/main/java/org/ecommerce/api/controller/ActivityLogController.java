package org.ecommerce.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.ecommerce.api.dto.ApiResponse;
import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.ActivityLogRequest;
import org.ecommerce.api.entity.ActivityLogEntity;
import org.ecommerce.api.service.ActivityLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Activity Logs", description = "Append-only user event log")
@RestController
@RequestMapping("/api/activity-logs")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    @Operation(summary = "List activity logs", description = "Filterable by user and event type, sorted newest-first.")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ActivityLogEntity>>> list(
            @Parameter(description = "Filter by user ID", example = "1")
            @RequestParam(required = false) Long userId,

            @Parameter(description = "Filter by event type", example = "add_to_cart")
            @RequestParam(required = false) String eventType,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        int clampedSize = Math.min(Math.max(size, 1), 100);
        PagedResponse<ActivityLogEntity> data = activityLogService.findAll(
                userId, eventType,
                PageRequest.of(page, clampedSize, Sort.by("loggedAt").descending()));

        return ResponseEntity.ok(ApiResponse.success("Activity logs retrieved successfully", data));
    }

    @Operation(summary = "Record an activity event",
               description = "eventData must be a valid JSON string. userId is optional for anonymous events.")
    @PostMapping
    public ResponseEntity<ApiResponse<ActivityLogEntity>> create(
            @Valid @RequestBody ActivityLogRequest request) {
        ActivityLogEntity created = activityLogService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Activity logged", created));
    }
}
