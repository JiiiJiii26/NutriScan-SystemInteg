package com.nutriscan.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Spring Security configuration for NutriScan (Increments 1 & 2):
 * - Session-based authentication with JSESSIONID
 * - BCrypt password hashing
 * - CORS configuration for React frontend (localhost:5173)
 * - Public endpoints: registration, local login, forgot-password, reset-password
 * - Protected endpoints: /api/me, /api/profile/**, /api/auth/change-password
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 1. Tells Spring to use standard HTTP Sessions (JSESSIONID)
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    // 2. Configures the security filter chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {}) // Uses CorsConfig
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized: Authentication required\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Authenticated password change requires valid session
                .requestMatchers("/api/auth/change-password").authenticated()
                // Forgot & Reset password are open to public
                .requestMatchers("/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                // Other auth endpoints (login, register, logout)
                .requestMatchers("/api/auth/**").permitAll()
                // Student Profile and Me endpoints require authentication
                .requestMatchers("/api/profile/**").authenticated()
                .requestMatchers("/api/me").authenticated()
                // Other endpoints (root status, OAuth2 callbacks, static resources)
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );

        return http.build();
    }

    // 3. AuthenticationManager bean for AuthController
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
