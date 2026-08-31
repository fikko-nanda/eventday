package com.example.eventday.dto;

import com.example.eventday.entity.Event.EventStatus;
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
public class EventResponse {
    private UUID eventId;
    private UUID organizerId;
    private String organizerName;
    private String title;
    private String description;
    private String category;
    private String venueName;
    private String bannerUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private EventStatus status;
    private String facility;
}