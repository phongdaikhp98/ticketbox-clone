package com.example.ticketbox.service;

import com.example.ticketbox.dto.TicketTransferResponse;
import com.example.ticketbox.dto.TransferTicketRequest;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.TicketRepository;
import com.example.ticketbox.repository.TicketTransferRepository;
import com.example.ticketbox.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketTransferService {

    private final TicketRepository ticketRepository;
    private final TicketTransferRepository transferRepository;
    private final UserRepository userRepository;

    @Transactional
    public TicketTransferResponse initiateTransfer(Long ticketId, Long fromUserId, TransferTicketRequest request) {
        Ticket ticket = ticketRepository.findByIdAndUserId(ticketId, fromUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        if (ticket.getStatus() != TicketStatus.ISSUED) {
            throw new BadRequestException("Chỉ có thể chuyển nhượng vé chưa sử dụng");
        }

        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", fromUserId));

        if (fromUser.getEmail().equalsIgnoreCase(request.getToEmail())) {
            throw new BadRequestException("Không thể chuyển nhượng vé cho chính mình");
        }

        if (transferRepository.existsByTicketIdAndStatus(ticketId, TicketTransferStatus.PENDING)) {
            throw new BadRequestException("Vé này đang có yêu cầu chuyển nhượng chờ xử lý");
        }

        TicketTransfer transfer = TicketTransfer.builder()
                .ticket(ticket)
                .fromUser(fromUser)
                .toEmail(request.getToEmail().toLowerCase())
                .transferToken(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();

        return toResponse(transferRepository.save(transfer));
    }

    public TicketTransferResponse getByToken(String token) {
        TicketTransfer transfer = transferRepository.findByTransferToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));
        // [SECURITY] Mask PII — caller is not necessarily the owner (L1)
        return toPublicResponse(transfer);
    }

    @Transactional
    public TicketTransferResponse acceptTransfer(String token, Long acceptingUserId) {
        // [SECURITY] Pessimistic lock prevents concurrent accept+cancel race condition (H1)
        TicketTransfer transfer = transferRepository.findByTransferTokenForUpdate(token)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found"));

        if (transfer.getStatus() != TicketTransferStatus.PENDING) {
            throw new BadRequestException("Yêu cầu chuyển nhượng này không còn hiệu lực");
        }

        if (transfer.getExpiresAt().isBefore(LocalDateTime.now())) {
            transfer.setStatus(TicketTransferStatus.EXPIRED);
            transferRepository.save(transfer);
            throw new BadRequestException("Yêu cầu chuyển nhượng đã hết hạn");
        }

        User acceptingUser = userRepository.findById(acceptingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", acceptingUserId));

        if (!acceptingUser.getEmail().equalsIgnoreCase(transfer.getToEmail())) {
            throw new BadRequestException("Email của bạn không khớp với yêu cầu chuyển nhượng này");
        }

        Ticket ticket = transfer.getTicket();
        ticket.setUser(acceptingUser);
        ticketRepository.save(ticket);

        transfer.setToUser(acceptingUser);
        transfer.setStatus(TicketTransferStatus.COMPLETED);
        transfer.setCompletedAt(LocalDateTime.now());

        return toResponse(transferRepository.save(transfer));
    }

    @Transactional
    public void cancelTransfer(Long transferId, Long userId) {
        // [SECURITY] Pessimistic lock prevents concurrent accept+cancel race condition (H1)
        TicketTransfer transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer", transferId));

        if (!transfer.getFromUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền hủy yêu cầu này");
        }

        if (transfer.getStatus() != TicketTransferStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy yêu cầu đang chờ xử lý");
        }

        transfer.setStatus(TicketTransferStatus.CANCELLED);
        transferRepository.save(transfer);
    }

    public List<TicketTransferResponse> getMyTransfers(Long userId) {
        return transferRepository.findByFromUserIdOrderByCreatedDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TicketTransferResponse toResponse(TicketTransfer t) {
        Ticket ticket = t.getTicket();
        return TicketTransferResponse.builder()
                .id(t.getId())
                .ticketId(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .eventTitle(ticket.getEvent().getTitle())
                .eventDate(ticket.getEvent().getEventDate().toString())
                .fromUserName(t.getFromUser().getFullName())
                .fromUserEmail(t.getFromUser().getEmail())
                .toEmail(t.getToEmail())
                .toUserName(t.getToUser() != null ? t.getToUser().getFullName() : null)
                .transferToken(t.getTransferToken())
                .status(t.getStatus().name())
                .expiresAt(t.getExpiresAt())
                .completedAt(t.getCompletedAt())
                .createdDate(t.getCreatedDate())
                .build();
    }

    /** [SECURITY] Public view — masks sender email to prevent PII leak to non-owners (L1) */
    private TicketTransferResponse toPublicResponse(TicketTransfer t) {
        Ticket ticket = t.getTicket();
        return TicketTransferResponse.builder()
                .id(t.getId())
                .ticketId(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .eventTitle(ticket.getEvent().getTitle())
                .eventDate(ticket.getEvent().getEventDate().toString())
                .fromUserName(t.getFromUser().getFullName())
                .fromUserEmail(maskEmail(t.getFromUser().getEmail()))
                .toEmail(maskEmail(t.getToEmail()))
                .toUserName(t.getToUser() != null ? t.getToUser().getFullName() : null)
                .transferToken(t.getTransferToken())
                .status(t.getStatus().name())
                .expiresAt(t.getExpiresAt())
                .completedAt(t.getCompletedAt())
                .createdDate(t.getCreatedDate())
                .build();
    }

    /** Masks an email address: john.doe@gmail.com → j***@gmail.com */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIdx = email.indexOf('@');
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (local.length() <= 1) return local + "***" + domain;
        return local.charAt(0) + "***" + domain;
    }
}
