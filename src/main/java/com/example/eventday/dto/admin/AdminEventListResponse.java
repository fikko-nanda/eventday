package com.example.eventday.dto.admin;

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
public class AdminEventListResponse {

    private UUID eventId;
    private String title;
    private String category;
    private String venueName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private Boolean isFeatured;
    private UUID organizerId;
    private String organizerName;
    private String bannerUrl;
    private LocalDateTime createdAt;
}
