package com.example.ticketbox.service;

import com.example.ticketbox.dto.*;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatMapServiceTest {

    @Mock private SeatMapRepository seatMapRepository;
    @Mock private SeatSectionRepository seatSectionRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private EventRepository eventRepository;
    @Mock private TicketTypeRepository ticketTypeRepository;
    @Mock private SeatReservationService reservationService;

    @InjectMocks
    private SeatMapService seatMapService;

    private User organizer;
    private Event event;
    private TicketType ticketType;
    private SeatMap seatMap;
    private SeatSection section;
    private Seat seat;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .id(1L)
                .email("organizer@test.com")
                .fullName("Organizer")
                .role(Role.ORGANIZER)
                .build();

        event = Event.builder()
                .id(10L)
                .title("Test Event")
                .organizer(organizer)
                .eventDate(LocalDateTime.now().plusDays(7))
                .status(EventStatus.PUBLISHED)
                .hasSeatMap(false)
                .ticketTypes(new ArrayList<>())
                .build();

        ticketType = TicketType.builder()
                .id(100L)
                .name("VIP")
                .price(new BigDecimal("500000"))
                .capacity(100)
                .soldCount(0)
                .event(event)
                .build();

        seat = Seat.builder()
                .id(1000L)
                .rowLabel("A")
                .seatNumber(1)
                .seatCode("A-1")
                .status(SeatStatus.AVAILABLE)
                .build();

        section = SeatSection.builder()
                .id(200L)
                .name("VIP Section")
                .color("#FF0000")
                .ticketType(ticketType)
                .seatsPerRow(2)
                .seats(List.of(seat))
                .build();

        seat.setSection(section);

        seatMap = SeatMap.builder()
                .id(50L)
                .event(event)
                .name("Main Hall")
                .sections(List.of(section))
                .build();

        section.setSeatMap(seatMap);
    }

    // ===================== createSeatMap =====================

    @Test
    void createSeatMap_shouldCreateSuccessfully() {
        SectionConfig sectionCfg = SectionConfig.builder()
                .name("VIP Section")
                .color("#FF0000")
                .ticketTypeId(100L)
                .rowLabels(List.of("A", "B"))
                .seatsPerRow(3)
                .build();

        CreateSeatMapRequest request = CreateSeatMapRequest.builder()
                .eventId(10L)
                .name("Main Hall")
                .sections(List.of(sectionCfg))
                .build();

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(seatMapRepository.existsByEventId(10L)).thenReturn(false);
        when(ticketTypeRepository.findById(100L)).thenReturn(Optional.of(ticketType));
        when(seatMapRepository.save(any(SeatMap.class))).thenReturn(seatMap);
        when(reservationService.getSeatReservations(anyList())).thenReturn(Collections.emptyList());

        SeatMapResponse response = seatMapService.createSeatMap(request, 1L);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals("Main Hall", response.getName());
        verify(eventRepository).save(event);
        assertTrue(event.isHasSeatMap());
    }

    @Test
    void createSeatMap_shouldThrowWhenEventNotFound() {
        CreateSeatMapRequest request = CreateSeatMapRequest.builder()
                .eventId(999L).name("X").sections(List.of()).build();

        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> seatMapService.createSeatMap(request, 1L));
    }

    @Test
    void createSeatMap_shouldThrowWhenNotOrganizer() {
        CreateSeatMapRequest request = CreateSeatMapRequest.builder()
                .eventId(10L).name("X").sections(List.of()).build();

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        // organizerId = 999, but event.organizer.id = 1
        assertThrows(BadRequestException.class,
                () -> seatMapService.createSeatMap(request, 999L));
    }

    @Test
    void createSeatMap_shouldThrowWhenSeatMapAlreadyExists() {
        CreateSeatMapRequest request = CreateSeatMapRequest.builder()
                .eventId(10L).name("X").sections(List.of()).build();

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(seatMapRepository.existsByEventId(10L)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> seatMapService.createSeatMap(request, 1L));
    }

    @Test
    void createSeatMap_shouldThrowWhenTicketTypeNotFound() {
        SectionConfig cfg = SectionConfig.builder()
                .name("S1").ticketTypeId(999L).rowLabels(List.of("A")).seatsPerRow(1).build();

        CreateSeatMapRequest request = CreateSeatMapRequest.builder()
                .eventId(10L).name("Hall").sections(List.of(cfg)).build();

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(seatMapRepository.existsByEventId(10L)).thenReturn(false);
        when(ticketTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> seatMapService.createSeatMap(request, 1L));
    }

    // ===================== getSeatMapByEvent =====================

    @Test
    void getSeatMapByEvent_shouldReturnSeatMap() {
        when(seatMapRepository.findByEventId(10L)).thenReturn(Optional.of(seatMap));
        when(reservationService.getSeatReservations(anyList())).thenReturn(Collections.emptyList());

        SeatMapResponse response = seatMapService.getSeatMapByEvent(10L, 1L);

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals(10L, response.getEventId());
        assertEquals(1, response.getSections().size());
    }

    @Test
    void getSeatMapByEvent_shouldMarkSeatReservedByMe() {
        when(seatMapRepository.findByEventId(10L)).thenReturn(Optional.of(seatMap));
        // reservation owned by user 1
        when(reservationService.getSeatReservations(anyList())).thenReturn(List.of("1"));

        SeatMapResponse response = seatMapService.getSeatMapByEvent(10L, 1L);

        SeatResponse seatResp = response.getSections().get(0).getSeats().get(0);
        assertEquals("RESERVED", seatResp.getStatus());
        assertTrue(seatResp.isReservedByMe());
    }

    @Test
    void getSeatMapByEvent_shouldMarkSeatReservedByOther() {
        when(seatMapRepository.findByEventId(10L)).thenReturn(Optional.of(seatMap));
        // reservation owned by user 2
        when(reservationService.getSeatReservations(anyList())).thenReturn(List.of("2"));

        SeatMapResponse response = seatMapService.getSeatMapByEvent(10L, 1L);

        SeatResponse seatResp = response.getSections().get(0).getSeats().get(0);
        assertEquals("RESERVED", seatResp.getStatus());
        assertFalse(seatResp.isReservedByMe());
    }

    @Test
    void getSeatMapByEvent_shouldShowSoldStatus() {
        seat.setStatus(SeatStatus.SOLD);
        when(seatMapRepository.findByEventId(10L)).thenReturn(Optional.of(seatMap));
        when(reservationService.getSeatReservations(anyList())).thenReturn(Collections.emptyList());

        SeatMapResponse response = seatMapService.getSeatMapByEvent(10L, 1L);

        assertEquals("SOLD", response.getSections().get(0).getSeats().get(0).getStatus());
    }

    @Test
    void getSeatMapByEvent_shouldThrowWhenNotFound() {
        when(seatMapRepository.findByEventId(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> seatMapService.getSeatMapByEvent(999L, 1L));
    }

    // ===================== updateSeatStatus =====================

    @Test
    void updateSeatStatus_shouldUpdateToBlocked() {
        when(seatRepository.findById(1000L)).thenReturn(Optional.of(seat));

        seatMapService.updateSeatStatus(1000L, SeatStatus.BLOCKED);

        assertEquals(SeatStatus.BLOCKED, seat.getStatus());
        verify(seatRepository).save(seat);
        verify(reservationService).releaseSeat(1000L);
    }

    @Test
    void updateSeatStatus_shouldUpdateToAvailable() {
        seat.setStatus(SeatStatus.BLOCKED);
        when(seatRepository.findById(1000L)).thenReturn(Optional.of(seat));

        seatMapService.updateSeatStatus(1000L, SeatStatus.AVAILABLE);

        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        verify(seatRepository).save(seat);
        verify(reservationService, never()).releaseSeat(anyLong());
    }

    @Test
    void updateSeatStatus_shouldThrowWhenSetToSold() {
        when(seatRepository.findById(1000L)).thenReturn(Optional.of(seat));

        assertThrows(BadRequestException.class,
                () -> seatMapService.updateSeatStatus(1000L, SeatStatus.SOLD));
    }

    @Test
    void updateSeatStatus_shouldThrowWhenSeatNotFound() {
        when(seatRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> seatMapService.updateSeatStatus(999L, SeatStatus.BLOCKED));
    }

    // ===================== deleteSeatMap =====================

    @Test
    void deleteSeatMap_shouldDeleteSuccessfully() {
        when(seatMapRepository.findById(50L)).thenReturn(Optional.of(seatMap));
        when(seatRepository.countSoldByEventId(10L)).thenReturn(0L);

        seatMapService.deleteSeatMap(50L, 1L);

        verify(seatMapRepository).delete(seatMap);
        verify(eventRepository).save(event);
        assertFalse(event.isHasSeatMap());
    }

    @Test
    void deleteSeatMap_shouldThrowWhenNotOrganizer() {
        when(seatMapRepository.findById(50L)).thenReturn(Optional.of(seatMap));

        assertThrows(BadRequestException.class,
                () -> seatMapService.deleteSeatMap(50L, 999L));
    }

    @Test
    void deleteSeatMap_shouldThrowWhenSeatsAlreadySold() {
        when(seatMapRepository.findById(50L)).thenReturn(Optional.of(seatMap));
        when(seatRepository.countSoldByEventId(10L)).thenReturn(5L);

        assertThrows(BadRequestException.class,
                () -> seatMapService.deleteSeatMap(50L, 1L));
    }

    @Test
    void deleteSeatMap_shouldThrowWhenSeatMapNotFound() {
        when(seatMapRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> seatMapService.deleteSeatMap(999L, 1L));
    }
}
