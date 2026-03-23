package com.example.ticketbox.config;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    // Limits
    private static final int AUTHENTICATED_LIMIT = 300;   // per user per minute
    private static final int ANONYMOUS_LIMIT = 60;         // per IP per minute
    private static final int SENSITIVE_LIMIT = 10;         // per IP per minute for sensitive endpoints

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private static final String KEY_USER = "RATE_LIMIT_USER:";
    private static final String KEY_IP = "RATE_LIMIT_IP:";
    private static final String KEY_SENSITIVE = "RATE_LIMIT_SENSITIVE:";

    private static final Set<String> SENSITIVE_PATHS = Set.of(
            "/v1/auth/login",
            "/v1/auth/register",
            "/v1/auth/forgot-password",
            "/v1/auth/reset-password"
    );

    private final StringRedisTemplate redisTemplate;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = getClientIp(request);
        String path = request.getRequestURI();

        // 1. Sensitive endpoint check (per IP, stricter limit)
        if (SENSITIVE_PATHS.contains(path)) {
            if (isRateLimited(KEY_SENSITIVE + ip, SENSITIVE_LIMIT, response, SENSITIVE_LIMIT)) {
                return;
            }
        }

        // 2. Per-user or per-IP check
        String userEmail = extractEmailFromToken(request);
        if (userEmail != null) {
            // Authenticated: track by email, generous limit
            if (isRateLimited(KEY_USER + userEmail, AUTHENTICATED_LIMIT, response, AUTHENTICATED_LIMIT)) {
                return;
            }
        } else {
            // Anonymous: track by IP, stricter limit
            if (isRateLimited(KEY_IP + ip, ANONYMOUS_LIMIT, response, ANONYMOUS_LIMIT)) {
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Increment counter for key and check against limit.
     * Sets remaining header and returns true if rate limited (should block).
     */
    private boolean isRateLimited(String key, int limit, HttpServletResponse response, int headerLimit)
            throws IOException {

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW);
        }

        if (count != null && count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> body = ApiResponse.error("429", "Too many requests. Please try again later.");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return true;
        }

        // Set headers based on most restrictive limit applied last
        response.setHeader("X-RateLimit-Limit", String.valueOf(headerLimit));
        response.setHeader("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, limit - (count != null ? count : 0))));
        return false;
    }

    private String extractEmailFromToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            try {
                if (jwtUtils.validateToken(token) && "ACCESS".equals(jwtUtils.getTokenType(token))) {
                    return jwtUtils.getEmailFromToken(token);
                }
            } catch (Exception ignored) {
                // Invalid token — fall through to IP-based limit
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
