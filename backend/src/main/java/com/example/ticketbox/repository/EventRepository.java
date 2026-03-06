package com.example.ticketbox.repository;

import com.example.ticketbox.model.Event;
import com.example.ticketbox.model.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.isFeatured = true ORDER BY e.eventDate ASC")
    List<Event> findFeaturedEvents();

    Long countByOrganizerId(Long organizerId);
}
