package com.booknest.cart.repository;

import com.booknest.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// Repository for database operations on CartItem entities
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Retrieve a specific book from a given user's cart
    Optional<CartItem> findByCartCartIdAndBookId(Long cartId, Long bookId);
}