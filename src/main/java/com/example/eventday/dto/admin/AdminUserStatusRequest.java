package com.example.eventday.dto.admin; 

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUserStatusRequest {
    @NotBlank(message = "Status tidak boleh kosong")
    private String status; // ACTIVE, INACTIVE, SUSPENDED
}