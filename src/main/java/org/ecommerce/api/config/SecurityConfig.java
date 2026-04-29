package org.ecommerce.api.config;

import org.ecommerce.api.security.CustomOAuth2UserService;
import org.ecommerce.api.security.JwtAccessDeniedHandler;
import org.ecommerce.api.security.JwtAuthEntryPoint;
import org.ecommerce.api.security.JwtAuthFilter;
import org.ecommerce.api.security.OAuth2AuthenticationFailureHandler;
import org.ecommerce.api.security.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // ── Beans ─────────────────────────────────────────────────────────────────

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService uds,
                                                         BCryptPasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ── CSRF demo filter chain (US 3.1) ───────────────────────────────────────
    // This secondary chain matches ONLY /csrf-demo/** and exists purely for
    // educational purposes. It enables CSRF + sessions to illustrate the token
    // handshake that traditional form-based apps must use.
    //
    // Why CSRF is irrelevant for the main JWT API:
    //   CSRF attacks rely on the browser automatically sending session cookies
    //   to a different origin's page. Because our main API uses Bearer tokens
    //   (Authorization header), which browsers never send automatically, there
    //   is nothing for an attacker to "forge". Disabling CSRF here is therefore
    //   correct and safe — it is NOT a security shortcut.
    //
    // When you MUST enable CSRF:
    //   - Cookie-based session authentication (Spring MVC + Thymeleaf forms)
    //   - Any endpoint whose state changes are triggered by browser form POSTs
    //   - Anywhere SameSite=Strict/Lax cookies are not sufficient by themselves
    @Bean
    @Order(1)
    public SecurityFilterChain csrfDemoFilterChain(HttpSecurity http) throws Exception {
        XorCsrfTokenRequestAttributeHandler requestHandler = new XorCsrfTokenRequestAttributeHandler();
        http
            .securityMatcher("/csrf-demo/**")
            .csrf(csrf -> csrf
                // CookieCsrfTokenRepository stores the token in an XSRF-TOKEN cookie.
                // withHttpOnlyFalse() lets JavaScript read it so SPAs can include it.
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // XorCsrfTokenRequestAttributeHandler masks the raw token on each
                // response (BREACH attack mitigation added in Spring Security 6).
                .csrfTokenRequestHandler(requestHandler)
            )
            // Sessions are required — the CSRF token is tied to the server session.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // ── Main JWT filter chain (US 1.1) ────────────────────────────────────────

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthFilter jwtAuthFilter,
                                                   AuthenticationProvider authProvider,
                                                   JwtAuthEntryPoint authEntryPoint,
                                                   JwtAccessDeniedHandler accessDeniedHandler,
                                                   CustomOAuth2UserService customOAuth2UserService,
                                                   OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler,
                                                   OAuth2AuthenticationFailureHandler oAuth2FailureHandler) throws Exception {
        http
            // CSRF disabled: this chain uses stateless Bearer tokens, not session cookies.
            // See csrfDemoFilterChain above for an explanation and live demonstration.
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authProvider)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .authorizeHttpRequests(auth -> auth

                // ── Public ────────────────────────────────────────────────────
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                .requestMatchers(
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/graphql/**", "/graphiql/**",
                    "/actuator/health", "/actuator/info"
                ).permitAll()
                // OAuth2 redirect endpoints must be public so the browser callback works
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                // ── Admin only ────────────────────────────────────────────────
                .requestMatchers("/api/monitoring/**").hasRole("ADMIN")
                .requestMatchers("/api/activity-logs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // ── Seller or Admin (product/category write + inventory) ───────
                .requestMatchers(HttpMethod.POST,   "/api/products/**", "/api/categories/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/products/**", "/api/categories/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**", "/api/categories/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers("/api/inventory/**").hasAnyRole("SELLER", "ADMIN")

                // ── Any authenticated user ─────────────────────────────────────
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(info -> info.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── CORS (US 1.2) ─────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow local dev clients and production domain; override via env in prod
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:[*]",
            "https://*.smartecommerce.ecommerce.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
