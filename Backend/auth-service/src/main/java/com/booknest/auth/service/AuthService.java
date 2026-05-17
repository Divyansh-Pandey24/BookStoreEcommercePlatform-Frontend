package com.booknest.auth.service;

import com.booknest.auth.dto.AuthResponse;
import com.booknest.auth.dto.LoginRequest;
import com.booknest.auth.dto.RegisterRequest;
import com.booknest.auth.exception.ResourceNotFoundException;
import com.booknest.auth.entity.PasswordResetToken;
import com.booknest.auth.entity.User;
import com.booknest.auth.repository.PasswordResetTokenRepository;
import com.booknest.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Main service for authentication logic
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordResetService emailService;
    private final AuthenticationManager authenticationManager;
    private final BloomFilterService bloomFilterService; // Inject Bloom Filter

    // Registers a new user with encrypted password
    public String register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new RuntimeException("This email is already registered.");
        }

        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setMobile(req.getMobile());
        user.setRole("CUSTOMER");
        user.setProvider("LOCAL");

        userRepo.save(user);
        bloomFilterService.addEmail(user.getEmail()); // Add to Bloom Filter
        return "User registered successfully. Please login to continue.";
    }

    // Authenticates user and returns JWT tokens
    public AuthResponse login(LoginRequest req) {
        // STEP 1: Check Bloom Filter FIRST to avoid DB hit for non-existent users
        if (!bloomFilterService.mightExist(req.getEmail())) {
            log.warn("Login attempt rejected by Bloom Filter for: {}", req.getEmail());
            throw new RuntimeException("Invalid email or password");
        }

        // STEP 2: Only check DB if Bloom Filter says "Maybe Yes"
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (Boolean.TRUE.equals(user.getSuspended())) {
            throw new RuntimeException("Your account has been suspended. Please contact support.");
        }

        if ("GOOGLE".equals(user.getProvider())) {
            throw new RuntimeException("This account uses Google login. Please click 'Login with Google'.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.getEmail(),
                        req.getPassword()
                )
        );

        return buildResponse(user);
    }

    // Refreshes access token using a valid refresh token
    public AuthResponse refreshTokens(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Refresh token is invalid or expired.");
        }

        String userId = jwtService.extractUserId(refreshToken);
        User user = userRepo.findById(Long.parseLong(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildResponse(user);
    }

    // Processes user data from Google OAuth success
    public AuthResponse processGoogleUser(String email, String name, String picture) {
        User user = userRepo.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setFullName(name);
            newUser.setEmail(email);
            newUser.setPasswordHash("GOOGLE_OAUTH_NO_PASSWORD");
            newUser.setRole("CUSTOMER");
            newUser.setProvider("GOOGLE");
            newUser.setProfilePicture(picture);
            User saved = userRepo.save(newUser);
            bloomFilterService.addEmail(saved.getEmail()); // Add new Google user to filter
            return saved;
        });

        if (picture != null && !picture.equals(user.getProfilePicture())) {
            user.setProfilePicture(picture);
            userRepo.save(user);
        }

        return buildResponse(user);
    }

    // Fetches user by ID
    public User getUserById(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Helper to build AuthResponse with tokens
    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                user.getRole(),
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getProfilePicture());
    }
}