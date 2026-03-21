package com.example.ticketbox.service;

import com.example.ticketbox.dto.CheckInResponse;
import com.example.ticketbox.dto.TicketResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.EventRepository;
import com.example.ticketbox.repository.TicketRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private QrCodeService qrCodeService;

    @InjectMocks
    private TicketService ticketService;

    private User organizer;
    private User customer;
    private Event event;
    private TicketType ticketType;
    private Order order;
    private OrderItem orderItem;
    private Ticket issuedTicket;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .id(10L)
                .email("organizer@example.com")
                .fullName("Event Organizer")
                .role(Role.ORGANIZER)
                .isActive(true)
                .emailVerified(true)
                .build();

        customer = User.builder()
                .id(20L)
                .email("customer@example.com")
                .fullName("John Doe")
                .role(Role.CUSTOMER)
                .isActive(true)
                .emailVerified(true)
                .build();

        event = Event.builder()
                .id(1L)
                .title("Rock Concert 2025")
                .location("Ha Noi Stadium")
                .eventDate(LocalDateTime.now().plusDays(30))
                .organizer(organizer)
                .build();

        ticketType = TicketType.builder()
                .id(1L)
                .name("VIP")
                .price(BigDecimal.valueOf(500000))
                .capacity(100)
                .soldCount(0)
                .event(event)
                .build();

        order = Order.builder()
                .id(100L)
                .user(customer)
                .status(OrderStatus.COMPLETED)
                .totalAmount(BigDecimal.valueOf(1000000))
                .build();

        orderItem = OrderItem.builder()
                .id(1L)
                .order(order)
                .event(event)
                .ticketType(ticketType)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(500000))
                .ticketTypeName("VIP")
                .build();

        order.getOrderItems().add(orderItem);

        issuedTicket = Ticket.builder()
                .id(1L)
                .ticketCode("TBX-20251201-ABCDEF")
                .qrData("{\"code\":\"TBX-20251201-ABCDEF\",\"eventId\":1,\"userId\":20}")
                .status(TicketStatus.ISSUED)
                .user(customer)
                .event(event)
                .ticketType(ticketType)
                .orderItem(orderItem)
                .build();
    }

    // --- generateTickets ---

    @Test
    void generateTickets_orderWithOneItemQty2_generates2Tickets() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId((long) (Math.random() * 1000 + 1));
            return t;
        });

        List<Ticket> tickets = ticketService.generateTickets(order);

        assertNotNull(tickets);
        assertEquals(2, tickets.size());
        verify(ticketRepository, times(2)).save(any(Ticket.class));
    }

    @Test
    void generateTickets_ticketCode_isNotNullAndStartsWithTBX() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Ticket> tickets = ticketService.generateTickets(order);

        for (Ticket t : tickets) {
            assertNotNull(t.getTicketCode());
            assertTrue(t.getTicketCode().startsWith("TBX-"));
        }
    }

    @Test
    void generateTickets_orderItemWithSeat_ticketHasSeatCode() {
        Seat seat = Seat.builder()
                .id(5L)
                .rowLabel("A")
                .seatNumber(1)
                .seatCode("A-1")
                .build();
        orderItem.setSeat(seat);

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Ticket> tickets = ticketService.generateTickets(order);

        assertNotNull(tickets);
        assertEquals(2, tickets.size());
        tickets.forEach(t -> assertEquals("A-1", t.getSeatCode()));
    }

    @Test
    void generateTickets_orderItemWithNoSeat_ticketHasNullSeatCode() {
        orderItem.setSeat(null);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Ticket> tickets = ticketService.generateTickets(order);

        assertNotNull(tickets);
        tickets.forEach(t -> assertNull(t.getSeatCode()));
    }

    @Test
    void generateTickets_orderWith2ItemsEachQty2_generates4Tickets() {
        OrderItem secondItem = OrderItem.builder()
                .id(2L)
                .order(order)
                .event(event)
                .ticketType(ticketType)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(300000))
                .ticketTypeName("Standard")
                .build();
        order.getOrderItems().add(secondItem);

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        List<Ticket> tickets = ticketService.generateTickets(order);

        assertNotNull(tickets);
        assertEquals(4, tickets.size());
        verify(ticketRepository, times(4)).save(any(Ticket.class));
    }

    // --- getMyTickets ---

    @Test
    void getMyTickets_noFilters_returnsPaginatedTicketsForUser() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> page = new PageImpl<>(List.of(issuedTicket), pageable, 1);
        when(ticketRepository.findByUserId(eq(20L), any(Pageable.class))).thenReturn(page);

        Page<TicketResponse> result = ticketService.getMyTickets(20L, null, null, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(ticketRepository).findByUserId(eq(20L), any(Pageable.class));
    }

    @Test
    void getMyTickets_withStatusFilter_callsCorrectRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> page = new PageImpl<>(List.of(issuedTicket), pageable, 1);
        when(ticketRepository.findByUserIdAndStatus(eq(20L), eq(TicketStatus.ISSUED), any(Pageable.class)))
                .thenReturn(page);

        Page<TicketResponse> result = ticketService.getMyTickets(20L, null, TicketStatus.ISSUED, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(ticketRepository).findByUserIdAndStatus(eq(20L), eq(TicketStatus.ISSUED), any(Pageable.class));
    }

    @Test
    void getMyTickets_withEventIdFilter_callsCorrectRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> page = new PageImpl<>(List.of(issuedTicket), pageable, 1);
        when(ticketRepository.findByUserIdAndEventId(eq(20L), eq(1L), any(Pageable.class))).thenReturn(page);

        Page<TicketResponse> result = ticketService.getMyTickets(20L, 1L, null, 0, 10);

        assertNotNull(result);
        verify(ticketRepository).findByUserIdAndEventId(eq(20L), eq(1L), any(Pageable.class));
    }

    @Test
    void getMyTickets_withEventIdAndStatusFilter_callsCorrectRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> page = new PageImpl<>(List.of(issuedTicket), pageable, 1);
        when(ticketRepository.findByUserIdAndEventIdAndStatus(eq(20L), eq(1L), eq(TicketStatus.ISSUED), any(Pageable.class)))
                .thenReturn(page);

        Page<TicketResponse> result = ticketService.getMyTickets(20L, 1L, TicketStatus.ISSUED, 0, 10);

        assertNotNull(result);
        verify(ticketRepository).findByUserIdAndEventIdAndStatus(eq(20L), eq(1L), eq(TicketStatus.ISSUED), any(Pageable.class));
    }

    // --- getTicketDetail ---

    @Test
    void getTicketDetail_validTicketForUser_returnsTicketResponse() {
        when(ticketRepository.findByIdAndUserId(1L, 20L)).thenReturn(Optional.of(issuedTicket));

        TicketResponse result = ticketService.getTicketDetail(20L, 1L);

        assertNotNull(result);
        assertEquals("TBX-20251201-ABCDEF", result.getTicketCode());
        assertEquals("ISSUED", result.getStatus());
        assertEquals("VIP", result.getTicketTypeName());
        assertEquals("Rock Concert 2025", result.getEventTitle());
    }

    @Test
    void getTicketDetail_ticketNotOwnedByUser_throwsResourceNotFoundException() {
        when(ticketRepository.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ticketService.getTicketDetail(99L, 1L));
    }

    // --- checkIn ---

    @Test
    void checkIn_validUnusedTicket_organizerOwnsEvent_marksAsUsedAndReturnsSuccess() {
        when(ticketRepository.findByTicketCode("TBX-20251201-ABCDEF"))
                .thenReturn(Optional.of(issuedTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(issuedTicket);

        CheckInResponse response = ticketService.checkIn("TBX-20251201-ABCDEF", 10L);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("TBX-20251201-ABCDEF", response.getTicketCode());
        assertEquals("Rock Concert 2025", response.getEventTitle());
        assertEquals("John Doe", response.getAttendeeName());
        assertEquals("VIP", response.getTicketTypeName());
        assertEquals(TicketStatus.USED, issuedTicket.getStatus());
        assertNotNull(issuedTicket.getUsedAt());
        verify(ticketRepository).save(issuedTicket);
    }

    @Test
    void checkIn_alreadyUsedTicket_returnsAlreadyUsedStatus() {
        issuedTicket.setStatus(TicketStatus.USED);
        issuedTicket.setUsedAt(LocalDateTime.now().minusHours(1));
        when(ticketRepository.findByTicketCode("TBX-20251201-ABCDEF"))
                .thenReturn(Optional.of(issuedTicket));

        CheckInResponse response = ticketService.checkIn("TBX-20251201-ABCDEF", 10L);

        assertNotNull(response);
        assertEquals("ALREADY_USED", response.getStatus());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void checkIn_cancelledTicket_returnsCancelledStatus() {
        issuedTicket.setStatus(TicketStatus.CANCELLED);
        when(ticketRepository.findByTicketCode("TBX-20251201-ABCDEF"))
                .thenReturn(Optional.of(issuedTicket));

        CheckInResponse response = ticketService.checkIn("TBX-20251201-ABCDEF", 10L);

        assertNotNull(response);
        assertEquals("CANCELLED", response.getStatus());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void checkIn_organizerDoesNotOwnEvent_throwsBadRequestException() {
        when(ticketRepository.findByTicketCode("TBX-20251201-ABCDEF"))
                .thenReturn(Optional.of(issuedTicket));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> ticketService.checkIn("TBX-20251201-ABCDEF", 999L));

        assertTrue(ex.getMessage().contains("organizer"));
    }

    @Test
    void checkIn_ticketNotFound_throwsResourceNotFoundException() {
        when(ticketRepository.findByTicketCode("INVALID-CODE"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ticketService.checkIn("INVALID-CODE", 10L));
    }

    // --- checkInAsAdmin ---

    @Test
    void checkInAsAdmin_validUnusedTicket_marksAsUsedAndReturnsSuccess() {
        when(ticketRepository.findByTicketCode("TBX-20251201-ABCDEF"))
                .thenReturn(Optional.of(issuedTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(issuedTicket);

        CheckInResponse response = ticketService.checkInAsAdmin("TBX-20251201-ABCDEF");

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(TicketStatus.USED, issuedTicket.getStatus());
        assertNotNull(issuedTicket.getUsedAt());
    }

    @Test
    void checkInAsAdmin_alreadyUsedTicket_returnsAlreadyUsedStatus() {
        issuedTicket.setStatus(TicketStatus.USED);
        issuedTicket.setUsedAt(LocalDateTime.now().minusHours(2));
        when(ticketRepository.findByTicketCode("TBX-20251201-ABCDEF"))
                .thenReturn(Optional.of(issuedTicket));

        CheckInResponse response = ticketService.checkInAsAdmin("TBX-20251201-ABCDEF");

        assertEquals("ALREADY_USED", response.getStatus());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void checkInAsAdmin_ticketNotFound_throwsResourceNotFoundException() {
        when(ticketRepository.findByTicketCode("BOGUS")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ticketService.checkInAsAdmin("BOGUS"));
    }
}
