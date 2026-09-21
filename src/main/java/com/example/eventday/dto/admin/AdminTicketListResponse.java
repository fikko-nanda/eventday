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
public class AdminTicketListResponse {

    private UUID ticketItemId;
    private String ticketCode;
    private UUID eventId;
    private String eventTitle;
    private String tierName;
    private BigDecimal tierPrice;
    private String attendeeName;
    private String attendeeEmail;
    private String checkInStatus;
    private LocalDateTime checkInAt;
    private UUID orderId;
    private String orderNumber;
    private LocalDateTime createdAt;
}
