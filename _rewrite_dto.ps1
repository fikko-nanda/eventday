package com.example.eventday.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Token tidak boleh kosong")
    private String token;

    @NotBlank(message = "Password baru tidak boleh kosong")
    @Size(min = 6, message = "Password minimal 6 karakter")
    private String newPassword;
}

