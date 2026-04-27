package org.ecommerce.api.service;

import org.ecommerce.api.dto.request.LoginRequest;
import org.ecommerce.api.dto.request.RegisterRequest;
import org.ecommerce.api.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
