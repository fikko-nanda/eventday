package com.example.eventday.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateEventRequest {
    private String title;
    private String description;
    private String category;
    private String location;
    private String venueName;
    private LocalDateTime eventDate;
    private String bannerUrl;
    private List<String> facilities;
    private List<TicketTierDto> ticketTiers;
    private UUID organizerId; // opsional: jika diisi → assign organizer, jika null → event Admin (organizer=null)

    @Data
    public static class TicketTierDto {
        private String name; // VIP, Early Bird, Regular
        private BigDecimal price;
        private Integer quota;
    }
}