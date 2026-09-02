package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_items", indexes = {
    @Index(name = "idx_attendee_nik_tier", columnList = "attendee_nik, tier_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ticket_item_id", updatable = false, nullable = false)
    private UUID ticketItemId;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "tier_id", nullable = false)
    private TicketTier tier;

    @Column(name = "attendee_email", length = 50, columnDefinition = "VARCHAR(50)")
    private String attendeeEmail;

    @Column(name = "attendee_name", nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    private String attendeeName;

    @Column(name = "attendee_nik", nullable = false, length = 16, columnDefinition = "VARCHAR(16)")
    private String attendeeNik;

    @Column(name = "check_in_status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private String checkInStatus = "UNREDEEMED";

    @Column(name = "check_in_at")
    private LocalDateTime checkInAt;

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