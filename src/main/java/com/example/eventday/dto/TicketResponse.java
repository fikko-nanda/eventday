package com.example.eventday.dto;

import com.example.eventday.entity.TicketItem.CheckInStatus;
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
public class TicketResponse {
    private UUID ticketItemId;
    private UUID orderId;
    private String orderNumber;
    private UUID tierId;
    private String tierName;
    private String ticketCode;
    private String attendeeName;
    private String attendeeNik;
    private CheckInStatus checkInStatus;
    private LocalDateTime checkInAt;
}
