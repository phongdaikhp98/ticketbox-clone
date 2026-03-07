package com.example.ticketbox.service;

import com.example.ticketbox.dto.AdminEventResponse;
import com.example.ticketbox.dto.AdminOrderResponse;
import com.example.ticketbox.dto.AdminOverviewResponse;
import com.example.ticketbox.dto.AdminUserResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.*;
import com.example.ticketbox.specification.EventSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TicketRepository ticketRepository;

    // ==================== Dashboard Overview ====================

    public AdminOverviewResponse getOverview() {
        Long totalUsers = userRepository.count();
        Long totalEvents = eventRepository.count();
        BigDecimal totalRevenue = orderItemRepository.sumTotalRevenue();
        Long totalOrders = orderRepository.count();
        Long totalTicketsSold = ticketRepository.count();
        Long totalCheckedIn = ticketRepository.countByStatus(TicketStatus.USED);

        // Recent 10 orders
        Page<Order> recentOrdersPage = orderRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdDate")));

        List<AdminOverviewResponse.RecentOrderDto> recentOrders = recentOrdersPage.getContent().stream()
                .map(order -> {
                    String eventTitle = order.getOrderItems().isEmpty() ? ""
                            : order.getOrderItems().get(0).getEvent().getTitle();
                    return AdminOverviewResponse.RecentOrderDto.builder()
                            .orderId(order.getId())
                            .customerName(order.getUser().getFullName())
                            .customerEmail(order.getUser().getEmail())
                            .eventTitle(eventTitle)
                            .totalAmount(order.getTotalAmount())
                            .status(order.getStatus().name())
                            .createdDate(order.getCreatedDate())
                            .build();
                })
                .collect(Collectors.toList());

        // Top 5 events by revenue
        List<Object[]> topEventsRaw = orderItemRepository.findTopEventsByRevenue(
                PageRequest.of(0, 5));
        List<AdminOverviewResponse.TopEventDto> topEvents = topEventsRaw.stream()
                .map(row -> AdminOverviewResponse.TopEventDto.builder()
                        .eventId((Long) row[0])
                        .eventTitle((String) row[1])
                        .revenue((BigDecimal) row[2])
                        .build())
                .collect(Collectors.toList());

        return AdminOverviewResponse.builder()
                .totalUsers(totalUsers)
                .totalEvents(totalEvents)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalOrders(totalOrders)
                .totalTicketsSold(totalTicketsSold)
                .totalCheckedIn(totalCheckedIn)
                .recentOrders(recentOrders)
                .topEventsByRevenue(topEvents)
                .build();
    }

    // ==================== User Management ====================

    public Page<AdminUserResponse> getUsers(Role role, Boolean isActive, String search, int page, int size) {
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<User> users = userRepository.findAllWithFilters(role, isActive, searchParam, pageable);
        return users.map(this::toAdminUserResponse);
    }

    @Transactional
    public AdminUserResponse changeRole(Long userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot change role of an admin user");
        }

        Role role;
        try {
            role = Role.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + newRole);
        }

        if (role == Role.ADMIN) {
            throw new BadRequestException("Cannot promote user to admin role");
        }

        user.setRole(role);
        userRepository.save(user);
        return toAdminUserResponse(user);
    }

    @Transactional
    public AdminUserResponse toggleActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot deactivate an admin user");
        }

        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        return toAdminUserResponse(user);
    }

    // ==================== Order Management ====================

    public Page<AdminOrderResponse> getOrders(OrderStatus status, String search, int page, int size) {
        String searchParam = (search != null && !search.isBlank()) ? search : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<Order> orders = orderRepository.findAllWithFilters(status, searchParam, pageable);
        return orders.map(this::toAdminOrderResponse);
    }

    public AdminOrderResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toAdminOrderResponse(order);
    }

    // ==================== Event Management ====================

    public Page<AdminEventResponse> getEvents(EventStatus status, EventCategory category,
                                               String search, int page, int size) {
        Specification<Event> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and(EventSpecification.hasStatus(status));
        }
        if (category != null) {
            spec = spec.and(EventSpecification.hasCategory(category));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(EventSpecification.titleContains(search));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<Event> events = eventRepository.findAll(spec, pageable);
        return events.map(this::toAdminEventResponse);
    }

    @Transactional
    public AdminEventResponse toggleFeatured(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        event.setIsFeatured(!event.getIsFeatured());
        eventRepository.save(event);
        return toAdminEventResponse(event);
    }

    @Transactional
    public AdminEventResponse changeEventStatus(Long eventId, EventStatus newStatus) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        EventStatus current = event.getStatus();
        if (current == EventStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status of a cancelled event");
        }
        if (current == EventStatus.PUBLISHED && newStatus == EventStatus.DRAFT) {
            throw new BadRequestException("Cannot revert a published event to draft");
        }

        event.setStatus(newStatus);
        eventRepository.save(event);
        return toAdminEventResponse(event);
    }

    // ==================== Mappers ====================

    private AdminUserResponse toAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .createdDate(user.getCreatedDate())
                .build();
    }

    private AdminOrderResponse toAdminOrderResponse(Order order) {
        List<AdminOrderResponse.OrderItemDto> items = order.getOrderItems().stream()
                .map(item -> AdminOrderResponse.OrderItemDto.builder()
                        .id(item.getId())
                        .eventTitle(item.getEvent().getTitle())
                        .ticketTypeName(item.getTicketType().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        String eventTitle = order.getOrderItems().isEmpty() ? ""
                : order.getOrderItems().get(0).getEvent().getTitle();

        return AdminOrderResponse.builder()
                .id(order.getId())
                .customerName(order.getUser().getFullName())
                .customerEmail(order.getUser().getEmail())
                .eventTitle(eventTitle)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .createdDate(order.getCreatedDate())
                .orderItems(items)
                .build();
    }

    private AdminEventResponse toAdminEventResponse(Event event) {
        int totalCapacity = event.getTicketTypes().stream()
                .mapToInt(TicketType::getCapacity)
                .sum();
        int totalSold = event.getTicketTypes().stream()
                .mapToInt(TicketType::getSoldCount)
                .sum();

        return AdminEventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .organizerName(event.getOrganizer().getFullName())
                .organizerId(event.getOrganizer().getId())
                .category(event.getCategory().name())
                .status(event.getStatus().name())
                .isFeatured(event.getIsFeatured())
                .totalCapacity(totalCapacity)
                .totalSold(totalSold)
                .eventDate(event.getEventDate())
                .createdDate(event.getCreatedDate())
                .build();
    }
}
