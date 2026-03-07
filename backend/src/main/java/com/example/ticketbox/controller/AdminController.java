package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.dto.*;
import com.example.ticketbox.model.EventCategory;
import com.example.ticketbox.model.EventStatus;
import com.example.ticketbox.model.OrderStatus;
import com.example.ticketbox.model.Role;
import com.example.ticketbox.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

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
            @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.changeRole(id, request.getRole())));
    }

    @PatchMapping("/users/{id}/toggle-active")
    public ResponseEntity<ApiResponse<AdminUserResponse>> toggleActive(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.toggleActive(id)));
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
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getEvents(status, category, search, page, size)));
    }

    @PatchMapping("/events/{id}/toggle-featured")
    public ResponseEntity<ApiResponse<AdminEventResponse>> toggleFeatured(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.toggleFeatured(id)));
    }

    @PatchMapping("/events/{id}/status")
    public ResponseEntity<ApiResponse<AdminEventResponse>> changeEventStatus(
            @PathVariable Long id,
            @RequestParam EventStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.changeEventStatus(id, status)));
    }
}
