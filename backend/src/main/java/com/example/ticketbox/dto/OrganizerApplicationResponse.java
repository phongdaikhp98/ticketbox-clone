package com.example.ticketbox.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OrganizerApplicationResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String orgName;
    private String taxNumber;
    private String contactPhone;
    private String reason;
    private String status;
    private String reviewedByName;
    private String reviewNote;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdDate;
}
