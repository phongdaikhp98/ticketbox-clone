package com.example.ticketbox.repository;

import com.example.ticketbox.model.Ticket;
import com.example.ticketbox.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Page<Ticket> findByUserId(Long userId, Pageable pageable);

    Page<Ticket> findByUserIdAndEventId(Long userId, Long eventId, Pageable pageable);

    Page<Ticket> findByUserIdAndStatus(Long userId, TicketStatus status, Pageable pageable);

    Page<Ticket> findByUserIdAndEventIdAndStatus(Long userId, Long eventId, TicketStatus status, Pageable pageable);

    Optional<Ticket> findByIdAndUserId(Long id, Long userId);

    Optional<Ticket> findByTicketCode(String ticketCode);

    Page<Ticket> findByEventId(Long eventId, Pageable pageable);
}
