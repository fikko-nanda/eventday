package com.example.eventday.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminEoApplicationResponse {
    private UUID organizerId;
    private UUID userId;
    private String nameOrganizer;
    private String userEmail;
    private String userPhone;
    private String npwpNumber;
    private String bankName;
    private String bankAccountNumber;
    private String aktaPerusahaan;
    private String verificationStatus; // UNVERIFIED, VERIFIED, REJECTED
    private LocalDateTime createdAt;
}