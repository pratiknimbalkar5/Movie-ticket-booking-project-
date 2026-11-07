package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a registered user of the system.
 *
 * Maps to the "users" table in MySQL.
 * A User can have many Bookings (one-to-many relationship).
 *
 * Why @Entity? It tells JPA/Hibernate: "this class = a database table".
 * Why @Table(name="users")? "user" is a reserved word in MySQL, so we use "users".
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT in MySQL
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * NEVER store plaintext password. This column stores the BCrypt hash.
     * BCrypt hash always starts with "$2a$" and is 60 characters long.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Role determines what the user can do:
     * - USER: browse movies, book seats, view own bookings, cancel own bookings
     * - ADMIN: all USER rights + create/update movies, theatres, screens, shows
     *
     * @Enumerated(EnumType.STRING): stores "USER" or "ADMIN" as text in DB,
     * not as integers (0, 1) — much more readable and safer.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * One user can have many bookings.
     * mappedBy = "user": the Booking entity owns this relationship (has the FK column).
     * FetchType.LAZY: don't load all bookings when we load a user (performance).
     * cascade = REMOVE: if user is deleted, their bookings are deleted too.
     */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Booking> bookings;

    public enum Role {
        USER, ADMIN
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Booking> getBookings() { return bookings; }
    public void setBookings(List<Booking> bookings) { this.bookings = bookings; }
}
