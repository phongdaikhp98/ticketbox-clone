package com.example.ticketbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long id;
    private Integer quantity;
    private TicketTypeSummary ticketType;
    private EventSummary event;
    private LocalDateTime createdDate;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTypeSummary {
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer availableCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventSummary {
        private Long id;
        private String title;
        private String imageUrl;
        private LocalDateTime eventDate;
        private String location;
    }
}
