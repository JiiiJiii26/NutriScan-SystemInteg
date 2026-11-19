package com.nutriscan.service;

import com.nutriscan.dto.RegisterRequest;
import com.nutriscan.dto.UserResponse;
import com.nutriscan.model.User;
import com.nutriscan.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service handling business logic for User accounts:
 * - Local registration with BCrypt hashing
 * - Google OAuth find-or-create user handling
 * - User lookup and DTO conversion
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Find a user by email address.
     */
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    /**
     * Register a new student account locally.
     * Enforces validation, uniqueness, and BCrypt password hashing.
     */
    @Transactional
    public User registerUser(RegisterRequest request) {
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Check for existing account with same email
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Hash password securely with BCrypt (never store plaintext)
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = new User(
                request.getFullName().trim(),
                normalizedEmail,
                hashedPassword,
                "STUDENT",
                "LOCAL"
        );

        return userRepository.save(newUser);
    }

    /**
     * Handle user record upon successful Google OAuth 2.0 login.
     * If user exists, retrieves user. If first-time login, creates student user.
     */
    @Transactional
    public User processOAuthPostLogin(String email, String name) {
        String normalizedEmail = email.trim().toLowerCase();
        String displayName = (name != null && !name.isBlank()) ? name.trim() : "Student User";

        return userRepository.findByEmail(normalizedEmail)
                .map(existingUser -> {
                    // Update name if currently empty
                    if (existingUser.getFullName() == null || existingUser.getFullName().isBlank()) {
                        existingUser.setFullName(displayName);
                        return userRepository.save(existingUser);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    // Create new student account with GOOGLE provider and null password
                    User newUser = new User(
                            displayName,
                            normalizedEmail,
                            null, // null password for Google OAuth accounts
                            "STUDENT",
                            "GOOGLE"
                    );
                    return userRepository.save(newUser);
                });
    }

    /**
     * Convert User entity to safe UserResponse DTO.
     */
    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getProvider()
        );
    }
}
