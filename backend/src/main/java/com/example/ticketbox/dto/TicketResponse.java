package com.example.ticketbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private Long id;
    private String ticketCode;
    private String status;
    private String ticketTypeName;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String eventLocation;
    private String eventImageUrl;
    private LocalDateTime usedAt;
    private LocalDateTime createdDate;
}
