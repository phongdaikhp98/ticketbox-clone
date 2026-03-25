package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.dto.SeatResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.Seat;
import com.example.ticketbox.model.SeatStatus;
import com.example.ticketbox.repository.SeatRepository;
import com.example.ticketbox.repository.UserRepository;
import com.example.ticketbox.service.SeatReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/seat-reservations")
@RequiredArgsConstructor
public class SeatReservationController {

    private final SeatReservationService reservationService;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    @PostMapping("/{seatId}/reserve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SeatResponse>> reserveSeat(
            @PathVariable Long seatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", seatId));

        if (seat.getStatus() == SeatStatus.SOLD) {
            throw new BadRequestException("Ghế đã được bán");
        }
        if (seat.getStatus() == SeatStatus.BLOCKED) {
            throw new BadRequestException("Ghế đang bị khóa");
        }

        boolean reserved = reservationService.reserveSeat(seatId, userId);
        if (!reserved) {
            throw new BadRequestException("Ghế đang được người khác giữ chỗ");
        }

        SeatResponse response = SeatResponse.builder()
                .id(seat.getId())
                .seatCode(seat.getSeatCode())
                .rowLabel(seat.getRowLabel())
                .seatNumber(seat.getSeatNumber())
                .status("RESERVED")
                .reservedByMe(true)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Giữ chỗ thành công", response));
    }

    @DeleteMapping("/{seatId}/release")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> releaseSeat(
            @PathVariable Long seatId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);

        if (!reservationService.isReservedByUser(seatId, userId)) {
            throw new BadRequestException("Bạn không giữ chỗ này");
        }

        reservationService.releaseSeat(seatId);
        return ResponseEntity.ok(ApiResponse.success("Hủy giữ chỗ thành công", null));
    }

    private Long getUserId(UserDetails userDetails) {
        // [SECURITY] Do not echo username into exception message (L3)
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }
}
