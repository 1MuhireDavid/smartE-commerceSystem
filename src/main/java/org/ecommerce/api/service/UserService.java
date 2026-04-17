package org.ecommerce.api.service;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.UserRequest;
import org.ecommerce.api.entity.UserEntity;
import org.springframework.data.domain.Pageable;

public interface UserService {

    PagedResponse<UserEntity> findAll(String keyword, String role, Boolean active, Pageable pageable);

    UserEntity findById(long id);

    UserEntity create(UserRequest request);

    UserEntity update(long id, UserRequest request);

    void delete(long id);
}
