package com.example.eventday.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePayoutStatusRequest {
    @NotBlank(message = "Status wajib diisi")
    private String status;
    private String adminNote;
}
