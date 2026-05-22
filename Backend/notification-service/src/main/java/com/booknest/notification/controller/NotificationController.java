package com.booknest.notification.controller;

import com.booknest.notification.dto.NotificationResponse;
import com.booknest.notification.service.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

// Controller for managing user notifications and read status
@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationServiceImpl notificationService;

    // Retrieves all notification records generated for the active user.
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getAll(@RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching notifications for user: {}", userId);
        return ResponseEntity.ok(notificationService.getNotifications(userId));
    }

    // Returns the count of all active notifications marked as unread.
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        log.info("Fetching unread count for user: {}", userId);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // Marks a single notification record as read.
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long notificationId, @RequestHeader("X-User-Id") Long userId) {
        log.info("Marking notification ID: {} as read for user: {}", notificationId, userId);
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, userId));
    }

    // Marks all notifications for a specific user as read.
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestHeader("X-User-Id") Long userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    // Deletes a specific notification record permanently.
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> delete(@PathVariable Long notificationId, @RequestHeader("X-User-Id") Long userId) {
        log.info("Deleting notification ID: {} for user: {}", notificationId, userId);
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.noContent().build();
    }
}
