package com.example.eventday.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponse {
    private UUID auditId;
    private UUID actorId;
    private String actorName;
    private String action;
    private String entityType;
    private String entityId;
    private String detail;
    private LocalDateTime createdAt;
}