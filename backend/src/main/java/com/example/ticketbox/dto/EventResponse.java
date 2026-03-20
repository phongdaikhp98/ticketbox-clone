package com.example.ticketbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private LocalDateTime endDate;
    private String location;
    private String imageUrl;
    private CategoryResponse category;
    private List<TagResponse> tags;
    private String status;
    private Boolean isFeatured;
    private boolean hasSeatMap;
    private OrganizerDto organizer;
    private List<TicketTypeResponse> ticketTypes;
    private Double averageRating;
    private Long reviewCount;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizerDto {
        private Long id;
        private String fullName;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTypeResponse {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer capacity;
        private Integer soldCount;
        private Integer availableCount;
    }
}
