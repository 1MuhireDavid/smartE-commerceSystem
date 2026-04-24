package org.ecommerce.api.service;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.ActivityLogRequest;
import org.ecommerce.api.entity.ActivityLogEntity;
import org.springframework.data.domain.Pageable;

public interface ActivityLogService {

    PagedResponse<ActivityLogEntity> findAll(Long userId, String eventType, Pageable pageable);

    ActivityLogEntity create(ActivityLogRequest request);
}
