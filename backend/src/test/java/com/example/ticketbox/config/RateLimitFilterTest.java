package com.example.ticketbox.config;

import com.example.ticketbox.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        // Use explicit config so tests are independent of default values
        AppProperties.RateLimit rl = new AppProperties.RateLimit();
        rl.setAuthenticatedLimit(300);
        rl.setAnonymousLimit(60);
        rl.setSensitiveLimit(10);
        rl.setWindowMinutes(1);
        rl.setSensitivePaths(List.of(
                "/v1/auth/login",
                "/v1/auth/register",
                "/v1/auth/forgot-password",
                "/v1/auth/reset-password"
        ));
        appProperties.setRateLimit(rl);

        rateLimitFilter = new RateLimitFilter(redisTemplate, jwtUtils, appProperties);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ──────────────────────────────────────────────
    // Anonymous (IP-based) tests
    // ──────────────────────────────────────────────

    @Test
    void anonymousRequest_underLimit_allowsRequest() throws Exception {
        when(valueOperations.increment("RATE_LIMIT_IP:127.0.0.1")).thenReturn(5L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertEquals("60", response.getHeader("X-RateLimit-Limit"));
        assertEquals("55", response.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    void anonymousRequest_overLimit_returns429() throws Exception {
        when(valueOperations.increment("RATE_LIMIT_IP:127.0.0.1")).thenReturn(61L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
    }

    @Test
    void anonymousRequest_firstRequest_setsExpiry() throws Exception {
        when(valueOperations.increment("RATE_LIMIT_IP:127.0.0.1")).thenReturn(1L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(redisTemplate).expire(eq("RATE_LIMIT_IP:127.0.0.1"), any());
    }

    @Test
    void anonymousRequest_exactlyAtLimit_allowsRequest() throws Exception {
        when(valueOperations.increment("RATE_LIMIT_IP:127.0.0.1")).thenReturn(60L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertEquals("0", response.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    void anonymousRequest_usesXForwardedForHeader() throws Exception {
        when(valueOperations.increment("RATE_LIMIT_IP:10.0.0.1")).thenReturn(1L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(valueOperations).increment("RATE_LIMIT_IP:10.0.0.1");
    }

    // ──────────────────────────────────────────────
    // Authenticated (user-based) tests
    // ──────────────────────────────────────────────

    @Test
    void authenticatedRequest_usesUserEmailAsKey() throws Exception {
        String token = "valid.jwt.token";
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getTokenType(token)).thenReturn("ACCESS");
        when(jwtUtils.getEmailFromToken(token)).thenReturn("user@example.com");
        when(valueOperations.increment("RATE_LIMIT_USER:user@example.com")).thenReturn(10L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(valueOperations).increment("RATE_LIMIT_USER:user@example.com");
        verify(valueOperations, never()).increment(startsWith("RATE_LIMIT_IP:"));
    }

    @Test
    void authenticatedRequest_higherLimit300_allowsRequest() throws Exception {
        String token = "valid.jwt.token";
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getTokenType(token)).thenReturn("ACCESS");
        when(jwtUtils.getEmailFromToken(token)).thenReturn("user@example.com");
        when(valueOperations.increment("RATE_LIMIT_USER:user@example.com")).thenReturn(250L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertEquals("300", response.getHeader("X-RateLimit-Limit"));
    }

    @Test
    void authenticatedRequest_overLimit_returns429() throws Exception {
        String token = "valid.jwt.token";
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getTokenType(token)).thenReturn("ACCESS");
        when(jwtUtils.getEmailFromToken(token)).thenReturn("user@example.com");
        when(valueOperations.increment("RATE_LIMIT_USER:user@example.com")).thenReturn(301L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
    }

    @Test
    void invalidToken_fallsBackToIpLimit() throws Exception {
        when(jwtUtils.validateToken("bad.token")).thenReturn(false);
        when(valueOperations.increment("RATE_LIMIT_IP:127.0.0.1")).thenReturn(5L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("Authorization", "Bearer bad.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(valueOperations).increment("RATE_LIMIT_IP:127.0.0.1");
    }

    // ──────────────────────────────────────────────
    // Sensitive endpoint tests
    // ──────────────────────────────────────────────

    @Test
    void sensitiveEndpoint_underSensitiveLimit_allowsRequest() throws Exception {
        when(valueOperations.increment("RATE_LIMIT_SENSITIVE:127.0.0.1")).thenReturn(3L);
        when(valueOperations.increment("RATE_LIMIT_IP:127.0.0.1")).thenReturn(3L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void sensitiveEndpoint_overSensitiveLimit_returns429() throws Exception {
        when(valueOperations.increment("RATE_LIMIT_SENSITIVE:127.0.0.1")).thenReturn(11L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
    }

    @Test
    void sensitiveEndpoint_forgotPassword_appliesSensitiveLimit() throws Exception {
        when(valueOperations.increment("RATE_LIMIT_SENSITIVE:127.0.0.1")).thenReturn(5L);
        when(valueOperations.increment("RATE_LIMIT_IP:127.0.0.1")).thenReturn(5L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/auth/forgot-password");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(valueOperations).increment("RATE_LIMIT_SENSITIVE:127.0.0.1");
        assertEquals(200, response.getStatus());
    }

    @Test
    void nonSensitiveEndpoint_doesNotCheckSensitiveKey() throws Exception {
        when(valueOperations.increment("RATE_LIMIT_IP:127.0.0.1")).thenReturn(1L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/events");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(valueOperations, never()).increment(startsWith("RATE_LIMIT_SENSITIVE:"));
        assertEquals(200, response.getStatus());
    }
}
