package org.ecommerce.api.service.impl;

import org.ecommerce.api.dto.request.LoginRequest;
import org.ecommerce.api.dto.request.RegisterRequest;
import org.ecommerce.api.dto.response.AuthResponse;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.repository.UserRepository;
import org.ecommerce.api.security.JwtService;
import org.ecommerce.api.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager) {
        this.userRepository      = userRepository;
        this.passwordEncoder     = passwordEncoder;
        this.jwtService          = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already in use: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username already taken: " + request.getUsername());
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole("customer");
        user.setActive(true);

        user = userRepository.save(user);
        return AuthResponse.from(user, jwtService.generateToken(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(), request.getPassword()));
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid username/email or password");
        }

        UserEntity user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return AuthResponse.from(user, jwtService.generateToken(user));
    }
}
