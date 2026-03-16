package com.example.ticketbox.repository;

import com.example.ticketbox.model.SeatMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeatMapRepository extends JpaRepository<SeatMap, Long> {
    Optional<SeatMap> findByEventId(Long eventId);
    boolean existsByEventId(Long eventId);
}
