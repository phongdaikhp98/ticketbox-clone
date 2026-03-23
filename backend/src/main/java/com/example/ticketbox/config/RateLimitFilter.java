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

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_USER = "RATE_LIMIT_USER:";
    private static final String KEY_IP = "RATE_LIMIT_IP:";
    private static final String KEY_SENSITIVE = "RATE_LIMIT_SENSITIVE:";

    private final StringRedisTemplate redisTemplate;
    private final JwtUtils jwtUtils;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = getClientIp(request);
        String path = request.getRequestURI();
        AppProperties.RateLimit cfg = appProperties.getRateLimit();
        Duration window = Duration.ofMinutes(cfg.getWindowMinutes());

        // 1. Sensitive endpoint check (per IP, stricter limit)
        if (cfg.getSensitivePaths().contains(path)) {
            if (isRateLimited(KEY_SENSITIVE + ip, cfg.getSensitiveLimit(), window, response)) {
                return;
            }
        }

        // 2. Per-user or per-IP check
        String userEmail = extractEmailFromToken(request);
        if (userEmail != null) {
            // Authenticated: track by email, generous limit
            if (isRateLimited(KEY_USER + userEmail, cfg.getAuthenticatedLimit(), window, response)) {
                return;
            }
        } else {
            // Anonymous: track by IP, stricter limit
            if (isRateLimited(KEY_IP + ip, cfg.getAnonymousLimit(), window, response)) {
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Increment counter for key and check against limit.
     * Sets remaining header and returns true if rate limited (should block).
     */
    private boolean isRateLimited(String key, int limit, Duration window, HttpServletResponse response)
            throws IOException {

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }

        if (count != null && count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> body = ApiResponse.error("429", "Too many requests. Please try again later.");
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return true;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
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
