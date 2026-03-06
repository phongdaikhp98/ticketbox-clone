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
public class AttendeeResponse {

    private Long ticketId;
    private String ticketCode;
    private String attendeeName;
    private String attendeeEmail;
    private String ticketTypeName;
    private String status;
    private LocalDateTime usedAt;
    private LocalDateTime createdDate;
}
