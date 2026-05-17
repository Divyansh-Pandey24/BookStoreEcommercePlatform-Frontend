package com.booknest.auth.service;

import com.booknest.auth.dto.AuthResponse;
import com.booknest.auth.dto.LoginRequest;
import com.booknest.auth.dto.RegisterRequest;
import com.booknest.auth.entity.User;
import com.booknest.auth.exception.ResourceNotFoundException;
import com.booknest.auth.repository.PasswordResetTokenRepository;
import com.booknest.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;
    
    @Mock
    private PasswordResetTokenRepository tokenRepo;

    @Mock
    private PasswordResetService emailService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private BloomFilterService bloomFilterService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setUserId(1L);
        sampleUser.setEmail("test@example.com");
        sampleUser.setFullName("Test User");
        sampleUser.setRole("CUSTOMER");
        sampleUser.setSuspended(false);
        sampleUser.setProvider("LOCAL");
    }

    @Test
    void testRegister_Success() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@example.com");
        req.setFullName("New User");
        req.setPassword("password");

        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

        String result = authService.register(req);
        
        assertEquals("User registered successfully. Please login to continue.", result);
        verify(userRepo, times(1)).save(any(User.class));
        verify(bloomFilterService, times(1)).addEmail(anyString());
    }

    @Test
    void testRegister_AlreadyExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        when(userRepo.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(req));
    }

    @Test
    void testLogin_Success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("password");

        when(bloomFilterService.mightExist(anyString())).thenReturn(true);
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateAccessToken(any())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");

        AuthResponse response = authService.login(req);
        
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    void testLogin_BloomFilterRejects() {
        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@example.com");

        when(bloomFilterService.mightExist("unknown@example.com")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(req));
        assertEquals("Invalid email or password", ex.getMessage());
        verify(userRepo, never()).findByEmail(anyString());
    }

    @Test
    void testLogin_Suspended() {
        sampleUser.setSuspended(true);
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");

        when(bloomFilterService.mightExist(anyString())).thenReturn(true);
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    void testLogin_GoogleProvider() {
        sampleUser.setProvider("GOOGLE");
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");

        when(bloomFilterService.mightExist(anyString())).thenReturn(true);
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    void testRefreshTokens_Success() {
        when(jwtService.isTokenValid("valid_refresh")).thenReturn(true);
        when(jwtService.extractUserId("valid_refresh")).thenReturn("1");
        when(userRepo.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("new_access");
        when(jwtService.generateRefreshToken(sampleUser)).thenReturn("new_refresh");

        AuthResponse response = authService.refreshTokens("valid_refresh");
        
        assertEquals("new_access", response.getAccessToken());
        assertEquals("new_refresh", response.getRefreshToken());
    }

    @Test
    void testRefreshTokens_Invalid() {
        when(jwtService.isTokenValid("invalid_refresh")).thenReturn(false);
        assertThrows(RuntimeException.class, () -> authService.refreshTokens("invalid_refresh"));
    }

    @Test
    void testRefreshTokens_UserNotFound() {
        when(jwtService.isTokenValid("valid_refresh")).thenReturn(true);
        when(jwtService.extractUserId("valid_refresh")).thenReturn("1");
        when(userRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.refreshTokens("valid_refresh"));
    }

    @Test
    void testProcessGoogleUser_ReturningUser() {
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateAccessToken(any())).thenReturn("access_token");

        AuthResponse response = authService.processGoogleUser("test@example.com", "Test User", "pic.jpg");
        
        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        verify(userRepo, times(1)).save(any(User.class)); 
    }

    @Test
    void testProcessGoogleUser_NewUser() {
        when(userRepo.findByEmail("newgoogle@example.com")).thenReturn(Optional.empty());
        when(userRepo.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setUserId(2L);
            return u;
        });
        when(jwtService.generateAccessToken(any())).thenReturn("access_token");

        AuthResponse response = authService.processGoogleUser("newgoogle@example.com", "New Google", "pic.jpg");
        
        assertEquals("access_token", response.getAccessToken());
        verify(bloomFilterService, times(1)).addEmail("newgoogle@example.com");
    }

    @Test
    void testGetUserById_Success() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(sampleUser));
        User user = authService.getUserById(1L);
        assertNotNull(user);
        assertEquals(1L, user.getUserId());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> authService.getUserById(1L));
    }
}
