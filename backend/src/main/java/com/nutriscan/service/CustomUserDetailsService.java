package com.nutriscan.service;

import com.nutriscan.model.User;
import com.nutriscan.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Custom UserDetailsService for Spring Security local authentication.
 * 
 * Loads a user from the PostgreSQL database by email and provides their
 * BCrypt-hashed password and assigned roles (e.g. ROLE_STUDENT).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));

        // If user registered only via Google, they do not have a local password
        String password = user.getPasswordHash();
        if (password == null || password.isBlank()) {
            throw new UsernameNotFoundException("Account was registered via Google OAuth. Please sign in with Google.");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
