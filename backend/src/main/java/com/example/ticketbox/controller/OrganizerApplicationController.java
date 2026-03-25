package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.dto.OrganizerApplicationRequest;
import com.example.ticketbox.dto.OrganizerApplicationResponse;
import com.example.ticketbox.security.UserDetailsImpl;
import com.example.ticketbox.service.OrganizerApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/v1/organizer-applications")
@RequiredArgsConstructor
public class OrganizerApplicationController {

    private final OrganizerApplicationService organizerApplicationService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')") // [SECURITY] Chỉ CUSTOMER mới được nộp đơn (L1)
    public ResponseEntity<ApiResponse<OrganizerApplicationResponse>> submit(
            @Valid @RequestBody OrganizerApplicationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        OrganizerApplicationResponse response = organizerApplicationService.submit(userId, request);
        return ResponseEntity.status(201).body(ApiResponse.created(response));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrganizerApplicationResponse>> getMyApplication(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getId();
        Optional<OrganizerApplicationResponse> response = organizerApplicationService.getMyApplication(userId);
        return ResponseEntity.ok(ApiResponse.success(response.orElse(null)));
    }
}
