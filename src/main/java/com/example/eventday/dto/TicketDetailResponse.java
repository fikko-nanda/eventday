package com.example.eventday.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailResponse {
    private String ticketId;
    private String ticketCode;
    private String orderId;
    private String eventId;        
    private String eventImageUrl; 
    private String eventTitle;
    private LocalDateTime eventDate;
    private String venueName;
    private String categoryName;
    private String attendeeName;
    private String attendeeEmail;
    private String customerEmail;
    private String attendeeIdentityNumber;
    private String status;
    private LocalDateTime issuedAt;
}