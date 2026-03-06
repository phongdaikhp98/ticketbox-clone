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
public class EventStatsResponse {

    private EventSummaryDto event;
    private BigDecimal totalRevenue;
    private Long totalTicketsSold;
    private Integer totalCapacity;
    private Long totalCheckedIn;
    private Long totalIssued;
    private Long totalCancelled;
    private List<TicketTypeStatsDto> ticketTypeStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventSummaryDto {
        private Long id;
        private String title;
        private LocalDateTime eventDate;
        private LocalDateTime endDate;
        private String location;
        private String imageUrl;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTypeStatsDto {
        private Long ticketTypeId;
        private String name;
        private BigDecimal price;
        private Integer capacity;
        private Integer soldCount;
        private BigDecimal revenue;
        private Long checkedInCount;
    }
}
