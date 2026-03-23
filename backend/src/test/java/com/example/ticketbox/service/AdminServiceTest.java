package com.example.ticketbox.service;

import com.example.ticketbox.config.AppProperties;
import com.example.ticketbox.dto.AdminEventResponse;
import com.example.ticketbox.dto.AdminOrderResponse;
import com.example.ticketbox.dto.AdminUserResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private OrganizerApplicationService organizerApplicationService;

    @Spy
    private AppProperties appProperties = new AppProperties();

    @InjectMocks
    private AdminService adminService;

    private User targetUser;
    private User adminUser;
    private Event testEvent;
    private final Long adminId = 1L;
    private final Long targetUserId = 2L;
    private final Long eventId = 10L;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(adminId)
                .email("admin@test.com")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .isActive(true)
                .emailVerified(true)
                .build();

        targetUser = User.builder()
                .id(targetUserId)
                .email("user@test.com")
                .fullName("Target User")
                .role(Role.CUSTOMER)
                .isActive(true)
                .emailVerified(false)
                .build();

        User organizer = User.builder()
                .id(99L)
                .email("org@test.com")
                .fullName("Organizer")
                .role(Role.ORGANIZER)
                .build();

        testEvent = Event.builder()
                .id(eventId)
                .title("Test Event")
                .status(EventStatus.DRAFT)
                .isFeatured(false)
                .organizer(organizer)
                .ticketTypes(new ArrayList<>())
                .build();
    }

    @Test
    void changeRole_validChange_successAndAuditLogCalled() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        AdminUserResponse result = adminService.changeRole(adminId, targetUserId, "ORGANIZER");

        assertNotNull(result);
        verify(auditLogService).log(eq(adminId), eq("CHANGE_ROLE"), eq("USER"), eq(targetUserId), any(), any(), any());
    }

    @Test
    void changeRole_targetIsAdmin_throwsBadRequestException() {
        targetUser.setRole(Role.ADMIN);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        assertThrows(BadRequestException.class,
                () -> adminService.changeRole(adminId, targetUserId, "CUSTOMER"));
    }

    @Test
    void changeRole_promoteToAdmin_throwsBadRequestException() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        assertThrows(BadRequestException.class,
                () -> adminService.changeRole(adminId, targetUserId, "ADMIN"));
    }

    @Test
    void changeRole_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.changeRole(adminId, targetUserId, "ORGANIZER"));
    }

    @Test
    void toggleActive_deactivateActiveUser_successAndAuditLogCalled() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        AdminUserResponse result = adminService.toggleActive(adminId, targetUserId);

        assertNotNull(result);
        verify(auditLogService).log(eq(adminId), eq("TOGGLE_ACTIVE"), eq("USER"), eq(targetUserId), any(), any(), any());
    }

    @Test
    void toggleActive_targetIsAdmin_throwsBadRequestException() {
        targetUser.setRole(Role.ADMIN);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        assertThrows(BadRequestException.class,
                () -> adminService.toggleActive(adminId, targetUserId));
    }

    @Test
    void toggleActive_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminService.toggleActive(adminId, targetUserId));
    }

    @Test
    void toggleFeatured_eventFound_togglesAndAuditLogCalled() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        AdminEventResponse result = adminService.toggleFeatured(adminId, eventId);

        assertNotNull(result);
        verify(auditLogService).log(eq(adminId), eq("TOGGLE_FEATURED"), eq("EVENT"), eq(eventId), any(), any(), any());
    }

    @Test
    void changeEventStatus_draftToPublished_successAndAuditLogCalled() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        AdminEventResponse result = adminService.changeEventStatus(adminId, eventId, EventStatus.PUBLISHED);

        assertNotNull(result);
        verify(auditLogService).log(eq(adminId), eq("CHANGE_EVENT_STATUS"), eq("EVENT"), eq(eventId), any(), any(), any());
    }

    @Test
    void changeEventStatus_alreadyCancelled_throwsBadRequestException() {
        testEvent.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        assertThrows(BadRequestException.class,
                () -> adminService.changeEventStatus(adminId, eventId, EventStatus.PUBLISHED));
    }

    @Test
    void changeEventStatus_publishedToDraft_throwsBadRequestException() {
        testEvent.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        assertThrows(BadRequestException.class,
                () -> adminService.changeEventStatus(adminId, eventId, EventStatus.DRAFT));
    }

    @Test
    void getUsers_returnsPagedResults() {
        Page<User> page = new PageImpl<>(Collections.singletonList(targetUser));
        when(userRepository.findAllWithFilters(any(), any(), any(), any())).thenReturn(page);

        Page<AdminUserResponse> result = adminService.getUsers(null, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOrders_returnsPagedResults() {
        Order testOrder = Order.builder()
                .id(1L)
                .user(targetUser)
                .status(OrderStatus.COMPLETED)
                .paymentStatus(PaymentStatus.SUCCESS)
                .totalAmount(BigDecimal.valueOf(100_000))
                .orderItems(new ArrayList<>())
                .build();
        Page<Order> page = new PageImpl<>(Collections.singletonList(testOrder));
        when(orderRepository.findAllWithFilters(any(), any(), any())).thenReturn(page);

        Page<AdminOrderResponse> result = adminService.getOrders(null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
