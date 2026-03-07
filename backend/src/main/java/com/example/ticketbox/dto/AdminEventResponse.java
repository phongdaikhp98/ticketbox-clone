package com.example.ticketbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEventResponse {

    private Long id;
    private String title;
    private String organizerName;
    private Long organizerId;
    private String category;
    private String status;
    private Boolean isFeatured;
    private Integer totalCapacity;
    private Integer totalSold;
    private LocalDateTime eventDate;
    private LocalDateTime createdDate;
}
