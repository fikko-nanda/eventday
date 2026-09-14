package com.example.eventday.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketDetailResponse {
    private String ticketId;
    private String ticketCode;
    private String orderId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String venueName;
    private String categoryName;
    private String attendeeName;
    private String attendeeEmail;
    private String attendeeIdentityNumber;
    private String status;
    private LocalDateTime issuedAt;
}