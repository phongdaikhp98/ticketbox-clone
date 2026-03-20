package com.example.ticketbox.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "AUDIT_LOGS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_seq")
    @SequenceGenerator(name = "audit_log_seq", sequenceName = "AUDIT_LOGS_SEQ", allocationSize = 1)
    @Column(name = "LOG_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADMIN_ID", nullable = false)
    private User admin;

    @Column(name = "ACTION", length = 100, nullable = false)
    private String action;

    @Column(name = "ENTITY_TYPE", length = 50, nullable = false)
    private String entityType;

    @Column(name = "ENTITY_ID", nullable = false)
    private Long entityId;

    @Column(name = "ENTITY_NAME", length = 255)
    private String entityName;

    @Column(name = "OLD_VALUE", length = 500)
    private String oldValue;

    @Column(name = "NEW_VALUE", length = 500)
    private String newValue;

    @Column(name = "CREATED_DATE", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}
