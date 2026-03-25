package com.example.ticketbox.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizerApplicationRequest {

    @NotBlank
    @Size(max = 255, message = "Tên tổ chức không được vượt quá 255 ký tự")
    private String orgName;

    @NotBlank
    @Size(max = 20, message = "Mã số thuế không được vượt quá 20 ký tự")
    @Pattern(regexp = "^[0-9\\-]+$", message = "Mã số thuế chỉ được chứa chữ số và dấu gạch ngang")
    private String taxNumber;

    @NotBlank
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    @Pattern(regexp = "^[0-9+\\-\\s]+$", message = "Số điện thoại không hợp lệ")
    private String contactPhone;

    @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự")
    private String reason;
}
