package org.ecommerce.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ecommerce.api.dto.ApiResponse;
import org.ecommerce.api.dto.request.ActivityLogRequest;
import org.ecommerce.api.dto.response.AuthResponse;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.service.ActivityLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Called by Spring Security after a successful OAuth2 authentication.
 * Generates a JWT for the authenticated user and writes it as a JSON
 * response body (HTTP 200) so API clients and Postman can read it directly.
 *
 * The UserEntity was attached to the OAuth2User attributes map by
 * CustomOAuth2UserService under the key "userEntity", so no additional
 * DB lookup is needed here.
 */
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final ActivityLogService activityLogService;

    public OAuth2AuthenticationSuccessHandler(JwtService jwtService,
                                              ObjectMapper objectMapper,
                                              ActivityLogService activityLogService) {
        this.jwtService          = jwtService;
        this.objectMapper        = objectMapper;
        this.activityLogService  = activityLogService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        UserEntity user = (UserEntity) oAuth2User.getAttributes().get("userEntity");

        String token = jwtService.generateToken(user);
        AuthResponse authResponse = AuthResponse.from(user, token, jwtService.getExpiration());

        audit(user.getUserId(), user.getEmail());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
            response.getWriter(),
            ApiResponse.success("Google login successful", authResponse)
        );
    }

    private void audit(Long userId, String email) {
        try {
            ActivityLogRequest req = new ActivityLogRequest();
            req.setUserId(userId);
            req.setEventType("oauth2_login_success");
            req.setEventData("{\"email\":\"" + email + "\",\"provider\":\"google\"}");
            activityLogService.create(req);
        } catch (Exception ex) {
            log.warn("Audit log failed for oauth2_login_success: {}", ex.getMessage());
        }
    }
}
