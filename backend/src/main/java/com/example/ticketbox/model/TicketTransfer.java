package com.example.ticketbox.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TICKET_TRANSFERS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_transfer_seq")
    @SequenceGenerator(name = "ticket_transfer_seq", sequenceName = "TICKET_TRANSFER_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TICKET_ID", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FROM_USER_ID", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TO_USER_ID")
    private User toUser;

    @Column(name = "TO_EMAIL", nullable = false, length = 255)
    private String toEmail;

    @Column(name = "TRANSFER_TOKEN", nullable = false, unique = true, length = 100)
    private String transferToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private TicketTransferStatus status = TicketTransferStatus.PENDING;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;

    @Column(name = "CREATED_DATE", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
    }
}
