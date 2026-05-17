package com.booknest.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

// Entity for storing password reset tokens
@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
public class PasswordResetToken {

    // Primary key for the token record
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique token string sent to user
    @Column(nullable = false, unique = true)
    private String token;

    // Date and time when the token expires
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    // Mapping to the user associated with this reset token
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User userAuthEntity;
}