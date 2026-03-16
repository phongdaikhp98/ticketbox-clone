package com.example.ticketbox.service;

import com.example.ticketbox.dto.*;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatMapService {

    private final SeatMapRepository seatMapRepository;
    private final SeatSectionRepository seatSectionRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final SeatReservationService reservationService;

    @Transactional
    public SeatMapResponse createSeatMap(CreateSeatMapRequest request, Long organizerId) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", request.getEventId()));

        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new BadRequestException("You are not the organizer of this event");
        }

        if (seatMapRepository.existsByEventId(request.getEventId())) {
            throw new BadRequestException("Seat map already exists for this event");
        }

        SeatMap seatMap = SeatMap.builder()
                .event(event)
                .name(request.getName())
                .build();

        List<SeatSection> sections = new ArrayList<>();
        for (SectionConfig cfg : request.getSections()) {
            TicketType ticketType = ticketTypeRepository.findById(cfg.getTicketTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("TicketType", cfg.getTicketTypeId()));

            SeatSection section = SeatSection.builder()
                    .seatMap(seatMap)
                    .name(cfg.getName())
                    .color(cfg.getColor())
                    .ticketType(ticketType)
                    .seatsPerRow(cfg.getSeatsPerRow())
                    .build();

            List<Seat> seats = generateSeats(section, cfg.getRowLabels(), cfg.getSeatsPerRow());
            section.setSeats(seats);
            sections.add(section);
        }

        seatMap.setSections(sections);
        SeatMap saved = seatMapRepository.save(seatMap);

        // Update event hasSeatMap flag
        event.setHasSeatMap(true);
        eventRepository.save(event);

        return toSeatMapResponse(saved, null);
    }

    public SeatMapResponse getSeatMapByEvent(Long eventId, Long currentUserId) {
        SeatMap seatMap = seatMapRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("SeatMap for event", eventId));

        // Collect all seat IDs for batch Redis lookup
        List<Long> allSeatIds = seatMap.getSections().stream()
                .flatMap(s -> s.getSeats().stream())
                .map(Seat::getId)
                .toList();

        List<String> reservations = reservationService.getSeatReservations(allSeatIds);

        String currentUserIdStr = currentUserId != null ? String.valueOf(currentUserId) : null;

        return toSeatMapResponseWithReservations(seatMap, allSeatIds, reservations, currentUserIdStr);
    }

    @Transactional
    public void updateSeatStatus(Long seatId, SeatStatus status) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", seatId));

        if (status == SeatStatus.SOLD) {
            throw new BadRequestException("Cannot manually set seat to SOLD");
        }

        seat.setStatus(status);
        seatRepository.save(seat);

        // If blocking a seat, release any existing reservation
        if (status == SeatStatus.BLOCKED) {
            reservationService.releaseSeat(seatId);
        }
    }

    @Transactional
    public void deleteSeatMap(Long seatMapId, Long organizerId) {
        SeatMap seatMap = seatMapRepository.findById(seatMapId)
                .orElseThrow(() -> new ResourceNotFoundException("SeatMap", seatMapId));

        Event event = seatMap.getEvent();
        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new BadRequestException("You are not the organizer of this event");
        }

        long soldSeats = seatRepository.countSoldByEventId(event.getId());
        if (soldSeats > 0) {
            throw new BadRequestException("Cannot delete seat map: some seats are already sold");
        }

        seatMapRepository.delete(seatMap);

        event.setHasSeatMap(false);
        eventRepository.save(event);
    }

    private List<Seat> generateSeats(SeatSection section, List<String> rowLabels, int seatsPerRow) {
        List<Seat> seats = new ArrayList<>();
        for (String row : rowLabels) {
            for (int num = 1; num <= seatsPerRow; num++) {
                String seatCode = row + "-" + num;
                seats.add(Seat.builder()
                        .section(section)
                        .rowLabel(row)
                        .seatNumber(num)
                        .seatCode(seatCode)
                        .status(SeatStatus.AVAILABLE)
                        .build());
            }
        }
        return seats;
    }

    private SeatMapResponse toSeatMapResponse(SeatMap seatMap, Long currentUserId) {
        List<Long> allSeatIds = seatMap.getSections().stream()
                .flatMap(s -> s.getSeats().stream())
                .map(Seat::getId)
                .toList();
        List<String> reservations = reservationService.getSeatReservations(allSeatIds);
        String currentUserIdStr = currentUserId != null ? String.valueOf(currentUserId) : null;
        return toSeatMapResponseWithReservations(seatMap, allSeatIds, reservations, currentUserIdStr);
    }

    private SeatMapResponse toSeatMapResponseWithReservations(
            SeatMap seatMap,
            List<Long> allSeatIds,
            List<String> reservations,
            String currentUserIdStr) {

        List<SectionResponse> sectionResponses = new ArrayList<>();
        int seatIndex = 0;

        for (SeatSection section : seatMap.getSections()) {
            List<SeatResponse> seatResponses = new ArrayList<>();
            for (Seat seat : section.getSeats()) {
                String reservedBy = seatIndex < reservations.size() ? reservations.get(seatIndex) : null;
                seatIndex++;

                String displayStatus;
                boolean reservedByMe = false;

                if (seat.getStatus() == SeatStatus.SOLD) {
                    displayStatus = "SOLD";
                } else if (seat.getStatus() == SeatStatus.BLOCKED) {
                    displayStatus = "BLOCKED";
                } else if (reservedBy != null) {
                    displayStatus = "RESERVED";
                    reservedByMe = reservedBy.equals(currentUserIdStr);
                } else {
                    displayStatus = "AVAILABLE";
                }

                seatResponses.add(SeatResponse.builder()
                        .id(seat.getId())
                        .seatCode(seat.getSeatCode())
                        .rowLabel(seat.getRowLabel())
                        .seatNumber(seat.getSeatNumber())
                        .status(displayStatus)
                        .reservedByMe(reservedByMe)
                        .build());
            }

            TicketType tt = section.getTicketType();
            sectionResponses.add(SectionResponse.builder()
                    .id(section.getId())
                    .name(section.getName())
                    .color(section.getColor())
                    .ticketTypeId(tt.getId())
                    .ticketTypeName(tt.getName())
                    .price(tt.getPrice())
                    .seats(seatResponses)
                    .build());
        }

        return SeatMapResponse.builder()
                .id(seatMap.getId())
                .eventId(seatMap.getEvent().getId())
                .name(seatMap.getName())
                .sections(sectionResponses)
                .build();
    }
}
