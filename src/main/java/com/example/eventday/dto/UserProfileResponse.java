package com.example.eventday.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID userId;
    private String name;
    private String email;
    private String username;
    private String phone;
    private String nik;
    private String role;
    private String avatarUrl; // opsional jika kolom avatar tersedia di User
}