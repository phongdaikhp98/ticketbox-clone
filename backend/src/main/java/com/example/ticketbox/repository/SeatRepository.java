package com.example.ticketbox.repository;

import com.example.ticketbox.model.Seat;
import com.example.ticketbox.model.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findBySectionId(Long sectionId);
    List<Seat> findBySectionIdOrderByRowLabelAscSeatNumberAsc(Long sectionId);
    Optional<Seat> findByIdAndStatus(Long id, SeatStatus status);

    @Query("SELECT s FROM Seat s WHERE s.section.seatMap.event.id = :eventId")
    List<Seat> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.section.seatMap.event.id = :eventId AND s.status = 'SOLD'")
    long countSoldByEventId(@Param("eventId") Long eventId);
}
