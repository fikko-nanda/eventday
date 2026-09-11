package com.example.eventday.dto;

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
public class TicketItemResponse {
    private UUID ticketId;
    private String ticketCode;
    private String eventTitle;
    private String categoryName;
    private LocalDateTime eventDate;
    private String location;
    private String status;
}