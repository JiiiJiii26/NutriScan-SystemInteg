package com.nutriscan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration.
 * 
 * Allows the React development server (http://localhost:5173) to communicate
 * with the Spring Boot backend, sending cookies (credentials) for session management.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow frontend origin (React Vite development server)
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));

        // Allow standard HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow all necessary headers
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));

        // Allow browser to send and receive session cookies (JSESSIONID)
        configuration.setAllowCredentials(true);

        // Pre-flight cache duration in seconds (1 hour)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
