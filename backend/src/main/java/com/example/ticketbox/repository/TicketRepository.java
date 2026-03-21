package com.example.ticketbox.repository;

import com.example.ticketbox.model.Ticket;
import com.example.ticketbox.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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

    // Dashboard aggregate queries

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.organizer.id = :organizerId")
    Long countByOrganizerId(@Param("organizerId") Long organizerId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.organizer.id = :organizerId AND t.status = 'USED'")
    Long countCheckedInByOrganizerId(@Param("organizerId") Long organizerId);

    Long countByStatus(TicketStatus status);

    Long countByEventIdAndStatus(Long eventId, TicketStatus status);

    Long countByEventId(Long eventId);

    @Query("SELECT t.ticketType.id, COUNT(t) FROM Ticket t WHERE t.event.id = :eventId AND t.status = 'USED' GROUP BY t.ticketType.id")
    List<Object[]> countCheckedInByEventIdGroupByTicketType(@Param("eventId") Long eventId);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.user JOIN FETCH t.event JOIN FETCH t.ticketType " +
           "WHERE t.status = 'ISSUED' AND t.event.eventDate BETWEEN :windowStart AND :windowEnd")
    List<Ticket> findIssuedTicketsForReminderWindow(@Param("windowStart") LocalDateTime windowStart,
                                                    @Param("windowEnd") LocalDateTime windowEnd);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.user JOIN FETCH t.ticketType " +
           "WHERE t.event.id = :eventId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:search IS NULL OR LOWER(t.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(t.ticketCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Ticket> findAttendeesByEventId(
            @Param("eventId") Long eventId,
            @Param("status") TicketStatus status,
            @Param("search") String search,
            Pageable pageable);
}
