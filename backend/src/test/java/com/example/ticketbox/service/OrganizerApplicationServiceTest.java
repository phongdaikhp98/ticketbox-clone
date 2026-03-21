package com.example.ticketbox.service;

import com.example.ticketbox.dto.OrganizerApplicationRequest;
import com.example.ticketbox.dto.OrganizerApplicationResponse;
import com.example.ticketbox.dto.ReviewApplicationRequest;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.OrganizerApplicationRepository;
import com.example.ticketbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizerApplicationServiceTest {

    @Mock private OrganizerApplicationRepository organizerApplicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private EmailService emailService;

    @InjectMocks
    private OrganizerApplicationService organizerApplicationService;

    private User customerUser;
    private User adminUser;
    private OrganizerApplication pendingApp;
    private OrganizerApplicationRequest request;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        customerUser = User.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Nguyen Van A")
                .role(Role.CUSTOMER)
                .isActive(true)
                .build();

        adminUser = User.builder()
                .id(99L)
                .email("admin@example.com")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .isActive(true)
                .build();

        pendingApp = OrganizerApplication.builder()
                .id(1L)
                .user(customerUser)
                .orgName("Công ty ABC")
                .taxNumber("0123456789")
                .contactPhone("0901234567")
                .reason("Muốn tổ chức sự kiện")
                .status(ApplicationStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .createdDate(LocalDateTime.now())
                .build();

        request = new OrganizerApplicationRequest();
        request.setOrgName("Công ty ABC");
        request.setTaxNumber("0123456789");
        request.setContactPhone("0901234567");
        request.setReason("Muốn tổ chức sự kiện âm nhạc");

        pageable = PageRequest.of(0, 10);
    }

    // ===================== submit =====================

    @Test
    void submit_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));
        when(organizerApplicationRepository.existsByUserIdAndStatus(1L, ApplicationStatus.PENDING))
                .thenReturn(false);
        when(organizerApplicationRepository.save(any(OrganizerApplication.class)))
                .thenReturn(pendingApp);

        OrganizerApplicationResponse response = organizerApplicationService.submit(1L, request);

        assertNotNull(response);
        assertEquals("Công ty ABC", response.getOrgName());
        assertEquals("0123456789", response.getTaxNumber());
        assertEquals("PENDING", response.getStatus());
        verify(organizerApplicationRepository).save(any(OrganizerApplication.class));
    }

    @Test
    void submit_userNotFound_throwsResourceNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> organizerApplicationService.submit(99L, request));

        verify(organizerApplicationRepository, never()).save(any());
    }

    @Test
    void submit_userAlreadyOrganizer_throwsBadRequest() {
        customerUser.setRole(Role.ORGANIZER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> organizerApplicationService.submit(1L, request));

        assertTrue(ex.getMessage().contains("Organizer"));
        verify(organizerApplicationRepository, never()).save(any());
    }

    @Test
    void submit_pendingApplicationExists_throwsBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));
        when(organizerApplicationRepository.existsByUserIdAndStatus(1L, ApplicationStatus.PENDING))
                .thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> organizerApplicationService.submit(1L, request));

        assertTrue(ex.getMessage().contains("đang chờ duyệt"));
        verify(organizerApplicationRepository, never()).save(any());
    }

    // ===================== getMyApplication =====================

    @Test
    void getMyApplication_found_shouldReturnResponse() {
        when(organizerApplicationRepository.findFirstByUserIdOrderByCreatedDateDesc(1L))
                .thenReturn(Optional.of(pendingApp));

        Optional<OrganizerApplicationResponse> result = organizerApplicationService.getMyApplication(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("PENDING", result.get().getStatus());
    }

    @Test
    void getMyApplication_notFound_shouldReturnEmpty() {
        when(organizerApplicationRepository.findFirstByUserIdOrderByCreatedDateDesc(1L))
                .thenReturn(Optional.empty());

        Optional<OrganizerApplicationResponse> result = organizerApplicationService.getMyApplication(1L);

        assertFalse(result.isPresent());
    }

    // ===================== getApplications =====================

    @Test
    void getApplications_withStatus_shouldFilterByStatus() {
        Page<OrganizerApplication> page = new PageImpl<>(List.of(pendingApp));
        when(organizerApplicationRepository.findByStatusOrderByCreatedDateDesc(
                ApplicationStatus.PENDING, pageable)).thenReturn(page);

        Page<OrganizerApplicationResponse> result =
                organizerApplicationService.getApplications(ApplicationStatus.PENDING, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("PENDING", result.getContent().get(0).getStatus());
        verify(organizerApplicationRepository).findByStatusOrderByCreatedDateDesc(ApplicationStatus.PENDING, pageable);
        verify(organizerApplicationRepository, never()).findAllByOrderByCreatedDateDesc(any());
    }

    @Test
    void getApplications_nullStatus_shouldReturnAll() {
        Page<OrganizerApplication> page = new PageImpl<>(List.of(pendingApp));
        when(organizerApplicationRepository.findAllByOrderByCreatedDateDesc(pageable))
                .thenReturn(page);

        Page<OrganizerApplicationResponse> result =
                organizerApplicationService.getApplications(null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(organizerApplicationRepository).findAllByOrderByCreatedDateDesc(pageable);
        verify(organizerApplicationRepository, never()).findByStatusOrderByCreatedDateDesc(any(), any());
    }

    // ===================== reviewApplication =====================

    @Test
    void reviewApplication_notFound_throwsResourceNotFound() {
        when(organizerApplicationRepository.findById(999L)).thenReturn(Optional.empty());

        ReviewApplicationRequest reviewRequest = new ReviewApplicationRequest();
        reviewRequest.setStatus(ApplicationStatus.APPROVED);

        assertThrows(ResourceNotFoundException.class,
                () -> organizerApplicationService.reviewApplication(99L, 999L, reviewRequest));
    }

    @Test
    void reviewApplication_alreadyReviewed_throwsBadRequest() {
        pendingApp.setStatus(ApplicationStatus.APPROVED);
        when(organizerApplicationRepository.findById(1L)).thenReturn(Optional.of(pendingApp));

        ReviewApplicationRequest reviewRequest = new ReviewApplicationRequest();
        reviewRequest.setStatus(ApplicationStatus.APPROVED);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> organizerApplicationService.reviewApplication(99L, 1L, reviewRequest));

        assertTrue(ex.getMessage().contains("đã được xử lý"));
    }

    @Test
    void reviewApplication_rejectedWithoutNote_throwsBadRequest() {
        when(organizerApplicationRepository.findById(1L)).thenReturn(Optional.of(pendingApp));

        ReviewApplicationRequest reviewRequest = new ReviewApplicationRequest();
        reviewRequest.setStatus(ApplicationStatus.REJECTED);
        reviewRequest.setReviewNote(""); // blank note

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> organizerApplicationService.reviewApplication(99L, 1L, reviewRequest));

        assertTrue(ex.getMessage().contains("lý do từ chối"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void reviewApplication_approved_shouldPromoteRoleAndSendEmail() {
        when(organizerApplicationRepository.findById(1L)).thenReturn(Optional.of(pendingApp));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenReturn(customerUser);
        when(organizerApplicationRepository.save(any(OrganizerApplication.class)))
                .thenReturn(pendingApp);

        ReviewApplicationRequest reviewRequest = new ReviewApplicationRequest();
        reviewRequest.setStatus(ApplicationStatus.APPROVED);

        organizerApplicationService.reviewApplication(99L, 1L, reviewRequest);

        // User role should be promoted to ORGANIZER
        assertEquals(Role.ORGANIZER, customerUser.getRole());
        verify(userRepository).save(customerUser);

        // Audit log called
        verify(auditLogService).log(eq(99L), eq("APPROVE_ORGANIZER"),
                eq("OrganizerApplication"), eq(1L), any(), any(), any());

        // Approval email sent
        verify(emailService).sendOrganizerApprovalEmail(
                eq("customer@example.com"), eq("Nguyen Van A"), eq("Công ty ABC"));
    }

    @Test
    void reviewApplication_rejected_shouldSendRejectionEmailAndNotChangeRole() {
        when(organizerApplicationRepository.findById(1L)).thenReturn(Optional.of(pendingApp));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));
        when(organizerApplicationRepository.save(any(OrganizerApplication.class)))
                .thenReturn(pendingApp);

        ReviewApplicationRequest reviewRequest = new ReviewApplicationRequest();
        reviewRequest.setStatus(ApplicationStatus.REJECTED);
        reviewRequest.setReviewNote("Thông tin không hợp lệ");

        organizerApplicationService.reviewApplication(99L, 1L, reviewRequest);

        // Role NOT changed
        assertEquals(Role.CUSTOMER, customerUser.getRole());
        verify(userRepository, never()).save(customerUser);

        // Audit log called
        verify(auditLogService).log(eq(99L), eq("REJECT_ORGANIZER"),
                eq("OrganizerApplication"), eq(1L), any(), any(), any());

        // Rejection email sent
        verify(emailService).sendOrganizerRejectionEmail(
                eq("customer@example.com"), eq("Nguyen Van A"),
                eq("Công ty ABC"), eq("Thông tin không hợp lệ"));
    }

    @Test
    void reviewApplication_approved_shouldSetReviewedAtAndReviewedBy() {
        when(organizerApplicationRepository.findById(1L)).thenReturn(Optional.of(pendingApp));
        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any())).thenReturn(customerUser);
        when(organizerApplicationRepository.save(any(OrganizerApplication.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ReviewApplicationRequest reviewRequest = new ReviewApplicationRequest();
        reviewRequest.setStatus(ApplicationStatus.APPROVED);

        organizerApplicationService.reviewApplication(99L, 1L, reviewRequest);

        assertEquals(ApplicationStatus.APPROVED, pendingApp.getStatus());
        assertEquals(adminUser, pendingApp.getReviewedBy());
        assertNotNull(pendingApp.getReviewedAt());
    }
}
