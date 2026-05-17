package com.booknest.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// Entity representing a user in the system
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    // Primary key with auto-increment
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    // User's full name
    @Column(nullable = false)
    private String fullName;

    // Unique email address for login
    @Column(unique = true, nullable = false)
    private String email;

    // BCrypt hashed password or OAuth placeholder
    private String passwordHash;

    // User role (e.g., CUSTOMER, ADMIN)
    @Column(nullable = false)
    private String role;

    // Authentication provider (LOCAL or GOOGLE)
    @Column(nullable = false)
    private String provider;

    // URL to profile picture
    private String profilePicture;

    // User's mobile number
    private String mobile;

    // Timestamp when the account was created
    private LocalDateTime createdAt = LocalDateTime.now();

    // Soft-delete / suspension flag — suspended users cannot login
    @Column(nullable = false)
    private Boolean suspended = false;
}