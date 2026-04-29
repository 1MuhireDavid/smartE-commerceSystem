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
import org.ecommerce.api.dto.request.UserRequest;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "Administrator endpoints for user management")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
        summary     = "List users",
        description = "Returns a paginated, optionally filtered list of all users. "
                    + "Supports keyword search across full name, email, and username."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid query parameter",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @GetMapping
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<PagedResponse<UserEntity>>> list(
            @Parameter(description = "Partial match on fullName, email, or username")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Filter by role", schema = @Schema(allowableValues = {"customer", "seller", "admin"}))
            @RequestParam(required = false) String role,

            @Parameter(description = "Filter by account status")
            @RequestParam(required = false) Boolean active,

            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (1–100)", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field", schema = @Schema(allowableValues = {"fullName", "email", "createdAt"}))
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction", schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        PagedResponse<UserEntity> data = userService.findAll(keyword, role, active, pageable);

        return ResponseEntity.ok(org.ecommerce.api.dto.ApiResponse.success("Users retrieved successfully", data));
    }

    @Operation(summary = "Get user by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<UserEntity>> getById(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable long id) {
        UserEntity user = userService.findById(id);
        return ResponseEntity.ok(org.ecommerce.api.dto.ApiResponse.success(user));
    }

    @Operation(
        summary     = "Create user",
        description = "Creates a new user account. Email and username must be unique."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "Email or username already in use",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @PostMapping
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<UserEntity>> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User details", required = true)
            @Valid @RequestBody UserRequest request) {
        UserEntity created = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(org.ecommerce.api.dto.ApiResponse.success("User created successfully", created));
    }

    @Operation(
        summary     = "Update user",
        description = "Replaces all mutable fields of an existing user. "
                    + "Omitting the password field leaves it unchanged."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "Email or username already in use",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<UserEntity>> update(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable long id,
            @Valid @RequestBody UserRequest request) {
        UserEntity updated = userService.update(id, request);
        return ResponseEntity.ok(org.ecommerce.api.dto.ApiResponse.success("User updated successfully", updated));
    }

    @Operation(summary = "Delete user", description = "Permanently removes a user account.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found",
                     content = @Content(schema = @Schema(implementation = org.ecommerce.api.dto.ApiResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<org.ecommerce.api.dto.ApiResponse<Void>> delete(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable long id) {
        userService.delete(id);
        return ResponseEntity.ok(org.ecommerce.api.dto.ApiResponse.success("User deleted successfully", null));
    }
}
