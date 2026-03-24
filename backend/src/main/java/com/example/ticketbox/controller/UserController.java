package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.dto.AuthResponse;
import com.example.ticketbox.dto.ChangePasswordRequest;
import com.example.ticketbox.dto.UpdateProfileRequest;
import com.example.ticketbox.security.UserDetailsImpl;
import com.example.ticketbox.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AuthResponse.UserDto>> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        AuthResponse.UserDto updated = userService.updateProfile(userDetails.getEmail(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", updated));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getEmail(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
