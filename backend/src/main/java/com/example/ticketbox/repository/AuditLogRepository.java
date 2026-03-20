package com.example.ticketbox.repository;

import com.example.ticketbox.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByCreatedDateDesc(Pageable pageable);
    Page<AuditLog> findByEntityTypeOrderByCreatedDateDesc(String entityType, Pageable pageable);
    Page<AuditLog> findByAdminIdOrderByCreatedDateDesc(Long adminId, Pageable pageable);
}
