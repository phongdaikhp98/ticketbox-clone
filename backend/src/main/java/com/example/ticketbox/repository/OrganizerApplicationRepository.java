package com.example.ticketbox.repository;

import com.example.ticketbox.model.ApplicationStatus;
import com.example.ticketbox.model.OrganizerApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrganizerApplicationRepository extends JpaRepository<OrganizerApplication, Long> {

    boolean existsByUserIdAndStatus(Long userId, ApplicationStatus status);

    Optional<OrganizerApplication> findFirstByUserIdOrderByCreatedDateDesc(Long userId);

    // [SECURITY] Cooldown check — prevent spam resubmission after rejection (L2)
    @Query("SELECT COUNT(a) > 0 FROM OrganizerApplication a " +
           "WHERE a.user.id = :userId AND a.status = 'REJECTED' AND a.reviewedAt > :since")
    boolean existsRecentRejectionByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    Page<OrganizerApplication> findByStatusOrderByCreatedDateDesc(ApplicationStatus status, Pageable pageable);

    Page<OrganizerApplication> findAllByOrderByCreatedDateDesc(Pageable pageable);
}
