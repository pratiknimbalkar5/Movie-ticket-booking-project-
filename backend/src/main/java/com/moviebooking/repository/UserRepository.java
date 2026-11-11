package com.moviebooking.repository;

import com.moviebooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User entity.
 *
 * By extending JpaRepository<User, Long>, we get these methods for FREE:
 *   - save(user)              → INSERT or UPDATE
 *   - findById(id)            → SELECT WHERE id = ?
 *   - findAll()               → SELECT *
 *   - delete(user)            → DELETE
 *   - count()                 → SELECT COUNT(*)
 *   - existsById(id)          → SELECT EXISTS(...)
 *   ... and more
 *
 * We add custom methods using Spring Data's "method name derivation":
 * Spring reads the method name and generates the SQL automatically.
 * findByUsername → SELECT * FROM users WHERE username = ?
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their username.
     * Used during login to look up the user.
     * Returns Optional to force null-checking — no NullPointerExceptions.
     */
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
