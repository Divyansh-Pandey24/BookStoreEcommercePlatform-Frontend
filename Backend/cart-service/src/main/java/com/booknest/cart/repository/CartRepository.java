package com.booknest.cart.repository;

import com.booknest.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// Repository for database operations on Cart entities
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    // Retrieve the cart for a specific user
    Optional<Cart> findByUserId(Long userId);

    // Verify if a cart exists for a given user ID
    boolean existsByUserId(Long userId);

    // Remove a user's cart from the database
    void deleteByUserId(Long userId);
}