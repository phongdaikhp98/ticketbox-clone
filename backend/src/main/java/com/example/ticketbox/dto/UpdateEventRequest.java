package com.example.ticketbox.dto;

import com.example.ticketbox.model.EventCategory;
import com.example.ticketbox.model.EventStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateEventRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    private LocalDateTime eventDate;

    private LocalDateTime endDate;

    @Size(max = 500, message = "Location must not exceed 500 characters")
    private String location;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    private EventCategory category;

    private EventStatus status;

    private Boolean isFeatured;

    @Valid
    private List<TicketTypeRequest> ticketTypes;
}
