package com.example.ticketbox.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String email, String role) {
        return buildToken(email, role, accessTokenExpirationMs, "ACCESS", UUID.randomUUID().toString());
    }

    public String generateRefreshToken(String email) {
        String jti = UUID.randomUUID().toString();
        return buildToken(email, null, refreshTokenExpirationMs, "REFRESH", jti);
    }

    private String buildToken(String email, String role, long expirationMs, String tokenType, String jti) {
        JwtBuilder builder = Jwts.builder()
                .id(jti)
                .subject(email)
                .claim("type", tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey());

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    public String getJtiFromToken(String token) {
        return parseToken(token).getPayload().getId();
    }

    public String getEmailFromToken(String token) {
        return parseToken(token).getPayload().getSubject();
    }

    public String getTokenType(String token) {
        return parseToken(token).getPayload().get("type", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
    }
}
