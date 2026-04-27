package org.ecommerce.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.ecommerce.api.dto.ApiResponse;
import org.ecommerce.api.dto.request.LoginRequest;
import org.ecommerce.api.dto.request.RegisterRequest;
import org.ecommerce.api.dto.response.AuthResponse;
import org.ecommerce.api.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register and log in to obtain a JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new customer account")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse body = authService.register(request);
        return ApiResponse.success("Registration successful", body);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a JWT (accepts username or email)")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse body = authService.login(request);
        return ApiResponse.success("Login successful", body);
    }
}
