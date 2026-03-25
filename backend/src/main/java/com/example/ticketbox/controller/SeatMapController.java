package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.dto.CreateSeatMapRequest;
import com.example.ticketbox.dto.SeatMapResponse;
import com.example.ticketbox.dto.UpdateSeatStatusRequest;
import com.example.ticketbox.model.SeatStatus;
import com.example.ticketbox.service.SeatMapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.ticketbox.repository.UserRepository;
import com.example.ticketbox.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/seat-maps")
@RequiredArgsConstructor
public class SeatMapController {

    private final SeatMapService seatMapService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SeatMapResponse>> createSeatMap(
            @Valid @RequestBody CreateSeatMapRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        SeatMapResponse response = seatMapService.createSeatMap(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Seat map created", response));
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<SeatMapResponse>> getSeatMapByEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userDetails != null ? getUserId(userDetails) : null;
        SeatMapResponse response = seatMapService.getSeatMapByEvent(eventId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{seatMapId}/seats/{seatId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateSeatStatus(
            @PathVariable Long seatMapId,
            @PathVariable Long seatId,
            @Valid @RequestBody UpdateSeatStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (request.getStatus() == SeatStatus.SOLD) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("400", "Cannot manually set seat to SOLD"));
        }
        Long userId = getUserId(userDetails);
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        seatMapService.updateSeatStatus(seatId, request.getStatus(), userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Seat status updated", null));
    }

    @DeleteMapping("/{seatMapId}")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSeatMap(
            @PathVariable Long seatMapId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        seatMapService.deleteSeatMap(seatMapId, userId);
        return ResponseEntity.ok(ApiResponse.success("Seat map deleted", null));
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userDetails.getUsername()))
                .getId();
    }
}
