package com.example.ticketbox.controller;

import com.example.ticketbox.common.ApiResponse;
import com.example.ticketbox.dto.*;
import com.example.ticketbox.security.UserDetailsImpl;
import com.example.ticketbox.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // === Public endpoints ===

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getEvents(
            @ModelAttribute EventFilterRequest filter) {
        Page<EventResponse> events = eventService.getPublishedEvents(filter);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getFeaturedEvents() {
        List<EventResponse> events = eventService.getFeaturedEvents();
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable Long id) {
        EventResponse event = eventService.getPublishedEventById(id);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    // === Organizer endpoints ===

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateEventRequest request) {
        EventResponse event = eventService.createEvent(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(event));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody UpdateEventRequest request) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        EventResponse event = eventService.updateEvent(id, userDetails.getId(), isAdmin, request);
        return ResponseEntity.ok(ApiResponse.success("Event updated", event));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        eventService.deleteEvent(id, userDetails.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Event deleted", null));
    }

    @GetMapping("/{id}/manage")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> getEventForManage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        EventResponse event = eventService.getEventForManage(id, userDetails.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> duplicateEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        EventResponse event = eventService.duplicateEvent(id, userDetails.getId(), isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(event));
    }

    @GetMapping("/my-events")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getMyEvents(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<EventResponse> events = eventService.getMyEvents(
                userDetails.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(events));
    }
}
