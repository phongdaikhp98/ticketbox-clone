package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.dto.CheckInRequest;
import com.example.ticketbox.dto.CheckInResponse;
import com.example.ticketbox.dto.TicketResponse;
import com.example.ticketbox.model.TicketStatus;
import com.example.ticketbox.security.UserDetailsImpl;
import com.example.ticketbox.service.TicketPdfService;
import com.example.ticketbox.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketPdfService ticketPdfService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> getMyTickets(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<TicketResponse> tickets = ticketService.getMyTickets(userDetails.getId(), eventId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketDetail(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        TicketResponse ticket = ticketService.getTicketDetail(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> getTicketQr(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        byte[] qrImage = ticketService.getTicketQrImage(userDetails.getId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadTicketPdf(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long id) {
        byte[] pdf = ticketPdfService.generateTicketPdf(userDetails.getId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket-" + id + ".pdf")
                .body(pdf);
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CheckInResponse>> checkIn(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CheckInRequest request) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        CheckInResponse response;
        if (isAdmin) {
            response = ticketService.checkInAsAdmin(request.getTicketCode(), userDetails.getId());
        } else {
            response = ticketService.checkIn(request.getTicketCode(), userDetails.getId());
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
