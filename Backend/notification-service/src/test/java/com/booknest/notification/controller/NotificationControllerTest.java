package com.booknest.notification.controller;

import com.booknest.notification.dto.NotificationResponse;
import com.booknest.notification.exception.GlobalExceptionHandler;
import com.booknest.notification.exception.ResourceNotFoundException;
import com.booknest.notification.service.NotificationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController Integration Tests (MockMvc)")
class NotificationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private NotificationServiceImpl notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private NotificationResponse sampleNotify;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleNotify = new NotificationResponse();
        sampleNotify.setNotificationId(1L);
        sampleNotify.setUserId(10L);
        sampleNotify.setMessage("Test message");
        sampleNotify.setIsRead(false);
        sampleNotify.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("GET /notifications: success → 200 OK")
    void getAll_success() throws Exception {
        when(notificationService.getNotifications(10L)).thenReturn(List.of(sampleNotify));

        mockMvc.perform(get("/notifications")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value(1L));
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read: success → 200 OK")
    void markAsRead_success() throws Exception {
        when(notificationService.markAsRead(1L, 10L)).thenReturn(sampleNotify);

        mockMvc.perform(patch("/notifications/1/read")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(1L));
    }

    @Test
    @DisplayName("PATCH /notifications/{id}/read: not found → 404 Not Found")
    void markAsRead_notFound_fails() throws Exception {
        when(notificationService.markAsRead(99L, 10L))
                .thenThrow(new ResourceNotFoundException("Notification not found"));

        mockMvc.perform(patch("/notifications/99/read")
                        .header("X-User-Id", 10L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found"));
    }

    @Test
    @DisplayName("DELETE /notifications/{id}: success → 204 No Content")
    void delete_success() throws Exception {
        doNothing().when(notificationService).deleteNotification(1L, 10L);

        mockMvc.perform(delete("/notifications/1")
                        .header("X-User-Id", 10L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /notifications/unread-count: success → 200 OK")
    void getUnreadCount_success() throws Exception {
        when(notificationService.getUnreadCount(10L)).thenReturn(5L);

        mockMvc.perform(get("/notifications/unread-count")
                        .header("X-User-Id", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }
}
