package com.booknest.order.dto;

import lombok.Data;

@Data
public class UserProfileDto {
    private Long userId;
    private String email;
    private String mobile;
    private String fullName;
}