package com.example.ticketbox.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TicketTransferResponse {
    private Long id;
    private Long ticketId;
    private String ticketCode;
    private String eventTitle;
    private String eventDate;
    private String fromUserName;
    private String fromUserEmail;
    private String toEmail;
    private String toUserName;
    private String transferToken;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdDate;
}
