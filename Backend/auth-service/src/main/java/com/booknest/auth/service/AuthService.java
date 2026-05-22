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

// This service orchestrates all core user authentication operations.
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Repository for performing database CRUD operations on User entities.
     */
    private final UserRepository userRepo;

    /**
     * Password encoder (BCrypt) used to hash raw user passwords before persistence.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Service handling JWT token generation, parsing, and expiration validation.
     */
    private final JwtService jwtService;

    /**
     * Repository managing password reset tokens persisted in the database.
     */
    private final PasswordResetTokenRepository tokenRepo;

    /**
     * Service responsible for dispatching email reset notifications.
     */
    private final PasswordResetService emailService;

    /**
     * Spring Security core manager that verifies user credentials against UserDetailsService.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Redis Bloom Filter wrapper used to prevent database load by checking if an email is registered.
     */
    private final BloomFilterService bloomFilterService;

    // Registers a new local system user.
    public String register(RegisterRequest req) {
        // Step 1: Query database to prevent registration of duplicate emails.
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new RuntimeException("This email is already registered.");
        }

        // Step 2: Assemble a new User entity from the client-provided DTO.
        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        
        // Step 3: Hash raw password using standard BCrypt before database persistence.
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setMobile(req.getMobile());
        
        // Step 4: Default newly registered profiles to CUSTOMER role and LOCAL auth provider.
        user.setRole("CUSTOMER");
        user.setProvider("LOCAL");

        // Step 5: Save entity into the relational database.
        userRepo.save(user);
        
        // Step 6: Warm the Redis Bloom filter cache with the newly registered email.
        bloomFilterService.addEmail(user.getEmail());
        
        return "User registered successfully. Please login to continue.";
    }

    // Authenticates local credential login attempts and returns dynamic session JWTs.
    public AuthResponse login(LoginRequest req) {
        // STEP 1: Verify presence in Redis Bloom Filter. If not present, abort immediately to safeguard database resources.
        if (!bloomFilterService.mightExist(req.getEmail())) {
            log.warn("Login attempt rejected by Bloom Filter for: {}", req.getEmail());
            throw new RuntimeException("Invalid email or password");
        }

        // STEP 2: Query database since the Bloom Filter returned a positive or potential match (false-positives are handled here).
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // STEP 3: Ensure administrative locks or suspensions are checked and enforced.
        if (Boolean.TRUE.equals(user.getSuspended())) {
            throw new RuntimeException("Your account has been suspended. Please contact support.");
        }

        // STEP 4: Prevent users who signed up via Google from using a raw password block login path.
        if ("GOOGLE".equals(user.getProvider())) {
            throw new RuntimeException("This account uses Google login. Please click 'Login with Google'.");
        }

        // STEP 5: Delegate raw password verification to Spring Security's AuthenticationManager.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.getEmail(),
                        req.getPassword()
                )
        );

        // STEP 6: Generate access and refresh tokens, package them in AuthResponse and return.
        return buildResponse(user);
    }

    // Refreshes and updates expired access tokens without re-requesting credentials.
    public AuthResponse refreshTokens(String refreshToken) {
        // Step 1: Validate token integrity and check if signature or timeout parameters are valid.
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new RuntimeException("Refresh token is invalid or expired.");
        }

        // Step 2: Extract embedded user ID subject from token claims.
        String userId = jwtService.extractUserId(refreshToken);
        
        // Step 3: Fetch active user record to ensure the user still exists in the system.
        User user = userRepo.findById(Long.parseLong(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Step 4: Issue new access/refresh tokens.
        return buildResponse(user);
    }

    // Processes and auto-registers users authenticated successfully via Google OAuth2.
    public AuthResponse processGoogleUser(String email, String name, String picture) {
        // Step 1: Find local record, or auto-create a new user profile on-the-fly for new Google signups.
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

        // Step 2: Update local profile picture if the Google avatar has changed.
        if (picture != null && !picture.equals(user.getProfilePicture())) {
            user.setProfilePicture(picture);
            userRepo.save(user);
        }

        // Step 3: Package local session JWTs and user profile details.
        return buildResponse(user);
    }

    // Retrieves a user profile by database primary key.
    public User getUserById(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Utility builder to generate tokens and wrap them in an AuthResponse.
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
