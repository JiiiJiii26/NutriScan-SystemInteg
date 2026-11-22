package com.nutriscan;

import com.nutriscan.dto.*;
import com.nutriscan.model.PasswordResetToken;
import com.nutriscan.model.User;
import com.nutriscan.repository.PasswordResetTokenRepository;
import com.nutriscan.repository.UserRepository;
import com.nutriscan.service.PasswordResetService;
import com.nutriscan.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================
    // Increment 1 Tests
    // =========================================================

    @Test
    void testRegisterUser_Success_HashesPasswordWithBCrypt() {
        RegisterRequest request = new RegisterRequest(
                "Maria Santos",
                "maria@university.edu.ph",
                "Password123!",
                "Password123!"
        );

        User savedUser = userService.registerUser(request);

        assertNotNull(savedUser.getId());
        assertEquals("Maria Santos", savedUser.getFullName());
        assertEquals("maria@university.edu.ph", savedUser.getEmail());
        assertEquals("STUDENT", savedUser.getRole());
        assertEquals("LOCAL", savedUser.getProvider());

        // CRITICAL: Prove password is BCrypt hashed, NOT plaintext
        assertNotEquals("Password123!", savedUser.getPasswordHash());
        assertTrue(savedUser.getPasswordHash().startsWith("$2a$") || savedUser.getPasswordHash().startsWith("$2b$"));
        assertTrue(passwordEncoder.matches("Password123!", savedUser.getPasswordHash()));
    }

    @Test
    void testRegisterUser_PasswordMismatch_ThrowsException() {
        RegisterRequest request = new RegisterRequest(
                "Juan Dela Cruz",
                "juan@university.edu.ph",
                "Password123!",
                "DifferentPassword!"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(request);
        });

        assertEquals("Passwords do not match", ex.getMessage());
    }

    @Test
    void testRegisterUser_DuplicateEmail_ThrowsException() {
        RegisterRequest first = new RegisterRequest(
                "Student One",
                "duplicate@university.edu.ph",
                "Password123!",
                "Password123!"
        );
        userService.registerUser(first);

        RegisterRequest duplicate = new RegisterRequest(
                "Student Two",
                "duplicate@university.edu.ph",
                "AnotherPassword123!",
                "AnotherPassword123!"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(duplicate);
        });

        assertEquals("Email is already registered", ex.getMessage());
    }

    @Test
    void testProcessOAuthPostLogin_CreatesStudentWhenNew() {
        User user = userService.processOAuthPostLogin("google.student@gmail.com", "Google Student");

        assertNotNull(user.getId());
        assertEquals("google.student@gmail.com", user.getEmail());
        assertEquals("Google Student", user.getFullName());
        assertEquals("STUDENT", user.getRole());
        assertEquals("GOOGLE", user.getProvider());
        assertNull(user.getPasswordHash(), "Google OAuth user should have null password_hash");
    }

    @Test
    void testProcessOAuthPostLogin_ReturnsExistingWhenAlreadyInDb() {
        userService.processOAuthPostLogin("existing.google@gmail.com", "Existing Google Student");
        User existing = userService.processOAuthPostLogin("existing.google@gmail.com", "Existing Google Student");

        assertNotNull(existing.getId());
        assertEquals("existing.google@gmail.com", existing.getEmail());
        assertEquals(1, userRepository.count());
    }

    // =========================================================
    // Increment 2 Tests: Profile Management
    // =========================================================

    @Test
    void testUpdateProfile_Success_UpdatesFullNameAndEmail() {
        User user = userService.registerUser(new RegisterRequest("Original Name", "original@university.edu.ph", "Password123!", "Password123!"));

        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", "updated@university.edu.ph");
        User updated = userService.updateProfile(user.getEmail(), request);

        assertEquals("Updated Name", updated.getFullName());
        assertEquals("updated@university.edu.ph", updated.getEmail());
    }

    @Test
    void testUpdateProfile_GoogleUser_CanUpdateFullName() {
        User googleUser = userService.processOAuthPostLogin("google@gmail.com", "Google Original");

        UpdateProfileRequest request = new UpdateProfileRequest("Google Renamed", "google@gmail.com");
        User updated = userService.updateProfile(googleUser.getEmail(), request);

        assertEquals("Google Renamed", updated.getFullName());
        assertEquals("google@gmail.com", updated.getEmail());
    }

    @Test
    void testUpdateProfile_GoogleUser_CannotChangeEmail_ThrowsException() {
        User googleUser = userService.processOAuthPostLogin("google@gmail.com", "Google Original");

        UpdateProfileRequest request = new UpdateProfileRequest("Google Renamed", "attempted.new@gmail.com");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateProfile(googleUser.getEmail(), request);
        });

        assertEquals("Email cannot be changed for Google accounts", ex.getMessage());
    }

    @Test
    void testUpdateProfile_DuplicateEmail_ThrowsException() {
        userService.registerUser(new RegisterRequest("Student A", "studentA@university.edu.ph", "Password123!", "Password123!"));
        User userB = userService.registerUser(new RegisterRequest("Student B", "studentB@university.edu.ph", "Password123!", "Password123!"));

        UpdateProfileRequest request = new UpdateProfileRequest("Student B Updated", "studentA@university.edu.ph");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.updateProfile(userB.getEmail(), request);
        });

        assertEquals("Email is already registered", ex.getMessage());
    }

    // =========================================================
    // Increment 2 Tests: Change Password
    // =========================================================

    @Test
    void testChangePassword_Success_HashesNewPasswordAndFailsOld() {
        User user = userService.registerUser(new RegisterRequest("Maria Santos", "maria@university.edu.ph", "OldPassword123!", "OldPassword123!"));

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "NewPassword456!", "NewPassword456!");
        userService.changePassword(user.getEmail(), request);

        User refreshed = userRepository.findByEmail(user.getEmail()).orElseThrow();
        // New password must match
        assertTrue(passwordEncoder.matches("NewPassword456!", refreshed.getPasswordHash()));
        // Old password must fail
        assertFalse(passwordEncoder.matches("OldPassword123!", refreshed.getPasswordHash()));
    }

    @Test
    void testChangePassword_WrongCurrentPassword_ThrowsException() {
        User user = userService.registerUser(new RegisterRequest("Maria Santos", "maria@university.edu.ph", "OldPassword123!", "OldPassword123!"));

        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword!", "NewPassword456!", "NewPassword456!");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword(user.getEmail(), request);
        });

        assertEquals("Current password is incorrect", ex.getMessage());
    }

    @Test
    void testChangePassword_SamePassword_ThrowsException() {
        User user = userService.registerUser(new RegisterRequest("Maria Santos", "maria@university.edu.ph", "OldPassword123!", "OldPassword123!"));

        ChangePasswordRequest request = new ChangePasswordRequest("OldPassword123!", "OldPassword123!", "OldPassword123!");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword(user.getEmail(), request);
        });

        assertEquals("New password cannot be the same as current password", ex.getMessage());
    }

    @Test
    void testChangePassword_GoogleUser_ThrowsException() {
        User googleUser = userService.processOAuthPostLogin("google@gmail.com", "Google Student");

        ChangePasswordRequest request = new ChangePasswordRequest("AnyPassword!", "NewPassword456!", "NewPassword456!");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword(googleUser.getEmail(), request);
        });

        assertEquals("Password change is not available for Google accounts.", ex.getMessage());
    }

    // =========================================================
    // Increment 2 Tests: Forgot & Reset Password
    // =========================================================

    @Test
    void testForgotPassword_GeneratesTokenForLocalUser() {
        User user = userService.registerUser(new RegisterRequest("Maria Santos", "maria@university.edu.ph", "Password123!", "Password123!"));

        userService.processForgotPassword(user.getEmail());

        assertEquals(1, tokenRepository.count());
    }

    @Test
    void testForgotPassword_SilentlyIgnoresGoogleUser() {
        User googleUser = userService.processOAuthPostLogin("google@gmail.com", "Google Student");

        userService.processForgotPassword(googleUser.getEmail());

        assertEquals(0, tokenRepository.count());
    }

    @Test
    void testResetPassword_Success_UpdatesPasswordAndInvalidatesToken() {
        User user = userService.registerUser(new RegisterRequest("Maria Santos", "maria@university.edu.ph", "OriginalPass123!", "OriginalPass123!"));
        String token = passwordResetService.createResetToken(user);

        ResetPasswordRequest request = new ResetPasswordRequest(token, "BrandNewPass999!", "BrandNewPass999!");
        userService.resetPassword(request);

        User updatedUser = userRepository.findByEmail(user.getEmail()).orElseThrow();
        assertTrue(passwordEncoder.matches("BrandNewPass999!", updatedUser.getPasswordHash()));
        assertFalse(passwordEncoder.matches("OriginalPass123!", updatedUser.getPasswordHash()));

        // Token must be marked as used
        PasswordResetToken storedToken = tokenRepository.findByToken(token).orElseThrow();
        assertTrue(storedToken.isUsed());
    }

    @Test
    void testResetPassword_AlreadyUsedToken_ThrowsException() {
        User user = userService.registerUser(new RegisterRequest("Maria Santos", "maria@university.edu.ph", "OriginalPass123!", "OriginalPass123!"));
        String token = passwordResetService.createResetToken(user);

        // Consume once
        userService.resetPassword(new ResetPasswordRequest(token, "BrandNewPass999!", "BrandNewPass999!"));

        // Try again with same token
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.resetPassword(new ResetPasswordRequest(token, "AnotherPass888!", "AnotherPass888!"));
        });

        assertEquals("Reset token has already been used", ex.getMessage());
    }

    @Test
    void testResetPassword_ExpiredToken_ThrowsException() {
        User user = userService.registerUser(new RegisterRequest("Maria Santos", "maria@university.edu.ph", "OriginalPass123!", "OriginalPass123!"));

        // Create token with expired timestamp
        PasswordResetToken expiredToken = new PasswordResetToken(user, "expired-token-123", LocalDateTime.now().minusMinutes(5));
        tokenRepository.save(expiredToken);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.resetPassword(new ResetPasswordRequest("expired-token-123", "BrandNewPass999!", "BrandNewPass999!"));
        });

        assertEquals("Reset token has expired", ex.getMessage());
    }
}
