package com.nutriscan.controller;

import com.nutriscan.dto.LoginRequest;
import com.nutriscan.dto.RegisterRequest;
import com.nutriscan.dto.UserResponse;
import com.nutriscan.model.User;
import com.nutriscan.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller handling user authentication:
 * - Local student registration
 * - Local email/password login with HTTP session
 * - Logout
 * - Current user session check (/api/me)
 */
@RestController
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UserService userService,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * POST /api/auth/register
     * Register a new student account locally.
     */
    @PostMapping("/api/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, BindingResult bindingResult) {
        // Handle validation errors from DTO annotations
        if (bindingResult.hasErrors()) {
            String firstError = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of("error", firstError));
        }

        try {
            User registeredUser = userService.registerUser(request);
            UserResponse response = userService.toUserResponse(registeredUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already registered"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Registration failed. Please try again."));
        }
    }

    /**
     * POST /api/auth/login
     * Authenticate student credentials, establish a session, and set JSESSIONID cookie.
     */
    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                  BindingResult bindingResult,
                                  HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        if (bindingResult.hasErrors()) {
            String firstError = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of("error", firstError));
        }

        try {
            String normalizedEmail = request.getEmail().trim().toLowerCase();

            // Perform Spring Security authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );

            // Create security context and save it to HTTP session
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            // Fetch user profile from DB
            User user = userService.findByEmail(normalizedEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(userService.toUserResponse(user));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred during login"));
        }
    }

    /**
     * POST /api/auth/logout
     * Invalidate current session and clear security context.
     */
    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                   HttpServletResponse response,
                                   Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /**
     * GET /api/me
     * Returns details of the currently authenticated student.
     * Enforced by Spring Security to require ROLE_STUDENT. Returns 401 if unauthenticated.
     */
    @GetMapping("/api/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: No active session found"));
        }

        // Determine user email based on authentication type (Local vs Google OAuth)
        String email;
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
        } else {
            email = authentication.getName();
        }

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No email associated with session"));
        }

        java.util.Optional<User> optionalUser = userService.findByEmail(email.trim().toLowerCase());
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found in database"));
        }

        return ResponseEntity.ok(userService.toUserResponse(optionalUser.get()));
    }
}
