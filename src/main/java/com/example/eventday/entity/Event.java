package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", updatable = false, nullable = false)
    private UUID eventId;

    @ManyToOne
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer;

    @Column(nullable = false, length = 150, columnDefinition = "VARCHAR(150)")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50, columnDefinition = "VARCHAR(50)")
    private String category;

    @Column(name = "venue_name", length = 150, columnDefinition = "VARCHAR(150)")
    private String venueName;

    @Column(name = "banner_url", columnDefinition = "VARCHAR(255)")
    private String bannerUrl;

    @Column(name = "facility", columnDefinition = "TEXT")
    private String facility;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "create_by")
    private UUID createBy;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "updated_by")
    private UUID updatedBy;
}