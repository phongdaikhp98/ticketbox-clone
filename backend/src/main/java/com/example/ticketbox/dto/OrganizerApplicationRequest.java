package com.example.ticketbox.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizerApplicationRequest {

    @NotBlank
    private String orgName;

    @NotBlank
    private String taxNumber;

    @NotBlank
    private String contactPhone;

    private String reason;
}
