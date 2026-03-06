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
public class WishlistResponse {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private String eventImageUrl;
    private LocalDateTime eventDate;
    private String eventLocation;
    private String eventCategory;
    private BigDecimal minPrice;
    private LocalDateTime createdDate;
}
