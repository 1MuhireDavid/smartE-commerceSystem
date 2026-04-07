package org.example.api.graphql;

import org.example.api.dto.PagedResponse;
import org.example.api.entity.UserEntity;
import org.example.api.graphql.input.UserFilter;
import org.example.api.graphql.input.UserInput;
import org.example.api.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL resolver for User queries and mutations.
 *
 * <p>Maps to the {@code Query} and {@code Mutation} roots defined in schema.graphqls.
 * Reuses the REST service layer — business logic and validation stay in one place.
 */
@Controller
@Transactional(readOnly = true)
public class UserGraphQlController {

    private final UserService userService;

    public UserGraphQlController(UserService userService) {
        this.userService = userService;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public UserEntity user(@Argument Long id) {
        return userService.findById(id);
    }

    @QueryMapping
    public PagedResponse<UserEntity> users(
            @Argument UserFilter filter,
            @Argument int page,
            @Argument int size,
            @Argument String sortBy,
            @Argument String sortDir) {

        String  keyword = filter != null ? filter.getKeyword() : null;
        String  role    = filter != null ? filter.getRole()    : null;
        Boolean active  = filter != null ? filter.getActive()  : null;

        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return userService.findAll(keyword, role, active, PageRequest.of(page, size, sort));
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    @Transactional
    public UserEntity createUser(@Argument UserInput input) {
        return userService.create(input.toRequest());
    }

    @MutationMapping
    @Transactional
    public UserEntity updateUser(@Argument Long id, @Argument UserInput input) {
        return userService.update(id, input.toRequest());
    }

    @MutationMapping
    @Transactional
    public boolean deleteUser(@Argument Long id) {
        userService.delete(id);
        return true;
    }
}
