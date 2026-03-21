package com.example.ticketbox.service;

import com.example.ticketbox.dto.AuthResponse;
import com.example.ticketbox.dto.LoginRequest;
import com.example.ticketbox.dto.RefreshTokenRequest;
import com.example.ticketbox.dto.RegisterRequest;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.model.AuthProvider;
import com.example.ticketbox.model.Role;
import com.example.ticketbox.model.User;
import com.example.ticketbox.repository.UserRepository;
import com.example.ticketbox.security.JwtUtils;
import com.example.ticketbox.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtils jwtUtils;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @SuppressWarnings("unchecked")
    @Mock
    private ValueOperations<String, String> valueOperations;

    private User testUser;
    private User googleUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(authService, "googleClientId", "");

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded_password")
                .fullName("Test User")
                .phone("0901234567")
                .address("Ha Noi")
                .role(Role.CUSTOMER)
                .isActive(true)
                .emailVerified(false)
                .provider(AuthProvider.LOCAL)
                .build();

        googleUser = User.builder()
                .id(2L)
                .email("google@example.com")
                .password(null)
                .fullName("Google User")
                .role(Role.CUSTOMER)
                .isActive(true)
                .emailVerified(true)
                .provider(AuthProvider.GOOGLE)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("123456");
        registerRequest.setFullName("Test User");
        registerRequest.setPhone("0901234567");
        registerRequest.setAddress("Ha Noi");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("123456");
    }

    // ===================== Register =====================

    @Test
    void register_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtils.generateAccessToken(anyString(), anyString())).thenReturn("access_token");
        when(jwtUtils.generateRefreshToken(anyString())).thenReturn("refresh_token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("refresh_token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("test@example.com", response.getUser().getEmail());
        assertEquals("Test User", response.getUser().getFullName());
        assertEquals("CUSTOMER", response.getUser().getRole());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_emailAlreadyExists_throwsBadRequest() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authService.register(registerRequest));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    // ===================== Login =====================

    @Test
    void login_success() {
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateAccessToken(anyString(), anyString())).thenReturn("access_token");
        when(jwtUtils.generateRefreshToken(anyString())).thenReturn("refresh_token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("test@example.com", response.getUser().getEmail());
    }

    @Test
    void login_badCredentials_throwsException() {
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.login(loginRequest));
    }

    @Test
    void login_googleAccount_throwsBadRequest() {
        loginRequest.setEmail("google@example.com");
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(googleUser));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.login(loginRequest));

        assertTrue(ex.getMessage().contains("Google"));
        verify(authenticationManager, never()).authenticate(any());
    }

    // ===================== Refresh Token =====================

    @Test
    void refreshToken_success() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid_refresh_token");

        when(jwtUtils.validateToken("valid_refresh_token")).thenReturn(true);
        when(jwtUtils.getTokenType("valid_refresh_token")).thenReturn("REFRESH");
        when(jwtUtils.getEmailFromToken("valid_refresh_token")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateAccessToken(anyString(), anyString())).thenReturn("new_access_token");
        when(jwtUtils.generateRefreshToken(anyString())).thenReturn("new_refresh_token");

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new_access_token", response.getAccessToken());
        assertEquals("new_refresh_token", response.getRefreshToken());
    }

    @Test
    void refreshToken_invalidToken_throwsBadRequest() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid_token");

        when(jwtUtils.validateToken("invalid_token")).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authService.refreshToken(request));

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    void refreshToken_accessTokenUsedAsRefresh_throwsBadRequest() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("access_token");

        when(jwtUtils.validateToken("access_token")).thenReturn(true);
        when(jwtUtils.getTokenType("access_token")).thenReturn("ACCESS");

        assertThrows(BadRequestException.class, () -> authService.refreshToken(request));
    }

    // ===================== Get Current User =====================

    @Test
    void getCurrentUser_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        AuthResponse.UserDto userDto = authService.getCurrentUser("test@example.com");

        assertNotNull(userDto);
        assertEquals(1L, userDto.getId());
        assertEquals("test@example.com", userDto.getEmail());
        assertEquals("Test User", userDto.getFullName());
        assertEquals("0901234567", userDto.getPhone());
        assertEquals("Ha Noi", userDto.getAddress());
    }

    @Test
    void getCurrentUser_notFound_throwsBadRequest() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> authService.getCurrentUser("unknown@example.com"));
    }

    // ===================== Forgot Password =====================

    @Test
    void forgotPassword_emailNotFound_shouldDoNothing() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.forgotPassword("notfound@example.com"));

        verify(redisTemplate, never()).opsForValue();
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPassword_googleAccount_throwsBadRequest() {
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(googleUser));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.forgotPassword("google@example.com"));

        assertTrue(ex.getMessage().contains("Google"));
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void forgotPassword_success_shouldStoreTokenAndSendEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertDoesNotThrow(() -> authService.forgotPassword("test@example.com"));

        verify(valueOperations).set(
                argThat(key -> key.startsWith("pwd_reset:")),
                eq("test@example.com"),
                eq(Duration.ofMinutes(15))
        );
        verify(emailService).sendPasswordResetEmail(
                eq("test@example.com"),
                eq("Test User"),
                argThat(url -> url.contains("/reset-password?token="))
        );
    }

    // ===================== Reset Password =====================

    @Test
    void resetPassword_invalidToken_throwsBadRequest() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("pwd_reset:bad_token")).thenReturn(null);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> authService.resetPassword("bad_token", "newpass123"));

        assertTrue(ex.getMessage().contains("Token"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_success_shouldUpdatePasswordAndDeleteToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("pwd_reset:valid_token")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded_new");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        assertDoesNotThrow(() -> authService.resetPassword("valid_token", "newpass123"));

        assertEquals("encoded_new", testUser.getPassword());
        verify(userRepository).save(testUser);
        verify(redisTemplate).delete("pwd_reset:valid_token");
    }

    @Test
    void resetPassword_userNotFound_throwsBadRequest() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("pwd_reset:orphan_token")).thenReturn("deleted@example.com");
        when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> authService.resetPassword("orphan_token", "newpass"));

        verify(userRepository, never()).save(any());
    }

    // ===================== Login with Google =====================

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void loginWithGoogle_newUser_shouldCreateAndReturnTokens() {
        Map<String, Object> tokenInfo = Map.of(
                "email", "new@google.com",
                "name", "New Google User",
                "picture", "https://photo.google.com/photo.jpg"
        );

        try (MockedStatic<RestClient> mocked = mockStatic(RestClient.class)) {
            RestClient mockClient = mock(RestClient.class);
            RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec requestSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            mocked.when(RestClient::create).thenReturn(mockClient);
            when(mockClient.get()).thenReturn(getSpec);
            doReturn(requestSpec).when(getSpec).uri(anyString(), any(Object.class));
            when(requestSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(tokenInfo);

            when(userRepository.findByEmail("new@google.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtUtils.generateAccessToken(anyString(), anyString())).thenReturn("access_token");
            when(jwtUtils.generateRefreshToken(anyString())).thenReturn("refresh_token");

            AuthResponse response = authService.loginWithGoogle("google_id_token");

            assertNotNull(response);
            assertEquals("access_token", response.getAccessToken());
            assertEquals("new@google.com", response.getUser().getEmail());
            verify(userRepository).save(argThat(u ->
                    u.getProvider() == AuthProvider.GOOGLE && Boolean.TRUE.equals(u.getEmailVerified())
            ));
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void loginWithGoogle_existingGoogleUser_shouldReturnTokens() {
        Map<String, Object> tokenInfo = Map.of(
                "email", "google@example.com",
                "name", "Google User"
        );

        try (MockedStatic<RestClient> mocked = mockStatic(RestClient.class)) {
            RestClient mockClient = mock(RestClient.class);
            RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec requestSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            mocked.when(RestClient::create).thenReturn(mockClient);
            when(mockClient.get()).thenReturn(getSpec);
            doReturn(requestSpec).when(getSpec).uri(anyString(), any(Object.class));
            when(requestSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(tokenInfo);

            when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(googleUser));
            when(jwtUtils.generateAccessToken(anyString(), anyString())).thenReturn("access_token");
            when(jwtUtils.generateRefreshToken(anyString())).thenReturn("refresh_token");

            AuthResponse response = authService.loginWithGoogle("google_id_token");

            assertNotNull(response);
            assertEquals("google@example.com", response.getUser().getEmail());
            // Existing user — no new save
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void loginWithGoogle_existingLocalUser_throwsBadRequest() {
        Map<String, Object> tokenInfo = Map.of(
                "email", "test@example.com",
                "name", "Test User"
        );

        try (MockedStatic<RestClient> mocked = mockStatic(RestClient.class)) {
            RestClient mockClient = mock(RestClient.class);
            RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec requestSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            mocked.when(RestClient::create).thenReturn(mockClient);
            when(mockClient.get()).thenReturn(getSpec);
            doReturn(requestSpec).when(getSpec).uri(anyString(), any(Object.class));
            when(requestSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(tokenInfo);

            // testUser has provider = LOCAL
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> authService.loginWithGoogle("google_id_token"));

            assertTrue(ex.getMessage().contains("mật khẩu"));
            verify(userRepository, never()).save(any());
        }
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void loginWithGoogle_invalidToken_throwsBadRequest() {
        try (MockedStatic<RestClient> mocked = mockStatic(RestClient.class)) {
            RestClient mockClient = mock(RestClient.class);
            RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
            RestClient.RequestHeadersSpec requestSpec = mock(RestClient.RequestHeadersSpec.class);
            RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

            mocked.when(RestClient::create).thenReturn(mockClient);
            when(mockClient.get()).thenReturn(getSpec);
            doReturn(requestSpec).when(getSpec).uri(anyString(), any(Object.class));
            when(requestSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.body(any(ParameterizedTypeReference.class)))
                    .thenThrow(new RuntimeException("Google API error"));

            assertThrows(BadRequestException.class,
                    () -> authService.loginWithGoogle("invalid_token"));
        }
    }
}
