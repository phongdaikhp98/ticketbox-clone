package com.example.ticketbox.specification;

import com.example.ticketbox.model.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EventSpecification {

    private EventSpecification() {}

    public static Specification<Event> hasStatus(EventStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Event> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Event> hasTag(String tagName) {
        return (root, query, cb) -> {
            // Use EXISTS subquery to avoid DISTINCT + JOIN conflict with pagination count query in Oracle
            Subquery<Integer> sq = query.subquery(Integer.class);
            Root<Event> sqEvent = sq.correlate(root);
            Join<Event, Tag> tagJoin = sqEvent.join("tags", JoinType.INNER);
            sq.select(cb.literal(1));
            sq.where(cb.equal(cb.lower(tagJoin.get("name")), tagName.toLowerCase()));
            return cb.exists(sq);
        };
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
        return searchKeyword(search);
    }

    public static Specification<Event> searchKeyword(String search) {
        return (root, query, cb) -> {
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Event> hasPriceRange(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            Subquery<Integer> sq = query.subquery(Integer.class);
            Root<Event> sqEvent = sq.correlate(root);
            Join<Event, TicketType> ticketJoin = sqEvent.join("ticketTypes", JoinType.INNER);
            sq.select(cb.literal(1));
            if (min != null && max != null) {
                sq.where(cb.between(ticketJoin.get("price"), min, max));
            } else if (min != null) {
                sq.where(cb.greaterThanOrEqualTo(ticketJoin.get("price"), min));
            } else {
                sq.where(cb.lessThanOrEqualTo(ticketJoin.get("price"), max));
            }
            return cb.exists(sq);
        };
    }
}
