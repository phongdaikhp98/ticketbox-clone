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
public class AdminOverviewResponse {

    private Long totalUsers;
    private Long totalEvents;
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long totalTicketsSold;
    private Long totalCheckedIn;
    private List<RecentOrderDto> recentOrders;
    private List<TopEventDto> topEventsByRevenue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrderDto {
        private Long orderId;
        private String customerName;
        private String customerEmail;
        private String eventTitle;
        private BigDecimal totalAmount;
        private String status;
        private LocalDateTime createdDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopEventDto {
        private Long eventId;
        private String eventTitle;
        private BigDecimal revenue;
    }
}
