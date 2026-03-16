package com.example.ticketbox.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SEAT_SECTIONS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatSection {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_section_seq")
    @SequenceGenerator(name = "seat_section_seq", sequenceName = "SEAT_SECTION_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SEAT_MAP_ID", nullable = false)
    private SeatMap seatMap;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "COLOR", length = 20)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TICKET_TYPE_ID", nullable = false)
    private TicketType ticketType;

    @Column(name = "SEATS_PER_ROW", nullable = false)
    private Integer seatsPerRow;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Seat> seats = new ArrayList<>();

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
