package com.example.ticketbox.service;

import com.example.ticketbox.dto.ReviewRequest;
import com.example.ticketbox.dto.ReviewResponse;
import com.example.ticketbox.exception.BadRequestException;
import com.example.ticketbox.exception.ResourceNotFoundException;
import com.example.ticketbox.model.*;
import com.example.ticketbox.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public Page<ReviewResponse> getEventReviews(Long eventId, int page, int size) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", eventId);
        }
        return reviewRepository.findByEventIdOrderByCreatedDateDesc(eventId, PageRequest.of(page, size))
                .map(this::toReviewResponse);
    }

    @Transactional
    public ReviewResponse createReview(Long userId, Long eventId, ReviewRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BadRequestException("Chỉ có thể đánh giá sự kiện đang hoạt động");
        }

        boolean hasAttended = orderItemRepository.existsByEventIdAndAttendee(eventId, userId);
        if (!hasAttended) {
            throw new BadRequestException("Bạn chỉ có thể đánh giá sự kiện mà bạn đã mua vé thành công");
        }

        if (reviewRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new BadRequestException("Bạn đã đánh giá sự kiện này rồi");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Review review = Review.builder()
                .event(event)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        return toReviewResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, Long userId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền sửa đánh giá này");
        }

        review.setRating(request.getRating());
        if (request.getComment() != null) {
            review.setComment(request.getComment());
        }

        return toReviewResponse(reviewRepository.save(review));
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        if (!isAdmin && !review.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xóa đánh giá này");
        }

        reviewRepository.delete(review);
    }

    private ReviewResponse toReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .eventId(review.getEvent().getId())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .userAvatarUrl(review.getUser().getAvatarUrl())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdDate(review.getCreatedDate())
                .updatedDate(review.getUpdatedDate())
                .build();
    }
}
