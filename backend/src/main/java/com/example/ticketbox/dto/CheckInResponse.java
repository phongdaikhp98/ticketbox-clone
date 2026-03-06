package com.example.ticketbox.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponse {

    private String ticketCode;
    private String status;
    private String message;
    private String eventTitle;
    private String attendeeName;
    private String ticketTypeName;
}
