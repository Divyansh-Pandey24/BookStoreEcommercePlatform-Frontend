package com.booknest.auth.controller;

import com.booknest.auth.dto.AuthResponse;
import com.booknest.auth.dto.LoginRequest;
import com.booknest.auth.dto.RegisterRequest;
import com.booknest.auth.exception.GlobalExceptionHandler;
import com.booknest.auth.repository.UserRepository;
import com.booknest.auth.service.AuthService;
import com.booknest.auth.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Integration Tests (MockMvc)")
class AuthControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock private AuthService authService;
    @Mock private PasswordResetService passwordResetService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private AuthResponse sampleAuthResponse() {
        return new AuthResponse(
                "access-token-xyz",
                "refresh-token-xyz",
                "CUSTOMER",
                1L,
                "test@booknest.com",
                "Test User",
                null
        );
    }

    // ─────────────────────────── REGISTER ───────────────────────────

    @Test
    @DisplayName("POST /auth/register: success → 200 OK")
    void register_success() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@booknest.com");
        req.setPassword("Password@123");
        req.setFullName("Test User");

        when(authService.register(any(RegisterRequest.class))).thenReturn("User registered successfully");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    @DisplayName("POST /auth/register: duplicate email → 400 Bad Request")
    void register_duplicateEmail_fails() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@booknest.com");
        req.setPassword("Password@123");
        req.setFullName("Existing User"); // Added to avoid validation error

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("This email is already registered."));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This email is already registered."));
    }

    @Test
    @DisplayName("POST /auth/register: invalid input → 400 Bad Request")
    void register_invalidInput_fails() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("invalid-email"); // Invalid email format
        req.setPassword("password"); // Valid length but missing number
        // fullName missing

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fullName").value("Full name is required"))
                .andExpect(jsonPath("$.email").value("Please provide a valid email"))
                .andExpect(jsonPath("$.password").value("Password must contain at least one letter and one number"));
    }

    // ─────────────────────────── LOGIN ───────────────────────────

    @Test
    @DisplayName("POST /auth/login: success → 200 OK")
    void login_success() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@booknest.com");
        req.setPassword("password");

        when(authService.login(any(LoginRequest.class))).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-xyz"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /auth/login: invalid credentials → 400 Bad Request")
    void login_invalidCredentials_fails() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("wrong@booknest.com");
        req.setPassword("wrong");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid email or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    // ─────────────────────────── REFRESH ───────────────────────────

    @Test
    @DisplayName("POST /auth/refresh: success → 200 OK")
    void refresh_success() throws Exception {
        Map<String, String> body = Map.of("refreshToken", "valid-token");

        when(authService.refreshTokens("valid-token")).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-xyz"));
    }

    @Test
    @DisplayName("POST /auth/refresh: missing token → 400 Bad Request")
    void refresh_missingToken_fails() throws Exception {
        Map<String, String> body = Map.of(); // Empty body

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh token is required"));
    }

    // ─────────────────────────── FORGOT PASSWORD ───────────────────────────

    @Test
    @DisplayName("POST /auth/forgot-password: success → 200 OK")
    void forgotPassword_success() throws Exception {
        Map<String, String> body = Map.of("email", "test@booknest.com");

        when(passwordResetService.forgotPassword("test@booknest.com")).thenReturn("Email sent");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email sent"));
    }
}
