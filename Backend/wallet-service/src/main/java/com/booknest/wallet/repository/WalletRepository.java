package com.booknest.wallet.repository;

import com.booknest.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// Repository for Wallet entities
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    // Retrieve wallet by user ID
    Optional<Wallet> findByUserId(Long userId);
}