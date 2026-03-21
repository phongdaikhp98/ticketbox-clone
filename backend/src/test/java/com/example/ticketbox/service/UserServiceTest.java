package com.example.ticketbox.service;

import com.example.ticketbox.dto.AuthResponse;
import com.example.ticketbox.dto.UpdateProfileRequest;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.Role;
import com.example.ticketbox.model.User;
import com.example.ticketbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UpdateProfileRequest updateRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encoded_password")
                .fullName("Nguyen Van A")
                .phone("0901234567")
                .address("Ha Noi")
                .avatarUrl("https://example.com/avatar.jpg")
                .role(Role.CUSTOMER)
                .isActive(true)
                .emailVerified(false)
                .build();

        updateRequest = new UpdateProfileRequest();
        updateRequest.setFullName("Nguyen Van B");
        updateRequest.setPhone("0987654321");
        updateRequest.setAddress("Ho Chi Minh");
        updateRequest.setAvatarUrl("https://example.com/new-avatar.jpg");
    }

    @Test
    void updateProfile_userExists_returnsUpdatedUserDto() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AuthResponse.UserDto result = userService.updateProfile("user@example.com", updateRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("user@example.com", result.getEmail());
        assertEquals("Nguyen Van B", result.getFullName());
        assertEquals("0987654321", result.getPhone());
        assertEquals("Ho Chi Minh", result.getAddress());
        assertEquals("https://example.com/new-avatar.jpg", result.getAvatarUrl());
        assertEquals("CUSTOMER", result.getRole());
        assertNotNull(result.getEmailVerified());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateProfile_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateProfile("unknown@example.com", updateRequest));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_nullFields_onlyNonNullFieldsAreUpdated() {
        UpdateProfileRequest partialRequest = new UpdateProfileRequest();
        partialRequest.setFullName("Only Name Updated");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.updateProfile("user@example.com", partialRequest);

        verify(userRepository).save(testUser);
        assertEquals("0901234567", testUser.getPhone());
        assertEquals("Ha Noi", testUser.getAddress());
        assertEquals("https://example.com/avatar.jpg", testUser.getAvatarUrl());
    }

    @Test
    void updateProfile_allNullFields_savesWithNoChanges() {
        UpdateProfileRequest emptyRequest = new UpdateProfileRequest();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AuthResponse.UserDto result = userService.updateProfile("user@example.com", emptyRequest);

        assertNotNull(result);
        assertEquals("Nguyen Van A", testUser.getFullName());
        assertEquals("0901234567", testUser.getPhone());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateProfile_adminUser_returnsCorrectRole() {
        User adminUser = User.builder()
                .id(2L)
                .email("admin@example.com")
                .password("encoded")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .isActive(true)
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Admin Updated");

        AuthResponse.UserDto result = userService.updateProfile("admin@example.com", req);

        assertEquals("ADMIN", result.getRole());
        assertEquals(true, result.getEmailVerified());
    }
}
