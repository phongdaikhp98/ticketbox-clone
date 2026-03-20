package com.example.ticketbox.service;

import com.example.ticketbox.dto.AuditLogResponse;
import com.example.ticketbox.model.AuditLog;
import com.example.ticketbox.model.User;
import com.example.ticketbox.repository.AuditLogRepository;
import com.example.ticketbox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(Long adminId, String action, String entityType, Long entityId,
                    String entityName, String oldValue, String newValue) {
        try {
            User admin = userRepository.findById(adminId).orElse(null);
            if (admin == null) {
                log.warn("AuditLog: admin {} not found, skipping log", adminId);
                return;
            }
            AuditLog entry = AuditLog.builder()
                    .admin(admin)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityName(entityName)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to save audit log: {}", e.getMessage());
        }
    }

    public Page<AuditLogResponse> getLogs(String entityType, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<AuditLog> logs = (entityType != null && !entityType.isBlank())
                ? auditLogRepository.findByEntityTypeOrderByCreatedDateDesc(entityType, pageable)
                : auditLogRepository.findAllByOrderByCreatedDateDesc(pageable);
        return logs.map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .adminId(auditLog.getAdmin().getId())
                .adminName(auditLog.getAdmin().getFullName())
                .adminEmail(auditLog.getAdmin().getEmail())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .entityName(auditLog.getEntityName())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .createdDate(auditLog.getCreatedDate())
                .build();
    }
}
