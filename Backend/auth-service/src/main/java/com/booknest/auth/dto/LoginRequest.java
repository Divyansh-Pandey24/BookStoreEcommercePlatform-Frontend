package com.booknest.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// DTO for user login requests
@Data
public class LoginRequest {

    // User's email address
    @Email(message = "Please provide a valid email")
    @NotBlank(message = "Email is required")
    private String email;

    // User's password
    @NotBlank(message = "Password is required")
    @lombok.ToString.Exclude
    private String password;
}