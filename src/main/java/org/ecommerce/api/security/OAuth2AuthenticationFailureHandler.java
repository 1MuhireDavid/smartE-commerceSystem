package org.ecommerce.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ecommerce.api.dto.ApiResponse;
import org.ecommerce.api.dto.request.ActivityLogRequest;
import org.ecommerce.api.service.ActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Called by Spring Security when OAuth2 authentication fails (e.g. the user
 * denies consent, or the Google token exchange fails).
 * Returns a JSON 401 instead of the default redirect to /login?error.
 */
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    private final ObjectMapper objectMapper;
    private final ActivityLogService activityLogService;

    public OAuth2AuthenticationFailureHandler(ObjectMapper objectMapper,
                                              ActivityLogService activityLogService) {
        this.objectMapper       = objectMapper;
        this.activityLogService = activityLogService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        audit(exception.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
            response.getWriter(),
            ApiResponse.error("OAuth2 authentication failed: " + exception.getMessage())
        );
    }

    private void audit(String reason) {
        try {
            ActivityLogRequest req = new ActivityLogRequest();
            req.setUserId(null);
            req.setEventType("oauth2_login_failure");
            req.setEventData("{\"provider\":\"google\",\"reason\":\"" + reason + "\"}");
            activityLogService.create(req);
        } catch (Exception ex) {
            log.warn("Audit log failed for oauth2_login_failure: {}", ex.getMessage());
        }
    }
}
