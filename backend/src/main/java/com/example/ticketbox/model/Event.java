package com.example.ticketbox.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "EVENTS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_seq")
    @SequenceGenerator(name = "event_seq", sequenceName = "EVENT_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TITLE", nullable = false, length = 255)
    private String title;

    @Column(name = "DESCRIPTION", columnDefinition = "CLOB")
    private String description;

    @Column(name = "EVENT_DATE", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "END_DATE")
    private LocalDateTime endDate;

    @Column(name = "LOCATION", nullable = false, length = 500)
    private String location;

    @Column(name = "IMAGE_URL", length = 500)
    private String imageUrl;

    // Legacy enum column — kept read-only for data migration only, do not use in new code
    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORY", length = 50, insertable = false, updatable = false)
    private EventCategory categoryLegacy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORY_ID")
    private Category category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "EVENT_TAGS",
            joinColumns = @JoinColumn(name = "EVENT_ID"),
            inverseJoinColumns = @JoinColumn(name = "TAG_ID")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "IS_FEATURED", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    /** Display order in the featured carousel (lower = higher priority). Default 999 = unordered. */
    @Column(name = "FEATURED_ORDER", nullable = false)
    @Builder.Default
    private Integer featuredOrder = 999;

    @Column(name = "HAS_SEAT_MAP", nullable = false)
    @Builder.Default
    private boolean hasSeatMap = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORGANIZER_ID", nullable = false)
    private User organizer;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TicketType> ticketTypes = new ArrayList<>();

    @Column(name = "CREATED_DATE", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
