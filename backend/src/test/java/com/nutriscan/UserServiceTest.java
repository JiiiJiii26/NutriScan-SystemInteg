package com.nutriscan;

import com.nutriscan.dto.RegisterRequest;
import com.nutriscan.model.User;
import com.nutriscan.repository.UserRepository;
import com.nutriscan.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

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
        // Pre-create user
        userService.processOAuthPostLogin("existing.google@gmail.com", "Existing Google Student");

        // Call again
        User existing = userService.processOAuthPostLogin("existing.google@gmail.com", "Existing Google Student");

        assertNotNull(existing.getId());
        assertEquals("existing.google@gmail.com", existing.getEmail());
        assertEquals(1, userRepository.count());
    }
}
