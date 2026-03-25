package com.example.ticketbox.service;

import com.example.ticketbox.config.AppProperties;
import com.example.ticketbox.dto.*;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.model.AuthProvider;
import com.example.ticketbox.model.Role;
import com.example.ticketbox.model.User;
import com.example.ticketbox.repository.UserRepository;
import com.example.ticketbox.security.JwtUtils;
import com.example.ticketbox.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final AppProperties appProperties;

    @Value("${app.mail.frontend-url}")
    private String frontendUrl;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshTokenExpirationMs;

    private static final String PWD_RESET_PREFIX      = "pwd_reset:";
    private static final String EMAIL_VERIFY_PREFIX   = "email_verify:";
    private static final String REFRESH_TOKEN_PREFIX  = "refresh_token:";

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);

        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());
        storeRefreshToken(refreshToken, user.getEmail());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (existingUser != null && existingUser.getProvider() == AuthProvider.GOOGLE) {
            throw new BadRequestException("Tài khoản này đăng nhập qua Google. Vui lòng dùng nút 'Đăng nhập với Google'.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());
        storeRefreshToken(refreshToken, user.getEmail());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (!jwtUtils.validateToken(token) || !"REFRESH".equals(jwtUtils.getTokenType(token))) {
            throw new BadRequestException("Invalid refresh token");
        }

        // [SECURITY] Kiểm tra jti trong Redis — nếu bị revoke hoặc đã dùng thì từ chối
        String jti = jwtUtils.getJtiFromToken(token);
        String storedEmail = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + jti);
        if (storedEmail == null) {
            throw new BadRequestException("Refresh token đã hết hạn hoặc đã bị thu hồi.");
        }

        // [SECURITY] Xóa token cũ (rotation) để tránh reuse sau khi issue token mới
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + jti);

        String email = jwtUtils.getEmailFromToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        String newAccessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getEmail());
        storeRefreshToken(newRefreshToken, user.getEmail());

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        try {
            if (refreshToken != null && jwtUtils.validateToken(refreshToken)
                    && "REFRESH".equals(jwtUtils.getTokenType(refreshToken))) {
                String jti = jwtUtils.getJtiFromToken(refreshToken);
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + jti);
                log.info("Refresh token revoked (jti={})", jti);
            }
        } catch (Exception e) {
            log.warn("Logout: could not revoke token: {}", e.getMessage());
        }
    }

    public AuthResponse.UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
        return toUserDto(user);
    }

    // ===================== Forgot / Reset Password =====================

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        // [SECURITY] Silent return — không tiết lộ email có tồn tại hay không,
        // kể cả tài khoản Google (tránh user enumeration qua error message)
        if (user == null || user.getProvider() == AuthProvider.GOOGLE) return;

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(PWD_RESET_PREFIX + token, email,
                Duration.ofMinutes(appProperties.getAuth().getPasswordResetTtlMinutes()));

        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(email, user.getFullName(), resetUrl);
        log.info("Password reset token issued for {}", email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String email = redisTemplate.opsForValue().get(PWD_RESET_PREFIX + token);
        if (email == null) {
            throw new BadRequestException("Token không hợp lệ hoặc đã hết hạn.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        redisTemplate.delete(PWD_RESET_PREFIX + token);
        log.info("Password reset successfully for {}", email);
    }

    // ===================== Email Verification =====================

    @Transactional
    public void sendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email đã được xác thực.");
        }
        if (user.getProvider() == AuthProvider.GOOGLE) {
            throw new BadRequestException("Tài khoản Google không cần xác thực email.");
        }

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(EMAIL_VERIFY_PREFIX + token, email,
                Duration.ofMinutes(appProperties.getAuth().getEmailVerifyTtlMinutes()));

        String verifyUrl = frontendUrl + "/verify-email?token=" + token;
        emailService.sendEmailVerificationEmail(email, user.getFullName(), verifyUrl);
        log.info("Email verification token issued for {}", email);
    }

    @Transactional
    public void verifyEmail(String token) {
        String email = redisTemplate.opsForValue().get(EMAIL_VERIFY_PREFIX + token);
        if (email == null) {
            throw new BadRequestException("Link xác thực không hợp lệ hoặc đã hết hạn.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        user.setEmailVerified(true);
        userRepository.save(user);
        redisTemplate.delete(EMAIL_VERIFY_PREFIX + token);
        log.info("Email verified successfully for {}", email);
    }

    // ===================== Google OAuth =====================

    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        Map<String, Object> tokenInfo = verifyGoogleToken(idToken);

        String email = (String) tokenInfo.get("email");
        String name = (String) tokenInfo.getOrDefault("name", email);
        String picture = (String) tokenInfo.get("picture");

        if (email == null) {
            throw new BadRequestException("Không lấy được email từ tài khoản Google.");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = User.builder()
                    .email(email)
                    .password(null)
                    .fullName(name)
                    .avatarUrl(picture)
                    .provider(AuthProvider.GOOGLE)
                    .role(Role.CUSTOMER)
                    .emailVerified(true)
                    .build();
            userRepository.save(user);
            log.info("New user created via Google OAuth: {}", email);
        } else if (user.getProvider() != AuthProvider.GOOGLE) {
            throw new BadRequestException("Email này đã được đăng ký bằng mật khẩu. Vui lòng đăng nhập bằng email/mật khẩu.");
        }

        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail());
        storeRefreshToken(refreshToken, user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    private Map<String, Object> verifyGoogleToken(String idToken) {
        try {
            RestClient restClient = RestClient.create();
            Map<String, Object> tokenInfo = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (tokenInfo == null) {
                throw new BadRequestException("Token Google không hợp lệ.");
            }

            if (tokenInfo.containsKey("error_description")) {
                throw new BadRequestException("Token Google không hợp lệ: " + tokenInfo.get("error_description"));
            }

            // [SECURITY] Luôn validate audience — reject nếu chưa cấu hình GOOGLE_CLIENT_ID
            if (googleClientId == null || googleClientId.isBlank()) {
                throw new BadRequestException("Google OAuth chưa được cấu hình trên server.");
            }
            String aud = (String) tokenInfo.get("aud");
            if (!googleClientId.equals(aud)) {
                throw new BadRequestException("Token Google không đúng ứng dụng.");
            }

            return tokenInfo;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google token verification failed: {}", e.getMessage());
            throw new BadRequestException("Token Google không hợp lệ.");
        }
    }

    // ===================== Helpers =====================

    private void storeRefreshToken(String refreshToken, String email) {
        String jti = jwtUtils.getJtiFromToken(refreshToken);
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + jti, email,
                Duration.ofMillis(refreshTokenExpirationMs)
        );
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(toUserDto(user))
                .build();
    }

    private AuthResponse.UserDto toUserDto(User user) {
        return AuthResponse.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .emailVerified(user.getEmailVerified())
                .build();
    }
}
