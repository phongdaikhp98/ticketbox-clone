package com.example.ticketbox.specification;

import com.example.ticketbox.model.Event;
import com.example.ticketbox.model.EventCategory;
import com.example.ticketbox.model.EventStatus;
import com.example.ticketbox.model.TicketType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EventSpecification {

    private EventSpecification() {}

    public static Specification<Event> hasStatus(EventStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Event> hasCategory(EventCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Event> eventDateAfter(LocalDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), from);
    }

    public static Specification<Event> eventDateBefore(LocalDateTime to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("eventDate"), to);
    }

    public static Specification<Event> locationContains(String location) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
    }

    public static Specification<Event> titleContains(String search) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%");
    }

    public static Specification<Event> hasPriceRange(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            Join<Event, TicketType> ticketJoin = root.join("ticketTypes", JoinType.INNER);
            if (min != null && max != null) {
                return cb.between(ticketJoin.get("price"), min, max);
            } else if (min != null) {
                return cb.greaterThanOrEqualTo(ticketJoin.get("price"), min);
            } else {
                return cb.lessThanOrEqualTo(ticketJoin.get("price"), max);
            }
        };
    }
}
