package com.example.ticketbox.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferTicketRequest {

    @NotBlank
    @Email
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String toEmail;
}
