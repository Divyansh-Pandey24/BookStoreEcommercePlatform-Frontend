package com.booknest.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 * This is the message we send to Kafka.
 * Notification Service reads this and creates a notification.
 *
 * Think of it like a letter:
 * - userId  → who gets the notification
 * - type    → what kind of event
 * - message → what to show the customer
 * - orderId → which order this is about
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventDto {
    private Long   userId;
    private String type;
    private String message;
    private Long   orderId;
    private String userEmail;      // ← NEW
    private String userMobile;     // ← NEW
}