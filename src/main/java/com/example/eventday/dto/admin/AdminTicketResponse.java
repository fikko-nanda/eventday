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
public class AdminTicketResponse {

    private UUID ticketItemId;
    private String ticketCode;
    private UUID orderId;
    private String orderNumber;
    private UUID eventId;
    private String eventTitle;
    private String eventVenue;
    private LocalDateTime eventDate;
    private String tierName;
    private BigDecimal tierPrice;
    private String attendeeName;
    private String attendeeEmail;
    private String attendeeNik;
    private String checkInStatus;
    private LocalDateTime checkInAt;
    private String customerName;
    private String customerEmail;
    private LocalDateTime createdAt;
}
