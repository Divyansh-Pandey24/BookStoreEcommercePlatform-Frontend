package com.booknest.wallet.repository;

import com.booknest.wallet.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// Repository for database operations on Transaction entities
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Retrieve transactions for a user, sorted by date in descending order
    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}