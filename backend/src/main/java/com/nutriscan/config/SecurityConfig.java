package com.nutriscan.config;

import com.nutriscan.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.HashSet;
import java.util.Set;

/**
 * Spring Security Configuration for NutriScan.
 * 
 * Configures:
 * 1. BCrypt password hashing for secure local credentials
 * 2. Session-based authentication with JSESSIONID cookies
 * 3. Google OAuth 2.0 login with automatic student registration
 * 4. Role-based access control (ROLE_STUDENT)
 * 5. CORS integration for React Vite frontend
 * 6. Clean JSON 401 responses for unauthenticated API requests
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Authentication manager bean for programmatic authentication in AuthController.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Security context repository to persist authentication into HTTP session.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Authority mapper for Google OAuth users to guarantee they have ROLE_STUDENT.
     */
    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<SimpleGrantedAuthority> mappedAuthorities = new HashSet<>();
            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
            return mappedAuthorities;
        };
    }

    /**
     * Main security filter chain defining endpoint protections.
     * Injects UserService directly into method to prevent circular reference.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserService userService) throws Exception {
        http
            // Enable CORS using CorsConfig
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // Disable CSRF for REST API / session-cookie workflow
            .csrf(csrf -> csrf.disable())

            // Configure Security Context to use HTTP Session
            .securityContext(context -> context.securityContextRepository(securityContextRepository()))

            // URL Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/",
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/logout",
                    "/oauth2/**",
                    "/login/**",
                    "/error"
                ).permitAll()

                // Protected student endpoint with Role-Based Access Control
                .requestMatchers("/api/me").hasRole("STUDENT")

                // Any other request must be authenticated
                .anyRequest().authenticated()
            )

            // Custom Exception Handling: Return clean JSON 401 instead of redirecting to login page
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\": \"Unauthorized: Please log in to access this resource\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\": \"Forbidden: Insufficient role permissions\"}");
                })
            )

            // Google OAuth 2.0 Login Configuration
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userAuthoritiesMapper(userAuthoritiesMapper())
                )
                // Success Handler: Find or create user in DB, then redirect to React frontend dashboard
                .successHandler((request, response, authentication) -> {
                    if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                        String email = oauth2User.getAttribute("email");
                        String name = oauth2User.getAttribute("name");
                        if (email != null) {
                            userService.processOAuthPostLogin(email, name);
                        }
                    }
                    response.sendRedirect("http://localhost:5173/dashboard");
                })
            )

            // Logout Configuration
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"message\": \"Logged out successfully\"}");
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}
