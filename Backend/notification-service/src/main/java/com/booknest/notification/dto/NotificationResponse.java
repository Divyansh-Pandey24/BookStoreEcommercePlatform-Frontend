package com.booknest.notification.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class NotificationResponse {
    private Long          notificationId;
    private Long          userId;
    private String        type;
    private String        message;
    private Boolean       isRead;
    private LocalDateTime createdAt;
}