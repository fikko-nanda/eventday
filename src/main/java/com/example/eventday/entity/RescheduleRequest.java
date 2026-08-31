package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reschedule_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reschedule_id", updatable = false, nullable = false)
    private UUID rescheduleId;

    // Relasi ke Event (karena yang di-reschedule adalah konser/acara)
    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "new_start_date", nullable = false)
    private LocalDateTime newStartDate;

    @Column(name = "new_end_date", nullable = false)
    private LocalDateTime newEndDate;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    @Builder.Default
    private RescheduleStatus status = RescheduleStatus.PENDING;

    @Column(name = "requested_at", updatable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    public enum RescheduleStatus {
        PENDING, APPROVED, REJECTED
    }
}