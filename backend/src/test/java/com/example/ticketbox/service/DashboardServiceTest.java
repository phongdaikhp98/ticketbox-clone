package com.example.ticketbox.service;

import com.example.ticketbox.config.AppProperties;
import com.example.ticketbox.dto.AttendeeResponse;
import com.example.ticketbox.dto.DashboardOverviewResponse;
import com.example.ticketbox.dto.EventStatsResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Spy
    private AppProperties appProperties = new AppProperties();

    @InjectMocks
    private DashboardService dashboardService;

    private User organizer;
    private Event testEvent;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .id(1L)
                .email("organizer@test.com")
                .fullName("Test Organizer")
                .role(Role.ORGANIZER)
                .build();

        TicketType ticketType = TicketType.builder()
                .id(1L)
                .name("General")
                .price(new BigDecimal("200000"))
                .capacity(50)
                .soldCount(10)
                .build();

        testEvent = Event.builder()
                .id(1L)
                .title("Test Event")
                .location("HCMC")
                .eventDate(LocalDateTime.now().plusDays(30))
                .status(EventStatus.PUBLISHED)
                .organizer(organizer)
                .ticketTypes(new ArrayList<>(List.of(ticketType)))
                .build();
        ticketType.setEvent(testEvent);

        OrderItem orderItem = OrderItem.builder()
                .id(1L)
                .event(testEvent)
                .ticketType(ticketType)
                .quantity(2)
                .unitPrice(new BigDecimal("200000"))
                .ticketTypeName("General")
                .build();

        testOrder = Order.builder()
                .id(1L)
                .user(organizer)
                .status(OrderStatus.COMPLETED)
                .totalAmount(new BigDecimal("400000"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .orderItems(new ArrayList<>(List.of(orderItem)))
                .createdDate(LocalDateTime.now())
                .build();
        orderItem.setOrder(testOrder);
    }

    @Test
    void getOverview_adminMode_returnsSystemWideStats() {
        Page<Order> recentOrdersPage = new PageImpl<>(List.of(testOrder));
        when(eventRepository.count()).thenReturn(5L);
        when(orderItemRepository.sumTotalRevenue()).thenReturn(new BigDecimal("2000000"));
        when(ticketRepository.count()).thenReturn(20L);
        when(ticketRepository.countByStatus(TicketStatus.USED)).thenReturn(10L);
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(recentOrdersPage);

        DashboardOverviewResponse result = dashboardService.getOverview(1L, true);

        assertNotNull(result);
        assertEquals(5L, result.getTotalEvents());
        assertEquals(new BigDecimal("2000000"), result.getTotalRevenue());
        assertEquals(20L, result.getTotalTicketsSold());
        assertEquals(10L, result.getTotalCheckedIn());
        assertEquals(1, result.getRecentOrders().size());
    }

    @Test
    void getOverview_organizerMode_returnsOrganizerStats() {
        Page<Order> recentOrdersPage = new PageImpl<>(List.of(testOrder));
        when(eventRepository.countByOrganizerId(1L)).thenReturn(3L);
        when(orderItemRepository.sumRevenueByOrganizerId(1L)).thenReturn(new BigDecimal("600000"));
        when(ticketRepository.countByOrganizerId(1L)).thenReturn(6L);
        when(ticketRepository.countCheckedInByOrganizerId(1L)).thenReturn(2L);
        when(orderRepository.findRecentCompletedOrdersByOrganizerId(eq(1L), any(Pageable.class)))
                .thenReturn(recentOrdersPage);

        DashboardOverviewResponse result = dashboardService.getOverview(1L, false);

        assertNotNull(result);
        assertEquals(3L, result.getTotalEvents());
        assertEquals(new BigDecimal("600000"), result.getTotalRevenue());
        assertEquals(6L, result.getTotalTicketsSold());
        assertEquals(2L, result.getTotalCheckedIn());
    }

    @Test
    void getOverview_nullRevenue_returnsZero() {
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());
        when(eventRepository.countByOrganizerId(1L)).thenReturn(0L);
        when(orderItemRepository.sumRevenueByOrganizerId(1L)).thenReturn(null);
        when(ticketRepository.countByOrganizerId(1L)).thenReturn(0L);
        when(ticketRepository.countCheckedInByOrganizerId(1L)).thenReturn(0L);
        when(orderRepository.findRecentCompletedOrdersByOrganizerId(eq(1L), any(Pageable.class)))
                .thenReturn(emptyPage);

        DashboardOverviewResponse result = dashboardService.getOverview(1L, false);

        assertEquals(BigDecimal.ZERO, result.getTotalRevenue());
    }

    @Test
    void getEventStats_adminAccess_returnsStats() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(orderItemRepository.sumRevenueByEventId(1L)).thenReturn(new BigDecimal("400000"));
        when(ticketRepository.countByEventId(1L)).thenReturn(10L);
        when(ticketRepository.countByEventIdAndStatus(1L, TicketStatus.USED)).thenReturn(4L);
        when(ticketRepository.countByEventIdAndStatus(1L, TicketStatus.ISSUED)).thenReturn(5L);
        when(ticketRepository.countByEventIdAndStatus(1L, TicketStatus.CANCELLED)).thenReturn(1L);
        when(ticketRepository.countCheckedInByEventIdGroupByTicketType(1L)).thenReturn(Collections.emptyList());

        EventStatsResponse result = dashboardService.getEventStats(1L, 999L, true);

        assertNotNull(result);
        assertEquals("Test Event", result.getEvent().getTitle());
        assertEquals(new BigDecimal("400000"), result.getTotalRevenue());
        assertEquals(10L, result.getTotalTicketsSold());
        assertEquals(4L, result.getTotalCheckedIn());
        assertEquals(50, result.getTotalCapacity());
    }

    @Test
    void getEventStats_organizerOwnsEvent_returnsStats() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(orderItemRepository.sumRevenueByEventId(1L)).thenReturn(new BigDecimal("400000"));
        when(ticketRepository.countByEventId(1L)).thenReturn(5L);
        when(ticketRepository.countByEventIdAndStatus(1L, TicketStatus.USED)).thenReturn(2L);
        when(ticketRepository.countByEventIdAndStatus(1L, TicketStatus.ISSUED)).thenReturn(3L);
        when(ticketRepository.countByEventIdAndStatus(1L, TicketStatus.CANCELLED)).thenReturn(0L);
        when(ticketRepository.countCheckedInByEventIdGroupByTicketType(1L)).thenReturn(Collections.emptyList());

        EventStatsResponse result = dashboardService.getEventStats(1L, 1L, false);

        assertNotNull(result);
        assertEquals(1, result.getTicketTypeStats().size());
    }

    @Test
    void getEventStats_eventNotFound_throwsException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> dashboardService.getEventStats(99L, 1L, false));
    }

    @Test
    void getEventStats_nonAdminWrongOrganizer_throwsException() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        assertThrows(ResourceNotFoundException.class,
                () -> dashboardService.getEventStats(1L, 999L, false));
    }

    @Test
    void getAttendees_validAccess_returnsPaginatedResults() {
        Ticket ticket = Ticket.builder()
                .id(1L)
                .ticketCode("TBX-20260615-ABC123")
                .user(organizer)
                .event(testEvent)
                .ticketType(testEvent.getTicketTypes().get(0))
                .status(TicketStatus.ISSUED)
                .qrData("{}")
                .createdDate(LocalDateTime.now())
                .build();

        Page<Ticket> ticketPage = new PageImpl<>(List.of(ticket));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(ticketRepository.findAttendeesByEventId(eq(1L), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(ticketPage);

        Page<AttendeeResponse> result = dashboardService.getAttendees(1L, 1L, false, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("TBX-20260615-ABC123", result.getContent().get(0).getTicketCode());
    }

    @Test
    void getAttendees_eventNotFound_throwsException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> dashboardService.getAttendees(99L, 1L, false, null, null, 0, 10));
    }

    @Test
    void getAttendees_wrongOrganizer_throwsException() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        assertThrows(ResourceNotFoundException.class,
                () -> dashboardService.getAttendees(1L, 999L, false, null, null, 0, 10));
    }
}
