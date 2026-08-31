package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ticket_tiers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tier_id", updatable = false, nullable = false)
    private UUID tierId;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "tier_name", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private String tierName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "total_quota", nullable = false)
    private Integer totalQuota;

    @Column(name = "available_quota", nullable = false)
    private Integer availableQuota;

    @Column(name = "max_per_user", nullable = false)
    private Integer maxPerUser;
}