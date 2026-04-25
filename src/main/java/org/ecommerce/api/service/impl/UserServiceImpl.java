package org.ecommerce.api.service.impl;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.UserRequest;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.repository.UserRepository;
import org.ecommerce.api.service.UserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public PagedResponse<UserEntity> findAll(
            String keyword, String role, Boolean active, Pageable pageable) {
        String pattern = keyword != null ? ("%" + keyword.toLowerCase() + "%") : null;
        Page<UserEntity> page = userRepository.search(pattern, role, active, pageable);
        return PagedResponse.of(page);
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserEntity findById(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserEntity create(UserRequest request) {
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
        user.setPasswordHash(request.getPassword());   // hashing handled externally / future auth layer
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole() != null ? request.getRole() : "customer");
        user.setActive(request.isActive());

        return userRepository.save(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserEntity update(long id, UserRequest request) {
        UserEntity user = findById(id);

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email already in use: " + request.getEmail());
        }
        if (!user.getUsername().equals(request.getUsername())
                && userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Username already taken: " + request.getUsername());
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(request.getPassword());
        }
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setActive(request.isActive());

        return userRepository.save(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void delete(long id) {
        UserEntity user = findById(id);
        userRepository.delete(user);
    }
}
