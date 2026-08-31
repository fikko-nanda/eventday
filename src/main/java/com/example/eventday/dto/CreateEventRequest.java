package com.example.eventday.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateEventRequest {
    private UUID organizerId;
    private String title;
    private String description;
    private String category;
    private String venueName;
    private String bannerUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private List<String> facility; 
}   