package org.ecommerce.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.ecommerce.api.dto.ApiResponse;
import org.ecommerce.api.dto.request.LoginRequest;
import org.ecommerce.api.dto.request.RegisterRequest;
import org.ecommerce.api.dto.response.AuthResponse;
import org.ecommerce.api.dto.response.TokenInfoResponse;
import org.ecommerce.api.security.JwtService;
import org.ecommerce.api.security.TokenBlacklistService;
import org.ecommerce.api.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register, log in, and inspect JWT claims")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(AuthService authService,
                          JwtService jwtService,
                          TokenBlacklistService tokenBlacklistService) {
        this.authService           = authService;
        this.jwtService            = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new customer account and receive a JWT")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("Registration successful", authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with username or email — returns a signed JWT")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }

    /**
     * Adds the caller's token to the in-memory blacklist so it is rejected
     * on all subsequent requests, even before natural expiry (US 5.1).
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Invalidate the current JWT (US 5.1 — token revocation)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            tokenBlacklistService.revoke(token, jwtService.extractExpiry(token));
        }
        return ApiResponse.success("Logged out successfully", null);
    }

    /**
     * Decodes the caller's own JWT and returns all claims in plain JSON.
     * Satisfies US 2.2: token payload can be viewed and verified in Postman.
     */
    @GetMapping("/me")
    @Operation(
        summary = "Decode and return claims from the current JWT (US 2.2 verification)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ApiResponse<TokenInfoResponse> me(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "No Bearer token present in Authorization header");
        }

        Claims claims = jwtService.extractAllClaims(header.substring(7));

        long remaining = Math.max(0,
                (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000);

        TokenInfoResponse info = new TokenInfoResponse(
                claims.getSubject(),
                claims.get("userId", Long.class),
                claims.get("email",  String.class),
                claims.get("role",   String.class),
                "HS256",
                claims.getIssuedAt(),
                claims.getExpiration(),
                remaining
        );

        return ApiResponse.success("Token is valid", info);
    }
}
