package com.example.ticketbox.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RefundResponse {
    private Long id;
    private Long orderId;
    private BigDecimal amount;
    private String status;
    private String vnpayRequestId;
    private String vnpayResponseCode;
    private String vnpayResponseMessage;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
