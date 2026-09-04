package com.example.eventday.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    // frontend bisa kirim {email, password} ATAU {username, password} ATAU {identifier, password}
    private String email;
    private String username;
    private String identifier;

    @NotBlank(message = "Password tidak boleh kosong")
    private String password;

    public String getIdentifier() {
        if (identifier != null && !identifier.isBlank()) return identifier.trim();
        if (email != null && !email.isBlank()) return email.trim();
        if (username != null && !username.isBlank()) return username.trim();
        return null;
    }
}