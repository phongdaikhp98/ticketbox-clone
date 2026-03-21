package com.example.ticketbox.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ORGANIZER_APPLICATIONS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organizer_app_seq")
    @SequenceGenerator(name = "organizer_app_seq", sequenceName = "ORGANIZER_APP_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(name = "ORG_NAME", nullable = false, length = 255)
    private String orgName;

    @Column(name = "TAX_NUMBER", nullable = false, length = 20)
    private String taxNumber;

    @Column(name = "CONTACT_PHONE", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "REASON", columnDefinition = "CLOB")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REVIEWED_BY")
    private User reviewedBy;

    @Column(name = "REVIEW_NOTE", length = 1000)
    private String reviewNote;

    @Column(name = "SUBMITTED_AT", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "REVIEWED_AT")
    private LocalDateTime reviewedAt;

    @Column(name = "CREATED_DATE", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        submittedAt = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
