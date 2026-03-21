package com.example.ticketbox.service;

import com.example.ticketbox.dto.AuditLogResponse;
import com.example.ticketbox.model.AuditLog;
import com.example.ticketbox.model.Role;
import com.example.ticketbox.model.User;
import com.example.ticketbox.repository.AuditLogRepository;
import com.example.ticketbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private User adminUser;
    private AuditLog testAuditLog;
    private final Long adminId = 1L;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(adminId)
                .email("admin@test.com")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .build();

        testAuditLog = AuditLog.builder()
                .id(1L)
                .admin(adminUser)
                .action("CHANGE_ROLE")
                .entityType("USER")
                .entityId(2L)
                .entityName("user@test.com")
                .oldValue("CUSTOMER")
                .newValue("ORGANIZER")
                .build();
    }

    @Test
    void log_adminFound_savesAuditLogWithCorrectFields() {
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);

        assertDoesNotThrow(() -> auditLogService.log(adminId, "CHANGE_ROLE", "USER", 2L, "user@test.com", "CUSTOMER", "ORGANIZER"));

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void log_adminNotFound_doesNotThrowAndSaveNeverCalled() {
        when(userRepository.findById(adminId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> auditLogService.log(adminId, "CHANGE_ROLE", "USER", 2L, "user@test.com", "CUSTOMER", "ORGANIZER"));

        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void log_repositoryThrows_doesNotPropagateException() {
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(auditLogRepository.save(any(AuditLog.class))).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> auditLogService.log(adminId, "CHANGE_ROLE", "USER", 2L, "user@test.com", "CUSTOMER", "ORGANIZER"));
    }

    @Test
    void getLogs_noFilter_returnsAllLogsPaginated() {
        Page<AuditLog> page = new PageImpl<>(Collections.singletonList(testAuditLog));
        when(auditLogRepository.findAllByOrderByCreatedDateDesc(any())).thenReturn(page);

        Page<AuditLogResponse> result = auditLogService.getLogs(null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(auditLogRepository).findAllByOrderByCreatedDateDesc(any());
        verify(auditLogRepository, never()).findByEntityTypeOrderByCreatedDateDesc(any(), any());
    }

    @Test
    void getLogs_filterByEntityTypeUser_filtersByEntityType() {
        Page<AuditLog> page = new PageImpl<>(Collections.singletonList(testAuditLog));
        when(auditLogRepository.findByEntityTypeOrderByCreatedDateDesc(eq("USER"), any())).thenReturn(page);

        Page<AuditLogResponse> result = auditLogService.getLogs("USER", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(auditLogRepository).findByEntityTypeOrderByCreatedDateDesc(eq("USER"), any());
    }

    @Test
    void getLogs_filterByEntityTypeEvent_filtersByEntityType() {
        AuditLog eventLog = AuditLog.builder()
                .id(2L)
                .admin(adminUser)
                .action("TOGGLE_FEATURED")
                .entityType("EVENT")
                .entityId(5L)
                .entityName("Test Event")
                .oldValue("NOT_FEATURED")
                .newValue("FEATURED")
                .build();
        Page<AuditLog> page = new PageImpl<>(Collections.singletonList(eventLog));
        when(auditLogRepository.findByEntityTypeOrderByCreatedDateDesc(eq("EVENT"), any())).thenReturn(page);

        Page<AuditLogResponse> result = auditLogService.getLogs("EVENT", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(auditLogRepository).findByEntityTypeOrderByCreatedDateDesc(eq("EVENT"), any());
    }
}
