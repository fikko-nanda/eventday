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
public class AuditLogExportResponse {
    private UUID auditId;
    private UUID actorId;
    private String actorName;
    private String action;
    private String detail;
    private LocalDateTime createdAt;
}
