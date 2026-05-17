package com.booknest.wallet.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;

// Entity representing a user's digital wallet and balance
@Entity
@Table(name = "wallets")
@Data
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long walletId;
    
    // ID of the user who owns this wallet (one wallet per user)
    @Column(unique = true, nullable = false)
    private Long userId;

    private Double balance;

    // Wallet lifecycle timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Automatically set timestamps before persisting
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // Automatically update timestamp before saving changes
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}