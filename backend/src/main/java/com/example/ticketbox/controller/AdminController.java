package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.dto.*;
import com.example.ticketbox.model.ApplicationStatus;
import com.example.ticketbox.model.EventStatus;
import com.example.ticketbox.model.OrderStatus;
import com.example.ticketbox.model.RefundStatus;
import com.example.ticketbox.model.Role;
import com.example.ticketbox.security.UserDetailsImpl;
import com.example.ticketbox.service.AdminService;
import com.example.ticketbox.service.AuditLogService;
import com.example.ticketbox.service.ExportService;
import com.example.ticketbox.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;
    private final RefundService refundService;
    private final ExportService exportService;

    // ==================== Dashboard ====================

    @GetMapping("/dashboard/overview")
    public ResponseEntity<ApiResponse<AdminOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getOverview()));
    }

    // ==================== User Management ====================

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getUsers(role, isActive, search, page, size)));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long adminId = userDetails.getId();
        return ResponseEntity.ok(ApiResponse.success(
                adminService.changeRole(adminId, id, request.getRole())));
    }

    @PatchMapping("/users/{id}/toggle-active")
    public ResponseEntity<ApiResponse<AdminUserResponse>> toggleActive(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long adminId = userDetails.getId();
        return ResponseEntity.ok(ApiResponse.success(adminService.toggleActive(adminId, id)));
    }

    // ==================== Order Management ====================

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<AdminOrderResponse>>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getOrders(status, search, page, size)));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<AdminOrderResponse>> getOrderDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getOrderDetail(id)));
    }

    // ==================== Event Management ====================

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<Page<AdminEventResponse>>> getEvents(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getEvents(status, categoryId, search, page, size)));
    }

    @PatchMapping("/events/{id}/toggle-featured")
    public ResponseEntity<ApiResponse<AdminEventResponse>> toggleFeatured(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long adminId = userDetails.getId();
        return ResponseEntity.ok(ApiResponse.success(adminService.toggleFeatured(adminId, id)));
    }

    @PatchMapping("/events/{id}/featured-order")
    public ResponseEntity<ApiResponse<AdminEventResponse>> setFeaturedOrder(
            @PathVariable Long id,
            @RequestParam Integer order,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.setFeaturedOrder(userDetails.getId(), id, order)));
    }

    @PatchMapping("/events/{id}/status")
    public ResponseEntity<ApiResponse<AdminEventResponse>> changeEventStatus(
            @PathVariable Long id,
            @RequestParam EventStatus status,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long adminId = userDetails.getId();
        return ResponseEntity.ok(ApiResponse.success(
                adminService.changeEventStatus(adminId, id, status)));
    }

    // ==================== Organizer Application Management ====================

    @GetMapping("/organizer-applications")
    public ResponseEntity<ApiResponse<Page<OrganizerApplicationResponse>>> getOrganizerApplications(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getOrganizerApplications(status, pageable)));
    }

    @PatchMapping("/organizer-applications/{id}/review")
    public ResponseEntity<ApiResponse<OrganizerApplicationResponse>> reviewOrganizerApplication(
            @PathVariable Long id,
            @Valid @RequestBody ReviewApplicationRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long adminId = userDetails.getId();
        return ResponseEntity.ok(ApiResponse.success(
                adminService.reviewOrganizerApplication(adminId, id, request)));
    }

    // ==================== Refund Management ====================

    @GetMapping("/refunds")
    public ResponseEntity<ApiResponse<Page<RefundResponse>>> getRefunds(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        return ResponseEntity.ok(ApiResponse.success(refundService.getAllRefunds(status, pageable)));
    }

    // ==================== Export ====================

    @GetMapping("/export/orders")
    public ResponseEntity<byte[]> exportOrders() {
        return buildExcelResponse(exportService.exportOrders(), "orders.xlsx");
    }

    @GetMapping("/export/users")
    public ResponseEntity<byte[]> exportUsers() {
        return buildExcelResponse(exportService.exportUsers(), "users.xlsx");
    }

    @GetMapping("/export/revenue")
    public ResponseEntity<byte[]> exportRevenue() {
        return buildExcelResponse(exportService.exportRevenue(), "revenue.xlsx");
    }

    private ResponseEntity<byte[]> buildExcelResponse(byte[] data, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        return ResponseEntity.ok().headers(headers).body(data);
    }

    // ==================== Audit Logs ====================

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getLogs(entityType, page, size)));
    }
}
