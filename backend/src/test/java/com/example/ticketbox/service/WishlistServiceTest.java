package com.example.ticketbox.service;

import com.example.ticketbox.dto.WishlistCheckResponse;
import com.example.ticketbox.dto.WishlistResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.EventRepository;
import com.example.ticketbox.repository.UserRepository;
import com.example.ticketbox.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private User testUser;
    private Event testEvent;
    private Wishlist testWishlist;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@test.com")
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .build();

        TicketType ticketType = TicketType.builder()
                .id(1L)
                .name("VIP")
                .price(new BigDecimal("500000"))
                .capacity(100)
                .soldCount(0)
                .build();

        testEvent = Event.builder()
                .id(1L)
                .title("Test Event")
                .location("Test Location")
                .eventDate(LocalDateTime.now().plusDays(7))
                .status(EventStatus.PUBLISHED)
                .category(EventCategory.MUSIC)
                .organizer(testUser)
                .ticketTypes(new ArrayList<>(List.of(ticketType)))
                .build();

        ticketType.setEvent(testEvent);

        testWishlist = Wishlist.builder()
                .id(1L)
                .user(testUser)
                .event(testEvent)
                .createdDate(LocalDateTime.now())
                .build();
    }

    @Test
    void addToWishlist_shouldAddSuccessfully() {
        when(wishlistRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(false);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(testWishlist);

        WishlistResponse response = wishlistService.addToWishlist(1L, 1L);

        assertNotNull(response);
        assertEquals(1L, response.getEventId());
        assertEquals("Test Event", response.getEventTitle());
    }

    @Test
    void addToWishlist_shouldThrowWhenAlreadyExists() {
        when(wishlistRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> wishlistService.addToWishlist(1L, 1L));
    }

    @Test
    void removeFromWishlist_shouldRemoveSuccessfully() {
        when(wishlistRepository.findByUserIdAndEventId(1L, 1L)).thenReturn(Optional.of(testWishlist));

        wishlistService.removeFromWishlist(1L, 1L);

        verify(wishlistRepository).delete(testWishlist);
    }

    @Test
    void removeFromWishlist_shouldThrowWhenNotFound() {
        when(wishlistRepository.findByUserIdAndEventId(1L, 999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> wishlistService.removeFromWishlist(1L, 999L));
    }

    @Test
    void getMyWishlist_shouldReturnPage() {
        Page<Wishlist> wishlistPage = new PageImpl<>(List.of(testWishlist));
        when(wishlistRepository.findByUserIdOrderByCreatedDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(wishlistPage);

        Page<WishlistResponse> result = wishlistService.getMyWishlist(1L, 0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("Test Event", result.getContent().get(0).getEventTitle());
    }

    @Test
    void checkWishlist_shouldReturnTrue() {
        when(wishlistRepository.existsByUserIdAndEventId(1L, 1L)).thenReturn(true);

        WishlistCheckResponse response = wishlistService.checkWishlist(1L, 1L);

        assertTrue(response.isWishlisted());
    }

    @Test
    void checkWishlist_shouldReturnFalse() {
        when(wishlistRepository.existsByUserIdAndEventId(1L, 999L)).thenReturn(false);

        WishlistCheckResponse response = wishlistService.checkWishlist(1L, 999L);

        assertFalse(response.isWishlisted());
    }
}
