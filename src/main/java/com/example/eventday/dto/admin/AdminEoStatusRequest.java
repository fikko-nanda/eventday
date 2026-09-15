package com.example.eventday.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminEoStatusRequest {
    @NotBlank(message = "Status verifikasi wajib diisi")
    private String status; // VERIFIED atau REJECTED
    private String rejectionReason;
}