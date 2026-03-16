package com.example.ticketbox.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SEAT_MAPS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMap {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_map_seq")
    @SequenceGenerator(name = "seat_map_seq", sequenceName = "SEAT_MAP_SEQ", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EVENT_ID", nullable = false, unique = true)
    private Event event;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @OneToMany(mappedBy = "seatMap", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SeatSection> sections = new ArrayList<>();

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
