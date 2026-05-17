package com.booknest.auth.service;

import com.booknest.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.Key;
import java.util.Date;

// Service for JWT operations including generation and validation
@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiry}")
    private long accessExpiry;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshExpiry;

    // Helper to get the cryptographic signing key
    private Key signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Generates a short-lived access token with user claims
    public String generateAccessToken(User user) {
        log.debug("Generating access token for user ID: {}", user.getUserId());
        return Jwts.builder()
            .setSubject(String.valueOf(user.getUserId()))
            .claim("role", user.getRole())
            .claim("email", user.getEmail())
            .claim("fullName", user.getFullName())
            .claim("profilePicture", user.getProfilePicture())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessExpiry))
            .signWith(signingKey())
            .compact();
    }

    // Generates a long-lived refresh token
    public String generateRefreshToken(User user) {
        log.debug("Generating refresh token for user ID: {}", user.getUserId());
        return Jwts.builder()
            .setSubject(String.valueOf(user.getUserId()))
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + refreshExpiry))
            .signWith(signingKey())
            .compact();
    }

    // Extracts all claims from a JWT token
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    // Extracts user ID from token subject
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extracts user role from token claims
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // Validates token signature and expiration
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}