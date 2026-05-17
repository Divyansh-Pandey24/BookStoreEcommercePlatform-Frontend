package com.booknest.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.booknest.auth.dto.AuthResponse;
import com.booknest.auth.dto.LoginRequest;
import com.booknest.auth.dto.RegisterRequest;
import com.booknest.auth.dto.UserProfileDto;
import com.booknest.auth.entity.User;
import com.booknest.auth.repository.UserRepository;
import com.booknest.auth.service.AuthService;
import com.booknest.auth.service.PasswordResetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Controller for authentication and user management
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final UserRepository userRepository;

    // Register a new user
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received register request for: {}", request.getEmail());
        return ResponseEntity.ok(authService.register(request));
    }

    // Authenticate user and return tokens
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request for: {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    // Refresh expired access tokens
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RuntimeException("Refresh token is required");
        }
        return ResponseEntity.ok(authService.refreshTokens(refreshToken));
    }

    // Get profile details of the logged-in user
    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    // Initiate password reset flow
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }
        String msg = passwordResetService.forgotPassword(email);
        return ResponseEntity.ok(Map.of("message", msg));
    }

    // Reset password using token
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("Token and new password are required");
        }
        String msg = passwordResetService.resetPassword(token, newPassword);
        return ResponseEntity.ok(Map.of("message", msg));
    }

    // Get user profile details by ID
    @GetMapping("/user/{userId}")
    public UserProfileDto getUserById(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new UserProfileDto(user.getUserId(), user.getFullName(), user.getEmail(), user.getMobile());
    }
}