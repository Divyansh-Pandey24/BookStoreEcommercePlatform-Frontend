package com.booknest.notification.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.booknest.notification.dto.NotificationResponse;
import com.booknest.notification.entity.Notification;
import com.booknest.notification.exception.ResourceNotFoundException;
import com.booknest.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Service implementation for managing and delivering user notifications via database and email
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    // Convert Notification entity to NotificationResponse DTO
    private NotificationResponse toResponse(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.setNotificationId(n.getNotificationId());
        r.setUserId(n.getUserId());
        r.setType(n.getType());
        r.setMessage(n.getMessage());
        r.setIsRead(n.getIsRead());
        r.setCreatedAt(n.getCreatedAt());
        return r;
    }

    // Persist a notification and trigger email/SMS delivery
    public void saveNotification(Long userId, String type, String message, String userEmail, String userMobile) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setMessage(message);
        notificationRepository.save(n);

        log.info("Notification saved for user: {}, type: {}", userId, type);
        sendEmail(userEmail, type, message);
        sendSMS(userMobile, message);
    }

    // Deliver notification message via email
    private void sendEmail(String email, String type, String message) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("Skipping email delivery: no email address provided");
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(email);
            mail.setSubject("BookNest: " + type);
            mail.setText(message + "\n\nThank you for using BookNest!");
            mailSender.send(mail);
            log.info("Email sent to: {}", email);
        } catch (Exception e) {
            log.error("Email delivery failed to {}: {}", email, e.getMessage());
        }
    }

    // Placeholder for SMS delivery integration
    private void sendSMS(String mobile, String message) {
        if (mobile != null && !mobile.trim().isEmpty()) {
            log.info("SMS delivery (placeholder) for {}: {}", mobile, message);
        }
    }

    // Retrieve all notifications for a specific user ID
    public List<NotificationResponse> getNotifications(Long userId) {
        log.info("Fetching notifications for user: {}", userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Get the count of unread notifications for a specific user ID
    public long getUnreadCount(Long userId) {
        log.debug("Fetching unread count for user: {}", userId);
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    // Mark a specific notification as read by ID
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!n.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: notification does not belong to user");
        }

        n.setIsRead(true);
        return toResponse(notificationRepository.save(n));
    }

    // Mark all unread notifications for a specific user as read
    @Transactional
    public void markAllAsRead(Long userId) {
        log.info("Marking all notifications as read for user: {}", userId);
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    // Delete a specific notification by ID
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!n.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied: notification does not belong to user");
        }

        notificationRepository.delete(n);
        log.info("Deleted notification ID: {}", notificationId);
    }
}