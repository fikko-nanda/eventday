package com.example.eventday.dto;

import lombok.Data;

@Data
public class TicketScanRequest {
    private String ticketCode;
    private Long eventId;
}