package com.example.eventday.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String username;
    private String identifier;
    private String password;

    public String getIdentifier() {
        if (identifier != null && !identifier.isBlank()) return identifier;
        if (email != null && !email.isBlank()) return email;
        if (username != null && !username.isBlank()) return username;
        return null;
    }
}
