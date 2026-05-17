package com.booknest.notification.repository;

import com.booknest.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

// Repository for database operations on Notification entities
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Retrieve notifications for a user, sorted by date in descending order
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Retrieve all unread notifications for a specific user
    List<Notification> findByUserIdAndIsReadFalse(Long userId);

    // Count total unread notifications for a specific user
    long countByUserIdAndIsReadFalse(Long userId);
}