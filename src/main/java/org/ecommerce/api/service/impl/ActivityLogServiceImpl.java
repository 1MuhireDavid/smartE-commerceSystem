package org.ecommerce.api.service.impl;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.ActivityLogRequest;
import org.ecommerce.api.entity.ActivityLogEntity;
import org.ecommerce.api.repository.ActivityLogRepository;
import org.ecommerce.api.repository.UserRepository;
import org.ecommerce.api.service.ActivityLogService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository        userRepository;

    public ActivityLogServiceImpl(ActivityLogRepository activityLogRepository,
                                  UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository        = userRepository;
    }

    @Override
    public PagedResponse<ActivityLogEntity> findAll(Long userId, String eventType, Pageable pageable) {
        return PagedResponse.of(activityLogRepository.search(userId, eventType, pageable));
    }

    @Override
    @Transactional
    public ActivityLogEntity create(ActivityLogRequest request) {
        ActivityLogEntity log = new ActivityLogEntity();
        log.setEventType(request.getEventType());
        log.setEventData(request.getEventData());

        if (request.getUserId() != null) {
            log.setUser(userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "User not found with id: " + request.getUserId())));
        }

        return activityLogRepository.save(log);
    }
}
