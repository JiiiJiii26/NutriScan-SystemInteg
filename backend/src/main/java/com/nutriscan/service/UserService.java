package com.nutriscan.service;

import com.nutriscan.dto.*;
import com.nutriscan.model.PasswordResetToken;
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
 * - Profile updates (Increment 2)
 * - Password change and reset flows (Increment 2)
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
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
     * Update user profile (Full Name and optionally Email).
     * Rejects email changes for Google OAuth accounts.
     * Never modifies passwordHash, role, provider, or createdAt.
     */
    @Transactional
    public User updateProfile(String currentEmail, UpdateProfileRequest request) {
        User user = findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newEmail = request.getEmail().trim().toLowerCase();
        String newFullName = request.getFullName().trim();

        // Check if provider is GOOGLE: Cannot change email
        if ("GOOGLE".equalsIgnoreCase(user.getProvider())) {
            if (!user.getEmail().equalsIgnoreCase(newEmail)) {
                throw new IllegalArgumentException("Email cannot be changed for Google accounts");
            }
        } else {
            // If local user is changing email, ensure new email is not already taken by another account
            if (!user.getEmail().equalsIgnoreCase(newEmail)) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new IllegalArgumentException("Email is already registered");
                }
                user.setEmail(newEmail);
            }
        }

        user.setFullName(newFullName);
        return userRepository.save(user);
    }

    /**
     * Authenticated change password.
     * Validates current password, checks mismatch, hashes new password with BCrypt.
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if ("GOOGLE".equalsIgnoreCase(user.getProvider()) || user.getPasswordHash() == null) {
            throw new IllegalArgumentException("Password change is not available for Google accounts.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }

        // Verify current password matches stored BCrypt hash
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Ensure new password is not the same as the current password
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as current password");
        }

        // Hash and persist new password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Forgot password initiation.
     * Creates reset token and logs link only if account is LOCAL and exists.
     * Silently ignores Google accounts and non-existent accounts to prevent enumeration.
     */
    @Transactional
    public void processForgotPassword(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        Optional<User> optionalUser = findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if ("LOCAL".equalsIgnoreCase(user.getProvider())) {
                passwordResetService.createResetToken(user);
            }
        }
    }

    /**
     * Complete password reset using token.
     * Hashes new password, updates user, marks token as used.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }

        PasswordResetToken resetToken = passwordResetService.validateToken(request.getToken());
        User user = resetToken.getUser();

        if ("GOOGLE".equalsIgnoreCase(user.getProvider())) {
            throw new IllegalArgumentException("Password reset is not available for Google accounts");
        }

        // Update password with BCrypt
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate token
        passwordResetService.consumeToken(resetToken);
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
                user.getProvider(),
                user.getCreatedAt()
        );
    }
}
