package com.example.ticketbox.dto;

import com.example.ticketbox.model.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewApplicationRequest {

    @NotNull
    private ApplicationStatus status;

    private String reviewNote;
}
