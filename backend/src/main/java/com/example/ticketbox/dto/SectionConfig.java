package com.example.ticketbox.dto;

import jakarta.validation.constraints.Min;
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
public class SectionConfig {

    @NotBlank
    private String name;

    private String color;

    @NotNull
    private Long ticketTypeId;

    @NotEmpty
    private List<String> rowLabels;

    @NotNull
    @Min(1)
    private Integer seatsPerRow;
}
