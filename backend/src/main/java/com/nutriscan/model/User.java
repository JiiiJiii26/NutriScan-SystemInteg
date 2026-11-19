package com.nutriscan.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * User Entity representing a student account in NutriScan.
 * 
 * Maps directly to the 'users' table in PostgreSQL.
 * Supports both local email/password registration and Google OAuth 2.0 logins.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Full name of the student
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    // Unique student email address
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    // BCrypt-hashed password (NULL for users who register via Google OAuth)
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    // Role-based access control (default 'STUDENT' for Increment 1)
    @Column(name = "role", nullable = false, length = 20)
    private String role = "STUDENT";

    // Authentication provider: 'LOCAL' or 'GOOGLE'
    @Column(name = "provider", nullable = false, length = 20)
    private String provider = "LOCAL";

    // Timestamp when the account was created
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Default constructor required by JPA
    public User() {
    }

    // Convenience constructor for creating a new user
    public User(String fullName, String email, String passwordHash, String role, String provider) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = (role != null) ? role : "STUDENT";
        this.provider = (provider != null) ? provider : "LOCAL";
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
