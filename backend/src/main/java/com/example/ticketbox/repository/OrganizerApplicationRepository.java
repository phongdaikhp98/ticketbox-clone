package com.example.ticketbox.repository;

import com.example.ticketbox.model.ApplicationStatus;
import com.example.ticketbox.model.OrganizerApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizerApplicationRepository extends JpaRepository<OrganizerApplication, Long> {

    boolean existsByUserIdAndStatus(Long userId, ApplicationStatus status);

    Optional<OrganizerApplication> findFirstByUserIdOrderByCreatedDateDesc(Long userId);

    Page<OrganizerApplication> findByStatusOrderByCreatedDateDesc(ApplicationStatus status, Pageable pageable);

    Page<OrganizerApplication> findAllByOrderByCreatedDateDesc(Pageable pageable);
}
