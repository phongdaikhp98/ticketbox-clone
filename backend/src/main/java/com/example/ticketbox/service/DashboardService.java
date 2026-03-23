package com.example.ticketbox.service;

import com.example.ticketbox.config.AppProperties;
import com.example.ticketbox.dto.AttendeeResponse;
import com.example.ticketbox.dto.DashboardOverviewResponse;
import com.example.ticketbox.dto.EventStatsResponse;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AppProperties appProperties;

    public DashboardOverviewResponse getOverview(Long userId, boolean isAdmin) {
        Long organizerId = isAdmin ? null : userId;

        Long totalEvents;
        BigDecimal totalRevenue;
        Long totalTicketsSold;
        Long totalCheckedIn;
        Page<Order> recentOrders;

        if (isAdmin) {
            totalEvents = eventRepository.count();
            totalRevenue = orderItemRepository.sumTotalRevenue();
            totalTicketsSold = ticketRepository.count();
            totalCheckedIn = ticketRepository.countByStatus(TicketStatus.USED);
            recentOrders = orderRepository.findAll(
                    PageRequest.of(0, appProperties.getDashboard().getRecentOrdersSize(),
                            Sort.by(Sort.Direction.DESC, "createdDate")));
        } else {
            totalEvents = eventRepository.countByOrganizerId(organizerId);
            totalRevenue = orderItemRepository.sumRevenueByOrganizerId(organizerId);
            totalTicketsSold = ticketRepository.countByOrganizerId(organizerId);
            totalCheckedIn = ticketRepository.countCheckedInByOrganizerId(organizerId);
            recentOrders = orderRepository.findRecentCompletedOrdersByOrganizerId(
                    organizerId, PageRequest.of(0, appProperties.getDashboard().getRecentOrdersSize()));
        }

        List<DashboardOverviewResponse.RecentOrderDto> recentOrderDtos = recentOrders.getContent().stream()
                .map(order -> {
                    String eventTitle = order.getOrderItems().isEmpty() ? ""
                            : order.getOrderItems().get(0).getEvent().getTitle();
                    return DashboardOverviewResponse.RecentOrderDto.builder()
                            .orderId(order.getId())
                            .customerName(order.getUser().getFullName())
                            .customerEmail(order.getUser().getEmail())
                            .eventTitle(eventTitle)
                            .totalAmount(order.getTotalAmount())
                            .status(order.getStatus().name())
                            .createdDate(order.getCreatedDate())
                            .build();
                })
                .toList();

        return DashboardOverviewResponse.builder()
                .totalEvents(totalEvents)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalTicketsSold(totalTicketsSold)
                .totalCheckedIn(totalCheckedIn)
                .recentOrders(recentOrderDtos)
                .build();
    }

    public EventStatsResponse getEventStats(Long eventId, Long userId, boolean isAdmin) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (!isAdmin && !event.getOrganizer().getId().equals(userId)) {
            throw new ResourceNotFoundException("Event not found");
        }

        BigDecimal totalRevenue = orderItemRepository.sumRevenueByEventId(eventId);
        Long totalTicketsSold = ticketRepository.countByEventId(eventId);
        Long totalCheckedIn = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.USED);
        Long totalIssued = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.ISSUED);
        Long totalCancelled = ticketRepository.countByEventIdAndStatus(eventId, TicketStatus.CANCELLED);

        Integer totalCapacity = event.getTicketTypes().stream()
                .mapToInt(TicketType::getCapacity)
                .sum();

        // Checked-in count per ticket type
        Map<Long, Long> checkedInByType = ticketRepository.countCheckedInByEventIdGroupByTicketType(eventId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        List<EventStatsResponse.TicketTypeStatsDto> ticketTypeStats = event.getTicketTypes().stream()
                .map(tt -> EventStatsResponse.TicketTypeStatsDto.builder()
                        .ticketTypeId(tt.getId())
                        .name(tt.getName())
                        .price(tt.getPrice())
                        .capacity(tt.getCapacity())
                        .soldCount(tt.getSoldCount())
                        .revenue(tt.getPrice().multiply(BigDecimal.valueOf(tt.getSoldCount())))
                        .checkedInCount(checkedInByType.getOrDefault(tt.getId(), 0L))
                        .build())
                .toList();

        EventStatsResponse.EventSummaryDto eventSummary = EventStatsResponse.EventSummaryDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .eventDate(event.getEventDate())
                .endDate(event.getEndDate())
                .location(event.getLocation())
                .imageUrl(event.getImageUrl())
                .status(event.getStatus().name())
                .build();

        return EventStatsResponse.builder()
                .event(eventSummary)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalTicketsSold(totalTicketsSold)
                .totalCapacity(totalCapacity)
                .totalCheckedIn(totalCheckedIn)
                .totalIssued(totalIssued)
                .totalCancelled(totalCancelled)
                .ticketTypeStats(ticketTypeStats)
                .build();
    }

    public Page<AttendeeResponse> getAttendees(Long eventId, Long userId, boolean isAdmin,
                                                TicketStatus status, String search,
                                                int page, int size) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (!isAdmin && !event.getOrganizer().getId().equals(userId)) {
            throw new ResourceNotFoundException("Event not found");
        }

        String searchParam = (search != null && !search.isBlank()) ? search : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        Page<Ticket> tickets = ticketRepository.findAttendeesByEventId(eventId, status, searchParam, pageable);

        return tickets.map(ticket -> AttendeeResponse.builder()
                .ticketId(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .attendeeName(ticket.getUser().getFullName())
                .attendeeEmail(ticket.getUser().getEmail())
                .ticketTypeName(ticket.getTicketType().getName())
                .status(ticket.getStatus().name())
                .usedAt(ticket.getUsedAt())
                .createdDate(ticket.getCreatedDate())
                .build());
    }
}
