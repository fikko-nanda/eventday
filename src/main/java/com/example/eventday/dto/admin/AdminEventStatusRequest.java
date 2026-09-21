package com.example.eventday.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminEventStatusRequest {

    @NotBlank(message = "Status tidak boleh kosong")
    private String status; // PUBLISHED, DRAFT, CANCELLED, DELETED

    private String rejectionReason;
}
