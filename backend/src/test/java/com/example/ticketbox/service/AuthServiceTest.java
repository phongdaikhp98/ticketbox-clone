package com.example.ticketbox.service;

import com.example.ticketbox.dto.AuthResponse;
import com.example.ticketbox.dto.LoginRequest;
import com.example.ticketbox.dto.RefreshTokenRequest;
import com.example.ticketbox.dto.RegisterRequest;
import com.example.ticketbox.exception.BadRequestException;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
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

    @Test
    void login_success() {
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateAccessToken(anyString(), anyString())).thenReturn("access_token");
        when(jwtUtils.generateRefreshToken(anyString())).thenReturn("refresh_token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        assertEquals("test@example.com", response.getUser().getEmail());
    }

    @Test
    void login_badCredentials_throwsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.login(loginRequest));
    }

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
}
