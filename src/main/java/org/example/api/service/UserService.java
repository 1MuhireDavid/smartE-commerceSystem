package org.example.api.service;

import org.example.api.dto.PagedResponse;
import org.example.api.dto.request.UserRequest;
import org.example.api.entity.UserEntity;
import org.springframework.data.domain.Pageable;

public interface UserService {

    PagedResponse<UserEntity> findAll(String keyword, String role, Boolean active, Pageable pageable);

    UserEntity findById(long id);

    UserEntity create(UserRequest request);

    UserEntity update(long id, UserRequest request);

    void delete(long id);
}
