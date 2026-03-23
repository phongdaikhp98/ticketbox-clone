package com.example.ticketbox.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferTicketRequest {

    @NotBlank
    @Email
    private String toEmail;
}
