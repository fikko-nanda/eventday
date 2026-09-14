package com.example.eventday.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketSummaryResponse {
    private String orderId;
    private String ticketId;
    private String ticketCode;
    private String eventTitle;
    private String bannerUrl;
    private LocalDateTime eventDate;
    private String venueName;
    private String categoryName;
    private String status; // ISSUED, USED, EXPIRED, REFUNDED
}