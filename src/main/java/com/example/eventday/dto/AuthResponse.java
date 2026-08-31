package com.example.eventday.dto;

import java.util.UUID;

import com.example.eventday.entity.User.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String message;
    private UUID userId;
    private String name;
    private String email;
    private Role role;
}