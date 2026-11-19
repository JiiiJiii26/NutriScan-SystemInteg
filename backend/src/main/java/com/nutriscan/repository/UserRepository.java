package com.nutriscan.repository;

import com.nutriscan.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for User entity.
 * 
 * Provides built-in CRUD operations and custom query methods
 * for email lookup and duplicate account checks.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by email address (case-sensitive or lowercase match).
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an account already exists with the given email.
     */
    boolean existsByEmail(String email);
}
