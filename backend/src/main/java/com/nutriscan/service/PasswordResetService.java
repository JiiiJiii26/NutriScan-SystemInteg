package com.nutriscan.service;

import com.nutriscan.model.PasswordResetToken;
import com.nutriscan.model.User;
import com.nutriscan.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Service managing password reset tokens:
 * - Secure token generation
 * - Expiration and single-use verification
 * - Console link logging for local testing/demo
 */
@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(PasswordResetTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Create and store a new password reset token valid for 15 minutes.
     * Logs the reset link to the console for testing.
     */
    @Transactional
    public String createResetToken(User user) {
        // Generate secure 32-byte cryptographically strong token string (43+ chars Base64URL)
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        PasswordResetToken resetToken = new PasswordResetToken(user, token, expiresAt);
        tokenRepository.save(resetToken);

        // Required console output for instructor & developer testing
        String resetUrl = "http://localhost:5173/reset-password?token=" + token;
        System.out.println("=================================================================================");
        System.out.println("[PASSWORD RESET] Link for " + user.getEmail() + ": " + resetUrl);
        System.out.println("=================================================================================");

        return token;
    }

    /**
     * Validate reset token: ensures token exists, is not used, and has not expired.
     */
    @Transactional(readOnly = true)
    public PasswordResetToken validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Reset token cannot be empty");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token has expired");
        }

        return resetToken;
    }

    /**
     * Mark a reset token as used after password update.
     */
    @Transactional
    public void consumeToken(PasswordResetToken token) {
        token.setUsed(true);
        tokenRepository.save(token);
    }

    /**
     * Optional lookup for debugging or testing.
     */
    public Optional<PasswordResetToken> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }
}
