package com.booknest.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEventDto {
    private Long   userId;
    private String type;
    private String message;
    private Long   orderId;
    private String userEmail;      // ← Added
    private String userMobile;     // ← Added
}