package com.example.ticketbox.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSeatMapRequest {

    @NotNull
    private Long eventId;

    @NotBlank
    private String name;

    @NotEmpty
    @Valid
    private List<SectionConfig> sections;
}
