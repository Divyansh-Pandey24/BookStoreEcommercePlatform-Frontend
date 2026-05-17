package com.booknest.order.repository;

import com.booknest.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// Repository for database operations on Order entities
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Retrieve order history for a specific customer sorted by newest first
    List<Order> findByUserIdOrderByPlacedAtDesc(Long userId);

    // Retrieve all orders filtered by their status
    List<Order> findByOrderStatus(String orderStatus);

    // Get the total number of orders placed by a specific user
    long countByUserId(Long userId);
}