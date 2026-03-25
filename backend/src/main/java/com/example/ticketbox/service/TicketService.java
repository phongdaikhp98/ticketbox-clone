package com.example.ticketbox.service;

import com.example.ticketbox.dto.CheckInResponse;
import com.example.ticketbox.dto.TicketResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.EventRepository;
import com.example.ticketbox.repository.TicketRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final QrCodeService qrCodeService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional
    public List<Ticket> generateTickets(Order order) {
        List<Ticket> tickets = new ArrayList<>();

        for (OrderItem item : order.getOrderItems()) {
            for (int i = 0; i < item.getQuantity(); i++) {
                String ticketCode = generateTicketCode();
                String qrData = buildQrData(ticketCode, item.getEvent().getId(), order.getUser().getId());

                String seatCode = item.getSeat() != null ? item.getSeat().getSeatCode() : null;

                Ticket ticket = Ticket.builder()
                        .orderItem(item)
                        .user(order.getUser())
                        .event(item.getEvent())
                        .ticketType(item.getTicketType())
                        .seat(item.getSeat())
                        .seatCode(seatCode)
                        .ticketCode(ticketCode)
                        .qrData(qrData)
                        .build();

                tickets.add(ticketRepository.save(ticket));
            }
        }

        return tickets;
    }

    public Page<TicketResponse> getMyTickets(Long userId, Long eventId, TicketStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<Ticket> ticketPage;

        if (eventId != null && status != null) {
            ticketPage = ticketRepository.findByUserIdAndEventIdAndStatus(userId, eventId, status, pageable);
        } else if (eventId != null) {
            ticketPage = ticketRepository.findByUserIdAndEventId(userId, eventId, pageable);
        } else if (status != null) {
            ticketPage = ticketRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            ticketPage = ticketRepository.findByUserId(userId, pageable);
        }

        return ticketPage.map(this::toTicketResponse);
    }

    public TicketResponse getTicketDetail(Long userId, Long ticketId) {
        Ticket ticket = ticketRepository.findByIdAndUserId(ticketId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        return toTicketResponse(ticket);
    }

    public byte[] getTicketQrImage(Long userId, Long ticketId) {
        Ticket ticket = ticketRepository.findByIdAndUserId(ticketId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        return qrCodeService.generateQrImage(ticket.getQrData(), 300, 300);
    }

    @Transactional
    public CheckInResponse checkIn(String ticketCode, Long organizerId) {
        // [SECURITY] Pessimistic lock prevents two concurrent check-ins from
        // both reading ISSUED and both marking the ticket USED (H1)
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        // Verify organizer owns this event (or is ADMIN)
        Event event = ticket.getEvent();
        if (!event.getOrganizer().getId().equals(organizerId)) {
            // Check if user is ADMIN — caller should handle this via @PreAuthorize or pass role
            throw new BadRequestException("You are not the organizer of this event");
        }

        if (ticket.getStatus() == TicketStatus.USED) {
            return CheckInResponse.builder()
                    .ticketCode(ticketCode)
                    .status("ALREADY_USED")
                    .message("This ticket has already been used at " + ticket.getUsedAt())
                    .eventTitle(event.getTitle())
                    .attendeeName(ticket.getUser().getFullName())
                    .ticketTypeName(ticket.getTicketType().getName())
                    .build();
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            return CheckInResponse.builder()
                    .ticketCode(ticketCode)
                    .status("CANCELLED")
                    .message("This ticket has been cancelled")
                    .eventTitle(event.getTitle())
                    .attendeeName(ticket.getUser().getFullName())
                    .ticketTypeName(ticket.getTicketType().getName())
                    .build();
        }

        // Mark as USED
        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        auditLogService.log(organizerId, "CHECK_IN", "TICKET",
                ticket.getId(), ticketCode,
                "ISSUED", "USED");

        return CheckInResponse.builder()
                .ticketCode(ticketCode)
                .status("SUCCESS")
                .message("Check-in successful!")
                .eventTitle(event.getTitle())
                .attendeeName(ticket.getUser().getFullName())
                .ticketTypeName(ticket.getTicketType().getName())
                .build();
    }

    @Transactional
    public void cancelTicketsByOrderId(Long orderId) {
        List<Ticket> tickets = ticketRepository.findByOrderItemOrderId(orderId);
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() != TicketStatus.CANCELLED) {
                ticket.setStatus(TicketStatus.CANCELLED);
                ticketRepository.save(ticket);
            }
        }
    }

    // Allow ADMIN to check-in without organizer validation
    @Transactional
    public CheckInResponse checkInAsAdmin(String ticketCode, Long adminId) {
        // [SECURITY] Same pessimistic lock as checkIn() — prevents race on admin path (H1)
        Ticket ticket = ticketRepository.findByTicketCodeForUpdate(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        Event event = ticket.getEvent();

        if (ticket.getStatus() == TicketStatus.USED) {
            return CheckInResponse.builder()
                    .ticketCode(ticketCode)
                    .status("ALREADY_USED")
                    .message("This ticket has already been used at " + ticket.getUsedAt())
                    .eventTitle(event.getTitle())
                    .attendeeName(ticket.getUser().getFullName())
                    .ticketTypeName(ticket.getTicketType().getName())
                    .build();
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            return CheckInResponse.builder()
                    .ticketCode(ticketCode)
                    .status("CANCELLED")
                    .message("This ticket has been cancelled")
                    .eventTitle(event.getTitle())
                    .attendeeName(ticket.getUser().getFullName())
                    .ticketTypeName(ticket.getTicketType().getName())
                    .build();
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        auditLogService.log(adminId, "CHECK_IN", "TICKET",
                ticket.getId(), ticketCode,
                "ISSUED", "USED");

        return CheckInResponse.builder()
                .ticketCode(ticketCode)
                .status("SUCCESS")
                .message("Check-in successful!")
                .eventTitle(event.getTitle())
                .attendeeName(ticket.getUser().getFullName())
                .ticketTypeName(ticket.getTicketType().getName())
                .build();
    }

    private String generateTicketCode() {
        String date = LocalDateTime.now().format(DATE_FMT);
        StringBuilder code = new StringBuilder("TBX-");
        code.append(date).append("-");
        for (int i = 0; i < 6; i++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    /**
     * [SECURITY] Use Jackson ObjectMapper instead of string concatenation to build
     * QR JSON — prevents any future JSON injection if ticketCode source ever changes (L1).
     */
    private String buildQrData(String ticketCode, Long eventId, Long userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", ticketCode);
        data.put("eventId", eventId);
        data.put("userId", userId);
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            // Should never happen with simple String/Long values
            throw new IllegalStateException("Failed to serialize QR data", e);
        }
    }

    private TicketResponse toTicketResponse(Ticket ticket) {
        Event event = ticket.getEvent();
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .seatCode(ticket.getSeatCode())
                .status(ticket.getStatus().name())
                .ticketTypeName(ticket.getTicketType().getName())
                .eventTitle(event.getTitle())
                .eventDate(event.getEventDate())
                .eventLocation(event.getLocation())
                .eventImageUrl(event.getImageUrl())
                .usedAt(ticket.getUsedAt())
                .createdDate(ticket.getCreatedDate())
                .build();
    }
}
