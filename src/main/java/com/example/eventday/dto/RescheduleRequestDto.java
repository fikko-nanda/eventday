package com.example.eventday.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RescheduleRequestDto {
    private UUID eventId;
    private LocalDateTime newStartDate;
    private LocalDateTime newEndDate;
    private String reason;
}