package com.example.ticketbox.service;

import com.example.ticketbox.dto.AuthResponse;
import com.example.ticketbox.dto.ChangePasswordRequest;
import com.example.ticketbox.dto.UpdateProfileRequest;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.AuthProvider;
import com.example.ticketbox.model.User;
import com.example.ticketbox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    // [SECURITY] Whitelist trusted domains for avatarUrl — prevents XSS / SSRF (H2)
    private static final List<String> TRUSTED_AVATAR_PREFIXES = List.of(
            "https://res.cloudinary.com/",       // Cloudinary uploads
            "https://lh3.googleusercontent.com/" // Google OAuth profile photos
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse.UserDto updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getAvatarUrl() != null) {
            if (!request.getAvatarUrl().isEmpty() && TRUSTED_AVATAR_PREFIXES.stream()
                    .noneMatch(request.getAvatarUrl()::startsWith)) {
                throw new BadRequestException("Avatar URL không hợp lệ. Vui lòng upload ảnh qua hệ thống.");
            }
            user.setAvatarUrl(request.getAvatarUrl());
        }

        userRepository.save(user);

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

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Password change is not available for social login accounts");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
