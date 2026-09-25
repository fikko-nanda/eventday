package com.example.eventday.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateEventRequest {
    private UUID eventId; // opsional: dipakai saat update (path/query/body)
    @JsonAlias({"id", "event_id"})
    private UUID id;
    private String title;
    private String description;
    private String category;
    private String location;
    @JsonAlias("venue_name")
    private String venueName;
    private LocalDateTime eventDate;
    @JsonAlias("start_date")
    private LocalDateTime startDate;
    @JsonAlias("end_date")
    private LocalDateTime endDate;
    @JsonAlias("banner_url")
    private String bannerUrl;
    @JsonAlias("facility")
    private List<String> facilities;
    @JsonAlias("ticket_tiers")
    private List<TicketTierDto> ticketTiers;
    @JsonAlias("organizer_id")
    private UUID organizerId; // opsional: jika diisi → assign organizer, jika null → event Admin (organizer=null)

    public UUID getEffectiveEventId() {
        if (eventId != null) return eventId;
        return id;
    }

    @Data
    public static class TicketTierDto {
        @JsonAlias("tier_name")
        private String name; // VIP, Early Bird, Regular
        private BigDecimal price;
        @JsonAlias({"total_quota", "totalQuota"})
        private Integer quota;
    }
}