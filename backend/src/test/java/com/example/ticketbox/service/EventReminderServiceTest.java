package com.example.ticketbox.service;

import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.ReminderLogRepository;
import com.example.ticketbox.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventReminderServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private ReminderLogRepository reminderLogRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private EventReminderService eventReminderService;

    private Ticket ticket1;
    private Ticket ticket2;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .build();

        Event event = Event.builder()
                .id(10L)
                .title("Concert ABC")
                .location("Hà Nội")
                .eventDate(LocalDateTime.now().plusHours(24))
                .build();

        TicketType ticketType = TicketType.builder()
                .id(1L)
                .name("VIP")
                .build();

        ticket1 = Ticket.builder()
                .id(101L)
                .ticketCode("TBX-20260321-ABC123")
                .user(user)
                .event(event)
                .ticketType(ticketType)
                .status(TicketStatus.ISSUED)
                .build();

        ticket2 = Ticket.builder()
                .id(102L)
                .ticketCode("TBX-20260321-DEF456")
                .user(user)
                .event(event)
                .ticketType(ticketType)
                .status(TicketStatus.ISSUED)
                .build();
    }

    @Test
    void sendEventReminders_noTicketsInWindow_shouldDoNothing() {
        when(ticketRepository.findIssuedTicketsForReminderWindow(any(), any()))
                .thenReturn(Collections.emptyList());

        eventReminderService.sendEventReminders();

        verify(emailService, never()).sendEventReminderEmail(any());
        verify(reminderLogRepository, never()).save(any());
    }

    @Test
    void sendEventReminders_reminderAlreadySent_shouldSkip() {
        when(ticketRepository.findIssuedTicketsForReminderWindow(any(), any()))
                .thenReturn(List.of(ticket1));
        when(reminderLogRepository.existsByTicketId(ticket1.getId())).thenReturn(true);

        eventReminderService.sendEventReminders();

        verify(emailService, never()).sendEventReminderEmail(any());
        verify(reminderLogRepository, never()).save(any());
    }

    @Test
    void sendEventReminders_newTicket_shouldSendEmailAndSaveLog() {
        when(ticketRepository.findIssuedTicketsForReminderWindow(any(), any()))
                .thenReturn(List.of(ticket1));
        when(reminderLogRepository.existsByTicketId(ticket1.getId())).thenReturn(false);

        eventReminderService.sendEventReminders();

        verify(emailService).sendEventReminderEmail(ticket1);

        ArgumentCaptor<ReminderLog> captor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(captor.capture());
        assertEquals(ticket1, captor.getValue().getTicket());
        assertEquals(ticket1.getEvent(), captor.getValue().getEvent());
    }

    @Test
    void sendEventReminders_mixedTickets_shouldOnlySendForNew() {
        when(ticketRepository.findIssuedTicketsForReminderWindow(any(), any()))
                .thenReturn(List.of(ticket1, ticket2));
        // ticket1 already sent, ticket2 is new
        when(reminderLogRepository.existsByTicketId(ticket1.getId())).thenReturn(true);
        when(reminderLogRepository.existsByTicketId(ticket2.getId())).thenReturn(false);

        eventReminderService.sendEventReminders();

        verify(emailService, never()).sendEventReminderEmail(ticket1);
        verify(emailService).sendEventReminderEmail(ticket2);
        verify(reminderLogRepository, times(1)).save(any());
    }

    @Test
    void sendEventReminders_queryUsesCorrectTimeWindow() {
        when(ticketRepository.findIssuedTicketsForReminderWindow(any(), any()))
                .thenReturn(Collections.emptyList());

        eventReminderService.sendEventReminders();

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(ticketRepository).findIssuedTicketsForReminderWindow(startCaptor.capture(), endCaptor.capture());

        LocalDateTime now = LocalDateTime.now();
        // windowStart should be ~23h from now
        assertTrue(startCaptor.getValue().isAfter(now.plusHours(22)));
        assertTrue(startCaptor.getValue().isBefore(now.plusHours(24)));
        // windowEnd should be ~25h from now
        assertTrue(endCaptor.getValue().isAfter(now.plusHours(24)));
        assertTrue(endCaptor.getValue().isBefore(now.plusHours(26)));
    }
}
