package com.example.ticketbox.service;

import com.example.ticketbox.dto.*;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.CategoryRepository;
import com.example.ticketbox.repository.EventRepository;
import com.example.ticketbox.repository.UserRepository;
import com.example.ticketbox.specification.EventSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagService tagService;

    // === Public ===

    public Page<EventResponse> getPublishedEvents(EventFilterRequest filter) {
        Specification<Event> spec = Specification.where(EventSpecification.hasStatus(EventStatus.PUBLISHED));
        spec = applyFilters(spec, filter);
        Pageable pageable = buildPageable(filter);
        Page<Event> events = eventRepository.findAll(spec, pageable);
        return events.map(this::toEventResponse);
    }

    public EventResponse getPublishedEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Event", id);
        }
        return toEventResponse(event);
    }

    public List<EventResponse> getFeaturedEvents() {
        return eventRepository.findFeaturedEvents().stream()
                .map(this::toEventResponse)
                .toList();
    }

    // === Organizer ===

    @Transactional
    public EventResponse createEvent(Long organizerId, CreateEventRequest request) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", organizerId));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));

        Set<Tag> tags = tagService.resolveTagList(request.getTags());

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .endDate(request.getEndDate())
                .location(request.getLocation())
                .imageUrl(request.getImageUrl())
                .category(category)
                .tags(new HashSet<>(tags))
                .isFeatured(request.getIsFeatured() != null && request.getIsFeatured())
                .organizer(organizer)
                .build();

        List<TicketType> ticketTypes = new ArrayList<>();
        for (TicketTypeRequest ttReq : request.getTicketTypes()) {
            TicketType tt = TicketType.builder()
                    .name(ttReq.getName())
                    .price(ttReq.getPrice())
                    .capacity(ttReq.getCapacity())
                    .event(event)
                    .build();
            ticketTypes.add(tt);
        }
        event.setTicketTypes(ticketTypes);
        tagService.incrementUsageCounts(tags);

        Event saved = eventRepository.save(event);
        return toEventResponse(saved);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, Long organizerId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new BadRequestException("You are not the owner of this event");
        }
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Cannot update a cancelled event");
        }

        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getEndDate() != null) event.setEndDate(request.getEndDate());
        if (request.getLocation() != null) event.setLocation(request.getLocation());
        if (request.getImageUrl() != null) event.setImageUrl(request.getImageUrl());
        if (request.getIsFeatured() != null) event.setIsFeatured(request.getIsFeatured());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
            event.setCategory(category);
        }

        if (request.getTags() != null) {
            Set<Tag> oldTags = new HashSet<>(event.getTags());
            Set<Tag> newTags = tagService.resolveTagList(request.getTags());

            Set<Tag> removedTags = new HashSet<>(oldTags);
            removedTags.removeAll(newTags);
            tagService.decrementUsageCounts(removedTags);

            Set<Tag> addedTags = new HashSet<>(newTags);
            addedTags.removeAll(oldTags);
            tagService.incrementUsageCounts(addedTags);

            event.getTags().clear();
            event.getTags().addAll(newTags);
        }

        if (request.getStatus() != null) {
            validateStatusTransition(event.getStatus(), request.getStatus());
            event.setStatus(request.getStatus());
        }

        if (request.getTicketTypes() != null) {
            boolean hasSold = event.getTicketTypes().stream()
                    .anyMatch(tt -> tt.getSoldCount() > 0);
            if (hasSold) {
                throw new BadRequestException("Cannot modify ticket types after tickets have been sold");
            }
            event.getTicketTypes().clear();
            for (TicketTypeRequest ttReq : request.getTicketTypes()) {
                TicketType tt = TicketType.builder()
                        .name(ttReq.getName())
                        .price(ttReq.getPrice())
                        .capacity(ttReq.getCapacity())
                        .event(event)
                        .build();
                event.getTicketTypes().add(tt);
            }
        }

        Event saved = eventRepository.save(event);
        return toEventResponse(saved);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new BadRequestException("You are not the owner of this event");
        }
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BadRequestException("Only draft events can be deleted");
        }

        tagService.decrementUsageCounts(event.getTags());
        eventRepository.delete(event);
    }

    public Page<EventResponse> getMyEvents(Long organizerId, Pageable pageable) {
        return eventRepository.findByOrganizerId(organizerId, pageable)
                .map(this::toEventResponse);
    }

    // === Private helpers ===

    private void validateStatusTransition(EventStatus current, EventStatus target) {
        if (current == EventStatus.DRAFT && target == EventStatus.PUBLISHED) return;
        if (current == EventStatus.PUBLISHED && target == EventStatus.CANCELLED) return;
        throw new BadRequestException("Invalid status transition: " + current + " -> " + target);
    }

    private Specification<Event> applyFilters(Specification<Event> spec, EventFilterRequest filter) {
        if (filter.getCategoryId() != null) {
            spec = spec.and(EventSpecification.hasCategoryId(filter.getCategoryId()));
        }
        if (filter.getTag() != null && !filter.getTag().isBlank()) {
            spec = spec.and(EventSpecification.hasTag(filter.getTag()));
        }
        if (filter.getDateFrom() != null) {
            spec = spec.and(EventSpecification.eventDateAfter(filter.getDateFrom()));
        }
        if (filter.getDateTo() != null) {
            spec = spec.and(EventSpecification.eventDateBefore(filter.getDateTo()));
        }
        if (filter.getLocation() != null && !filter.getLocation().isBlank()) {
            spec = spec.and(EventSpecification.locationContains(filter.getLocation()));
        }
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            spec = spec.and(EventSpecification.titleContains(filter.getSearch()));
        }
        if (filter.getPriceMin() != null || filter.getPriceMax() != null) {
            spec = spec.and(EventSpecification.hasPriceRange(filter.getPriceMin(), filter.getPriceMax()));
        }
        return spec;
    }

    private Pageable buildPageable(EventFilterRequest filter) {
        Sort sort = Sort.by(Sort.Direction.ASC, "eventDate");
        if (filter.getSort() != null) {
            Sort.Direction dir = "desc".equalsIgnoreCase(filter.getDirection())
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = switch (filter.getSort()) {
                case "title" -> Sort.by(dir, "title");
                default -> Sort.by(dir, "eventDate");
            };
        }
        return PageRequest.of(
                filter.getPage() != null ? filter.getPage() : 0,
                filter.getSize() != null ? filter.getSize() : 10,
                sort
        );
    }

    public EventResponse toEventResponse(Event event) {
        EventResponse.OrganizerDto orgDto = EventResponse.OrganizerDto.builder()
                .id(event.getOrganizer().getId())
                .fullName(event.getOrganizer().getFullName())
                .avatarUrl(event.getOrganizer().getAvatarUrl())
                .build();

        List<EventResponse.TicketTypeResponse> ttResponses = event.getTicketTypes().stream()
                .map(this::toTicketTypeResponse)
                .toList();

        CategoryResponse categoryResponse = event.getCategory() != null
                ? CategoryResponse.builder()
                    .id(event.getCategory().getId())
                    .name(event.getCategory().getName())
                    .slug(event.getCategory().getSlug())
                    .icon(event.getCategory().getIcon())
                    .displayOrder(event.getCategory().getDisplayOrder())
                    .build()
                : null;

        List<TagResponse> tagResponses = event.getTags().stream()
                .map(tagService::toTagResponse)
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .toList();

        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .endDate(event.getEndDate())
                .location(event.getLocation())
                .imageUrl(event.getImageUrl())
                .category(categoryResponse)
                .tags(tagResponses)
                .status(event.getStatus().name())
                .isFeatured(event.getIsFeatured())
                .organizer(orgDto)
                .ticketTypes(ttResponses)
                .createdDate(event.getCreatedDate())
                .updatedDate(event.getUpdatedDate())
                .build();
    }

    private EventResponse.TicketTypeResponse toTicketTypeResponse(TicketType tt) {
        return EventResponse.TicketTypeResponse.builder()
                .id(tt.getId())
                .name(tt.getName())
                .price(tt.getPrice())
                .capacity(tt.getCapacity())
                .soldCount(tt.getSoldCount())
                .availableCount(tt.getCapacity() - tt.getSoldCount())
                .build();
    }
}
