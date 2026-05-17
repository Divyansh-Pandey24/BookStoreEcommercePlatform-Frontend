package com.booknest.notification.service;

import com.booknest.notification.dto.NotificationResponse;
import com.booknest.notification.entity.Notification;
import com.booknest.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private JavaMailSender mailSender;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = new Notification();
        sampleNotification.setNotificationId(1L);
        sampleNotification.setUserId(10L);
        sampleNotification.setType("ORDER_PLACED");
        sampleNotification.setMessage("Your order has been placed!");
        sampleNotification.setIsRead(false);
    }

    // ─────────────────────────── SAVE NOTIFICATION ───────────────────────────

    @Test
    @DisplayName("saveNotification: saves to DB and sends email when email is valid")
    void saveNotification_withEmail_savesAndSendsEmail() {
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(sampleNotification);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.saveNotification(
                10L, "ORDER_PLACED", "Your order has been placed!",
                "user@booknest.com", "9876543210");

        verify(notificationRepository).save(any(Notification.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("saveNotification: empty email → saves to DB, skips email send")
    void saveNotification_emptyEmail_skipsEmail() {
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(sampleNotification);

        notificationService.saveNotification(
                10L, "ORDER_PLACED", "Your order has been placed!", "", "");

        verify(notificationRepository).save(any(Notification.class));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ─────────────────────────── GET NOTIFICATIONS ───────────────────────────

    @Test
    @DisplayName("getNotifications: returns list of notifications for user")
    void getNotifications_returnsList() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(sampleNotification));

        List<NotificationResponse> result = notificationService.getNotifications(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("ORDER_PLACED");
    }

    // ─────────────────────────── GET UNREAD COUNT ───────────────────────────

    @Test
    @DisplayName("getUnreadCount: returns count of unread notifications")
    void getUnreadCount_returnsCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(10L)).thenReturn(3L);

        long count = notificationService.getUnreadCount(10L);

        assertThat(count).isEqualTo(3L);
    }

    // ─────────────────────────── MARK AS READ ───────────────────────────

    @Test
    @DisplayName("markAsRead: own notification → marked read and saved")
    void markAsRead_success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        NotificationResponse response = notificationService.markAsRead(1L, 10L);

        assertThat(sampleNotification.getIsRead()).isTrue();
        verify(notificationRepository).save(sampleNotification);
    }

    @Test
    @DisplayName("markAsRead: different user's notification → throws RuntimeException")
    void markAsRead_wrongUser_throws() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }

    // ─────────────────────────── MARK ALL AS READ ───────────────────────────

    @Test
    @DisplayName("markAllAsRead: marks all unread notifications as read")
    void markAllAsRead_success() {
        Notification unread1 = new Notification();
        unread1.setUserId(10L);
        unread1.setIsRead(false);
        Notification unread2 = new Notification();
        unread2.setUserId(10L);
        unread2.setIsRead(false);

        when(notificationRepository.findByUserIdAndIsReadFalse(10L))
                .thenReturn(List.of(unread1, unread2));
        when(notificationRepository.saveAll(anyList())).thenReturn(List.of());

        notificationService.markAllAsRead(10L);

        assertThat(unread1.getIsRead()).isTrue();
        assertThat(unread2.getIsRead()).isTrue();
        verify(notificationRepository).saveAll(anyList());
    }

    // ─────────────────────────── DELETE ───────────────────────────

    @Test
    @DisplayName("deleteNotification: own notification → deleted")
    void deleteNotification_success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));
        doNothing().when(notificationRepository).delete(sampleNotification);

        notificationService.deleteNotification(1L, 10L);

        verify(notificationRepository).delete(sampleNotification);
    }

    @Test
    @DisplayName("deleteNotification: different user → throws RuntimeException")
    void deleteNotification_wrongUser_throws() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));

        assertThatThrownBy(() -> notificationService.deleteNotification(1L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }
}
