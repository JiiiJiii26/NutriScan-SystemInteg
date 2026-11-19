package com.nutriscan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * NutriScan Spring Boot Main Application.
 * 
 * Increment 1: User Accounts and Authentication
 * - Student local registration & login
 * - Google OAuth 2.0 integration
 * - PostgreSQL user storage & BCrypt password hashing
 */
@SpringBootApplication
public class NutriScanApplication {

    public static void main(String[] args) {
        SpringApplication.run(NutriScanApplication.class, args);
    }

    /**
     * Password encoder bean using standard BCrypt algorithm.
     * Placed here to prevent circular dependencies between SecurityConfig and UserService.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
