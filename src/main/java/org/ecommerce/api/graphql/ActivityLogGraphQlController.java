package org.ecommerce.api.graphql;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.entity.ActivityLogEntity;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.graphql.input.ActivityLogFilter;
import org.ecommerce.api.graphql.input.ActivityLogInput;
import org.ecommerce.api.repository.UserRepository;
import org.ecommerce.api.service.ActivityLogService;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL resolver for ActivityLog queries, mutations, and nested field resolvers.
 *
 * <p>Because OSIV is disabled, the {@code ActivityLog.user} resolver loads the
 * UserEntity explicitly. userId may be null for anonymous activity logs.
 */
@Controller
@Transactional(readOnly = true)
public class ActivityLogGraphQlController {

    private final ActivityLogService activityLogService;
    private final UserRepository     userRepository;

    public ActivityLogGraphQlController(ActivityLogService activityLogService,
                                        UserRepository userRepository) {
        this.activityLogService = activityLogService;
        this.userRepository     = userRepository;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public PagedResponse<ActivityLogEntity> activityLogs(
            @Argument ActivityLogFilter filter,
            @Argument int page,
            @Argument int size) {

        Long   userId    = filter != null ? filter.getUserId()    : null;
        String eventType = filter != null ? filter.getEventType() : null;

        return activityLogService.findAll(userId, eventType, PageRequest.of(page, size));
    }

    // ── Nested field resolvers ─────────────────────────────────────────────────

    @SchemaMapping(typeName = "ActivityLog", field = "user")
    public UserEntity user(ActivityLogEntity log) {
        Long uid = log.getUserId();
        return uid != null ? userRepository.findById(uid).orElse(null) : null;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    @Transactional
    public ActivityLogEntity createActivityLog(@Argument ActivityLogInput input) {
        return activityLogService.create(input.toRequest());
    }
}
