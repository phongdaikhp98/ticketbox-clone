package com.example.ticketbox.dto;

import com.example.ticketbox.model.SeatStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSeatStatusRequest {
    @NotNull
    private SeatStatus status;
}
