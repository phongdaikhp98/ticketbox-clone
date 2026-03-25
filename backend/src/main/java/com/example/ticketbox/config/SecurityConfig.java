package com.example.ticketbox.config;

import com.example.ticketbox.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    // [SECURITY] Đọc từ springdoc.swagger-ui.enabled — mặc định true (dev), false trên prod
    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerEnabled;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // [SECURITY] HTTP Strict Transport Security — tells browsers to only use HTTPS
                // for this domain for the next year (M2). includeSubDomains covers all subdomains.
                // Note: HSTS is only sent over HTTPS; browsers ignore it on plain HTTP responses.
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(false)
                        )
                )
                .authorizeHttpRequests(auth -> {
                        auth.requestMatchers("/v1/auth/register", "/v1/auth/login", "/v1/auth/refresh-token",
                                        "/v1/auth/forgot-password", "/v1/auth/reset-password", "/v1/auth/oauth2/google",
                                        "/v1/auth/verify-email", "/v1/auth/logout").permitAll();
                        // [SECURITY] Chỉ GET public — POST/PUT/DELETE yêu cầu xác thực
                        auth.requestMatchers(HttpMethod.GET, "/v1/events/**").permitAll();
                        auth.requestMatchers(HttpMethod.GET, "/v1/categories", "/v1/categories/**").permitAll();
                        auth.requestMatchers("/v1/payment/vnpay-ipn").permitAll();
                        // [SECURITY] Swagger chỉ public khi swaggerEnabled = true (dev).
                        // Production (application-prod.yml) đặt springdoc.swagger-ui.enabled=false
                        if (swaggerEnabled) {
                            auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();
                        }
                        auth.anyRequest().authenticated();
                })
                .addFilterBefore(rateLimitFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
