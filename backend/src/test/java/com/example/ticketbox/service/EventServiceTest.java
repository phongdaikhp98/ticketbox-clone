package com.example.ticketbox.service;

import com.example.ticketbox.dto.*;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.EventRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventService eventService;

    private User testOrganizer;
    private Event testEvent;
    private CreateEventRequest createRequest;

    @BeforeEach
    void setUp() {
        testOrganizer = User.builder()
                .id(1L)
                .email("organizer@test.com")
                .fullName("Test Organizer")
                .role(Role.ORGANIZER)
                .build();

        TicketType vipTicket = TicketType.builder()
                .id(1L)
                .name("VIP")
                .price(new BigDecimal("500000"))
                .capacity(100)
                .soldCount(0)
                .build();

        testEvent = Event.builder()
                .id(1L)
                .title("Test Event")
                .description("Test Description")
                .eventDate(LocalDateTime.of(2026, 6, 15, 19, 0))
                .location("Ho Chi Minh City")
                .category(EventCategory.MUSIC)
                .status(EventStatus.DRAFT)
                .isFeatured(false)
                .organizer(testOrganizer)
                .ticketTypes(new ArrayList<>(List.of(vipTicket)))
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .build();
        vipTicket.setEvent(testEvent);

        TicketTypeRequest ttReq = new TicketTypeRequest();
        ttReq.setName("VIP");
        ttReq.setPrice(new BigDecimal("500000"));
        ttReq.setCapacity(100);

        createRequest = new CreateEventRequest();
        createRequest.setTitle("Test Event");
        createRequest.setDescription("Test Description");
        createRequest.setEventDate(LocalDateTime.of(2026, 6, 15, 19, 0));
        createRequest.setLocation("Ho Chi Minh City");
        createRequest.setCategory(EventCategory.MUSIC);
        createRequest.setTicketTypes(List.of(ttReq));
    }

    @Test
    void createEvent_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testOrganizer));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        EventResponse response = eventService.createEvent(1L, createRequest);

        assertNotNull(response);
        assertEquals("Test Event", response.getTitle());
        assertEquals("MUSIC", response.getCategory());
        assertEquals("DRAFT", response.getStatus());
        assertEquals(1, response.getTicketTypes().size());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_userNotFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.createEvent(99L, createRequest));
    }

    @Test
    void getPublishedEventById_success() {
        testEvent.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        EventResponse response = eventService.getPublishedEventById(1L);

        assertNotNull(response);
        assertEquals("Test Event", response.getTitle());
        assertEquals("PUBLISHED", response.getStatus());
    }

    @Test
    void getPublishedEventById_notFound_throwsException() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.getPublishedEventById(99L));
    }

    @Test
    void getPublishedEventById_draftEvent_throwsException() {
        testEvent.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.getPublishedEventById(1L));
    }

    @Test
    void updateEvent_success() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        UpdateEventRequest updateReq = new UpdateEventRequest();
        updateReq.setTitle("Updated Title");

        EventResponse response = eventService.updateEvent(1L, 1L, updateReq);

        assertNotNull(response);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void updateEvent_notOwner_throwsException() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        UpdateEventRequest updateReq = new UpdateEventRequest();
        updateReq.setTitle("Updated");

        assertThrows(BadRequestException.class,
                () -> eventService.updateEvent(1L, 999L, updateReq));
    }

    @Test
    void updateEvent_cancelledEvent_throwsException() {
        testEvent.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        UpdateEventRequest updateReq = new UpdateEventRequest();
        updateReq.setTitle("Updated");

        assertThrows(BadRequestException.class,
                () -> eventService.updateEvent(1L, 1L, updateReq));
    }

    @Test
    void updateEvent_invalidStatusTransition_throwsException() {
        testEvent.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        UpdateEventRequest updateReq = new UpdateEventRequest();
        updateReq.setStatus(EventStatus.CANCELLED);

        assertThrows(BadRequestException.class,
                () -> eventService.updateEvent(1L, 1L, updateReq));
    }

    @Test
    void updateEvent_publishSuccess() {
        testEvent.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        UpdateEventRequest updateReq = new UpdateEventRequest();
        updateReq.setStatus(EventStatus.PUBLISHED);

        EventResponse response = eventService.updateEvent(1L, 1L, updateReq);
        assertNotNull(response);
    }

    @Test
    void deleteEvent_success() {
        testEvent.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        assertDoesNotThrow(() -> eventService.deleteEvent(1L, 1L));
        verify(eventRepository).delete(testEvent);
    }

    @Test
    void deleteEvent_notOwner_throwsException() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        assertThrows(BadRequestException.class,
                () -> eventService.deleteEvent(1L, 999L));
    }

    @Test
    void deleteEvent_publishedEvent_throwsException() {
        testEvent.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        assertThrows(BadRequestException.class,
                () -> eventService.deleteEvent(1L, 1L));
    }

    @Test
    void getMyEvents_success() {
        Page<Event> page = new PageImpl<>(List.of(testEvent));
        when(eventRepository.findByOrganizerId(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<EventResponse> result = eventService.getMyEvents(1L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getFeaturedEvents_success() {
        testEvent.setStatus(EventStatus.PUBLISHED);
        testEvent.setIsFeatured(true);
        when(eventRepository.findFeaturedEvents()).thenReturn(List.of(testEvent));

        List<EventResponse> result = eventService.getFeaturedEvents();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsFeatured());
    }
}
