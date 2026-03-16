package com.example.ticketbox.repository;

import com.example.ticketbox.model.SeatSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatSectionRepository extends JpaRepository<SeatSection, Long> {
    List<SeatSection> findBySeatMapIdOrderByName(Long seatMapId);
}
