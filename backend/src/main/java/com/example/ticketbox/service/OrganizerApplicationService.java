package com.example.ticketbox.service;

import com.example.ticketbox.dto.OrganizerApplicationRequest;
import com.example.ticketbox.dto.OrganizerApplicationResponse;
import com.example.ticketbox.dto.ReviewApplicationRequest;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.ApplicationStatus;
import com.example.ticketbox.model.OrganizerApplication;
import com.example.ticketbox.model.Role;
import com.example.ticketbox.model.User;
import com.example.ticketbox.repository.OrganizerApplicationRepository;
import com.example.ticketbox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizerApplicationService {

    private final OrganizerApplicationRepository organizerApplicationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Transactional
    public OrganizerApplicationResponse submit(Long userId, OrganizerApplicationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.CUSTOMER) {
            throw new BadRequestException("Bạn đã là Organizer");
        }

        if (organizerApplicationRepository.existsByUserIdAndStatus(userId, ApplicationStatus.PENDING)) {
            throw new BadRequestException("Bạn đã có đơn đang chờ duyệt");
        }

        OrganizerApplication application = OrganizerApplication.builder()
                .user(user)
                .orgName(request.getOrgName())
                .taxNumber(request.getTaxNumber())
                .contactPhone(request.getContactPhone())
                .reason(request.getReason())
                .build();

        OrganizerApplication saved = organizerApplicationRepository.save(application);
        log.info("Organizer application #{} submitted by user #{}", saved.getId(), userId);
        return toResponse(saved);
    }

    public Optional<OrganizerApplicationResponse> getMyApplication(Long userId) {
        return organizerApplicationRepository.findFirstByUserIdOrderByCreatedDateDesc(userId)
                .map(this::toResponse);
    }

    public Page<OrganizerApplicationResponse> getApplications(ApplicationStatus status, Pageable pageable) {
        Page<OrganizerApplication> page = (status == null)
                ? organizerApplicationRepository.findAllByOrderByCreatedDateDesc(pageable)
                : organizerApplicationRepository.findByStatusOrderByCreatedDateDesc(status, pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public OrganizerApplicationResponse reviewApplication(Long adminId, Long appId, ReviewApplicationRequest request) {
        OrganizerApplication application = organizerApplicationRepository.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer application not found"));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("Đơn này đã được xử lý");
        }

        if (request.getStatus() == ApplicationStatus.REJECTED
                && (request.getReviewNote() == null || request.getReviewNote().isBlank())) {
            throw new BadRequestException("Vui lòng nhập lý do từ chối");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        application.setStatus(request.getStatus());
        application.setReviewedBy(admin);
        application.setReviewNote(request.getReviewNote());
        application.setReviewedAt(LocalDateTime.now());

        if (request.getStatus() == ApplicationStatus.APPROVED) {
            User applicant = application.getUser();
            applicant.setRole(Role.ORGANIZER);
            userRepository.save(applicant);
            auditLogService.log(adminId, "APPROVE_ORGANIZER", "OrganizerApplication", appId,
                    applicant.getEmail(), ApplicationStatus.PENDING.name(), ApplicationStatus.APPROVED.name());
            emailService.sendOrganizerApprovalEmail(applicant.getEmail(), applicant.getFullName(), application.getOrgName());
        } else {
            User applicant = application.getUser();
            auditLogService.log(adminId, "REJECT_ORGANIZER", "OrganizerApplication", appId,
                    applicant.getEmail(), ApplicationStatus.PENDING.name(), ApplicationStatus.REJECTED.name());
            emailService.sendOrganizerRejectionEmail(applicant.getEmail(), applicant.getFullName(),
                    application.getOrgName(), request.getReviewNote());
        }

        OrganizerApplication saved = organizerApplicationRepository.save(application);
        log.info("Organizer application #{} reviewed by admin #{}: {}", appId, adminId, request.getStatus());
        return toResponse(saved);
    }

    private OrganizerApplicationResponse toResponse(OrganizerApplication app) {
        return OrganizerApplicationResponse.builder()
                .id(app.getId())
                .userId(app.getUser().getId())
                .userFullName(app.getUser().getFullName())
                .userEmail(app.getUser().getEmail())
                .orgName(app.getOrgName())
                .taxNumber(app.getTaxNumber())
                .contactPhone(app.getContactPhone())
                .reason(app.getReason())
                .status(app.getStatus().name())
                .reviewedByName(app.getReviewedBy() != null ? app.getReviewedBy().getFullName() : null)
                .reviewNote(app.getReviewNote())
                .submittedAt(app.getSubmittedAt())
                .reviewedAt(app.getReviewedAt())
                .createdDate(app.getCreatedDate())
                .build();
    }
}
