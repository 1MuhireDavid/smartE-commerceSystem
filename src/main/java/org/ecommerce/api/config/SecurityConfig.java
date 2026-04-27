package org.ecommerce.api.config;

import org.ecommerce.api.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    // ── Filter chain (US 1.1) ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthFilter jwtAuthFilter,
                                                   AuthenticationProvider authProvider) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authProvider)
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
