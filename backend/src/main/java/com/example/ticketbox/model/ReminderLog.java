package com.example.ticketbox.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "REMINDER_LOGS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reminder_log_seq")
    @SequenceGenerator(name = "reminder_log_seq", sequenceName = "REMINDER_LOG_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TICKET_ID", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EVENT_ID", nullable = false)
    private Event event;

    @Column(name = "SENT_AT", nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}
