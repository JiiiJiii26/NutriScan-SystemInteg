package com.nutriscan.controller;

import com.nutriscan.dto.UpdateProfileRequest;
import com.nutriscan.dto.UserResponse;
import com.nutriscan.model.User;
import com.nutriscan.service.UserService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller handling student profile management (Increment 2):
 * - GET /api/profile : Fetch current student's profile
 * - PUT /api/profile : Update full name and email (enforcing ID-injection protection)
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Helper to extract current authenticated user's email.
     */
    private String getAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return oauth2User.getAttribute("email");
        }
        return authentication.getName();
    }

    /**
     * GET /api/profile
     * Fetch current user's profile. Requires authentication.
     */
    @GetMapping
    public ResponseEntity<?> getProfile(Authentication authentication) {
        String email = getAuthenticatedEmail(authentication);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: Please log in to view profile"));
        }

        return userService.findByEmail(email)
                .map(user -> ResponseEntity.ok(userService.toUserResponse(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body((UserResponse) null));
    }

    /**
     * PUT /api/profile
     * Update full name and email for the currently authenticated student.
     * Prevents ID injection by binding strictly to the SecurityContext principal.
     */
    @PutMapping
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                          BindingResult bindingResult,
                                          Authentication authentication) {
        String email = getAuthenticatedEmail(authentication);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized: Please log in to update profile"));
        }

        if (bindingResult.hasErrors()) {
            String firstError = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(Map.of("error", firstError));
        }

        try {
            User updatedUser = userService.updateProfile(email, request);
            return ResponseEntity.ok(userService.toUserResponse(updatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already registered"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update profile. Please try again."));
        }
    }
}
