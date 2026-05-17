package com.booknest.auth.service;

import com.booknest.auth.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "my-super-secret-key-that-must-be-very-long-and-secure");
        ReflectionTestUtils.setField(jwtService, "accessExpiry", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtService, "refreshExpiry", 86400000L); // 24 hours

        sampleUser = new User();
        sampleUser.setUserId(1L);
        sampleUser.setEmail("test@example.com");
        sampleUser.setRole("CUSTOMER");
        sampleUser.setFullName("Test User");
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        String token = jwtService.generateAccessToken(sampleUser);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        
        assertEquals("1", jwtService.extractUserId(token));
        assertEquals("CUSTOMER", jwtService.extractRole(token));
    }

    @Test
    void testGenerateAndValidateRefreshToken() {
        String token = jwtService.generateRefreshToken(sampleUser);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("1", jwtService.extractUserId(token));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.string"));
    }

    @Test
    void testExtractClaims() {
        String token = jwtService.generateAccessToken(sampleUser);
        Claims claims = jwtService.extractAllClaims(token);
        
        assertEquals("1", claims.getSubject());
        assertEquals("test@example.com", claims.get("email"));
        assertEquals("Test User", claims.get("fullName"));
    }
}
