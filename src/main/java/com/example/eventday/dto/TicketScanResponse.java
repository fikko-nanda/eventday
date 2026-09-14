package com.example.eventday.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketScanResponse {
    private boolean valid;
    private String message;
    private String attendeeName;
    private String categoryName;
    private LocalDateTime scannedAt;
}