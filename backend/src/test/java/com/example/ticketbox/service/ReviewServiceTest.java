package com.example.ticketbox.service;

import com.example.ticketbox.dto.ReviewRequest;
import com.example.ticketbox.dto.ReviewResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Event testEvent;
    private Review testReview;
    private ReviewRequest reviewRequest;
    private final Long userId = 1L;
    private final Long eventId = 10L;
    private final Long reviewId = 100L;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(userId)
                .email("user@test.com")
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .isActive(true)
                .build();

        testEvent = Event.builder()
                .id(eventId)
                .title("Test Event")
                .status(EventStatus.PUBLISHED)
                .organizer(testUser)
                .build();

        testReview = Review.builder()
                .id(reviewId)
                .event(testEvent)
                .user(testUser)
                .rating(5)
                .comment("Great event!")
                .build();

        reviewRequest = new ReviewRequest();
        reviewRequest.setRating(4);
        reviewRequest.setComment("Very good");
    }

    @Test
    void createReview_userHasAttendedAndNoReview_createsReview() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(orderItemRepository.existsByEventIdAndAttendee(eventId, userId)).thenReturn(true);
        when(reviewRepository.existsByEventIdAndUserId(eventId, userId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        ReviewResponse result = reviewService.createReview(userId, eventId, reviewRequest);

        assertNotNull(result);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_eventNotPublished_throwsBadRequestException() {
        testEvent.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        assertThrows(BadRequestException.class,
                () -> reviewService.createReview(userId, eventId, reviewRequest));
    }

    @Test
    void createReview_userHasNotAttended_throwsBadRequestException() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(orderItemRepository.existsByEventIdAndAttendee(eventId, userId)).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> reviewService.createReview(userId, eventId, reviewRequest));
    }

    @Test
    void createReview_userAlreadyReviewed_throwsBadRequestException() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));
        when(orderItemRepository.existsByEventIdAndAttendee(eventId, userId)).thenReturn(true);
        when(reviewRepository.existsByEventIdAndUserId(eventId, userId)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> reviewService.createReview(userId, eventId, reviewRequest));
    }

    @Test
    void createReview_eventNotFound_throwsResourceNotFoundException() {
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.createReview(userId, eventId, reviewRequest));
    }

    @Test
    void updateReview_ownerUpdates_success() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        ReviewResponse result = reviewService.updateReview(reviewId, userId, reviewRequest);

        assertNotNull(result);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void updateReview_nonOwner_throwsBadRequestException() {
        Long otherUserId = 999L;
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));

        assertThrows(BadRequestException.class,
                () -> reviewService.updateReview(reviewId, otherUserId, reviewRequest));
    }

    @Test
    void updateReview_reviewNotFound_throwsResourceNotFoundException() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.updateReview(reviewId, userId, reviewRequest));
    }

    @Test
    void deleteReview_ownerDeletesOwnReview_success() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));

        assertDoesNotThrow(() -> reviewService.deleteReview(reviewId, userId, false));
        verify(reviewRepository).delete(testReview);
    }

    @Test
    void deleteReview_adminDeletesAnyReview_success() {
        Long otherUserId = 999L;
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));

        assertDoesNotThrow(() -> reviewService.deleteReview(reviewId, otherUserId, true));
        verify(reviewRepository).delete(testReview);
    }

    @Test
    void deleteReview_nonOwnerNonAdmin_throwsBadRequestException() {
        Long otherUserId = 999L;
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));

        assertThrows(BadRequestException.class,
                () -> reviewService.deleteReview(reviewId, otherUserId, false));
    }

    @Test
    void getEventReviews_returnsPagedResults() {
        when(eventRepository.existsById(eventId)).thenReturn(true);
        Page<Review> page = new PageImpl<>(Collections.singletonList(testReview));
        when(reviewRepository.findByEventIdOrderByCreatedDateDesc(any(), any())).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getEventReviews(eventId, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
