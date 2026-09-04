package com.example.eventday.dto;

import com.example.eventday.entity.User;
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
    private String token;
    private Long expiresIn;
}