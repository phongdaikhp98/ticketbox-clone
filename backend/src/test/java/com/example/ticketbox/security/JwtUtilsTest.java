package com.example.ticketbox.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        String secret = Base64.getEncoder().encodeToString("test-secret-key-that-is-at-least-32-bytes-long!".getBytes());
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtils, "accessTokenExpirationMs", 86400000L);
        ReflectionTestUtils.setField(jwtUtils, "refreshTokenExpirationMs", 604800000L);
    }

    @Test
    void generateAccessToken_returnsValidToken() {
        String token = jwtUtils.generateAccessToken("test@example.com", "CUSTOMER");

        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));
        assertEquals("test@example.com", jwtUtils.getEmailFromToken(token));
        assertEquals("ACCESS", jwtUtils.getTokenType(token));
    }

    @Test
    void generateRefreshToken_returnsValidToken() {
        String token = jwtUtils.generateRefreshToken("test@example.com");

        assertNotNull(token);
        assertTrue(jwtUtils.validateToken(token));
        assertEquals("test@example.com", jwtUtils.getEmailFromToken(token));
        assertEquals("REFRESH", jwtUtils.getTokenType(token));
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(jwtUtils.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_nullToken_returnsFalse() {
        assertFalse(jwtUtils.validateToken(null));
    }

    @Test
    void validateToken_emptyToken_returnsFalse() {
        assertFalse(jwtUtils.validateToken(""));
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtUtils, "accessTokenExpirationMs", -1000L);
        String token = jwtUtils.generateAccessToken("test@example.com", "CUSTOMER");

        assertFalse(jwtUtils.validateToken(token));
    }

    @Test
    void accessAndRefreshTokens_haveDifferentTypes() {
        String accessToken = jwtUtils.generateAccessToken("test@example.com", "CUSTOMER");
        String refreshToken = jwtUtils.generateRefreshToken("test@example.com");

        assertEquals("ACCESS", jwtUtils.getTokenType(accessToken));
        assertEquals("REFRESH", jwtUtils.getTokenType(refreshToken));
        assertNotEquals(accessToken, refreshToken);
    }
}
