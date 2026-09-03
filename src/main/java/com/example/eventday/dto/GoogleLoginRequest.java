package com.example.eventday.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "ID Token Google tidak boleh kosong")
    private String idToken;
}
