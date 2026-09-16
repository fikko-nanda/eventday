package com.example.eventday.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutDetailResponse {
    private UUID payoutId;
    private UUID organizerId;
    private String nameOrganizer;
    private String userEmail;
    private String userPhone;
    private BigDecimal amount;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String status;
    private String rejectionReason;
    private String adminNote;
    private String reconciliationDocumentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
