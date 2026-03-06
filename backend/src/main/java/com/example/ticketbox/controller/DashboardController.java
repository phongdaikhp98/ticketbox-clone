package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.dto.AttendeeResponse;
import com.example.ticketbox.dto.DashboardOverviewResponse;
import com.example.ticketbox.dto.EventStatsResponse;
import com.example.ticketbox.model.TicketStatus;
import com.example.ticketbox.security.UserDetailsImpl;
import com.example.ticketbox.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/organizer/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        boolean isAdmin = isAdminRole(userDetails);
        DashboardOverviewResponse data = dashboardService.getOverview(userDetails.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/events/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventStatsResponse>> getEventStats(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        boolean isAdmin = isAdminRole(userDetails);
        EventStatsResponse data = dashboardService.getEventStats(eventId, userDetails.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/events/{eventId}/attendees")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AttendeeResponse>>> getAttendees(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        boolean isAdmin = isAdminRole(userDetails);
        Page<AttendeeResponse> data = dashboardService.getAttendees(
                eventId, userDetails.getId(), isAdmin, status, search, page, size);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    private boolean isAdminRole(UserDetailsImpl userDetails) {
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
