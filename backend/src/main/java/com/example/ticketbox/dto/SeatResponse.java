package com.example.ticketbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private String seatCode;
    private String rowLabel;
    private Integer seatNumber;
    private String status;      // AVAILABLE / SOLD / BLOCKED / RESERVED
    private boolean reservedByMe;
}
