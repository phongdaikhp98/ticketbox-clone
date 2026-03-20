package com.example.ticketbox.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private Long adminId;
    private String adminName;
    private String adminEmail;
    private String action;
    private String entityType;
    private Long entityId;
    private String entityName;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdDate;
}
