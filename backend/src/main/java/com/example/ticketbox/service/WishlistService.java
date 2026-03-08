package com.example.ticketbox.service;

import com.example.ticketbox.dto.WishlistCheckResponse;
import com.example.ticketbox.dto.WishlistResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.Event;
import com.example.ticketbox.model.TicketType;
import com.example.ticketbox.model.User;
import com.example.ticketbox.model.Wishlist;
import com.example.ticketbox.repository.EventRepository;
import com.example.ticketbox.repository.UserRepository;
import com.example.ticketbox.repository.WishlistRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    public WishlistResponse addToWishlist(Long userId, Long eventId) {
        if (wishlistRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw new BadRequestException("Event already in wishlist");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .event(event)
                .build();

        return toWishlistResponse(wishlistRepository.save(wishlist));
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long eventId) {
        Wishlist wishlist = wishlistRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
        wishlistRepository.delete(wishlist);
    }

    public Page<WishlistResponse> getMyWishlist(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return wishlistRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable)
                .map(this::toWishlistResponse);
    }

    public WishlistCheckResponse checkWishlist(Long userId, Long eventId) {
        boolean exists = wishlistRepository.existsByUserIdAndEventId(userId, eventId);
        return WishlistCheckResponse.builder()
                .wishlisted(exists)
                .build();
    }

    private WishlistResponse toWishlistResponse(Wishlist wishlist) {
        Event event = wishlist.getEvent();
        BigDecimal minPrice = event.getTicketTypes().stream()
                .map(TicketType::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventImageUrl(event.getImageUrl())
                .eventDate(event.getEventDate())
                .eventLocation(event.getLocation())
                .eventCategory(event.getCategory().name())
                .minPrice(minPrice)
                .createdDate(wishlist.getCreatedDate())
                .build();
    }
}
