package com.example.eventday.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String message;
    private UUID userId;
    private String name;
    private String email;
    private String username;
    private String role;
    @JsonIgnore
    private String token;
    private Long expiresIn;
}
