package com.example.eventday.entity;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "eo_payout_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"organizer_id", "event_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EOPayoutBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(name = "event_id")
    private UUID eventId;

    @Builder.Default
    @Column(name = "gross_sales", nullable = false)
    private BigDecimal grossSales = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_admin_fees", nullable = false)
    private BigDecimal totalAdminFees = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_refunds", nullable = false)
    private BigDecimal totalRefunds = BigDecimal.ZERO;

    // Direct read-only mapping untuk kolom generated database
    @Column(name = "net_balance", insertable = false, updatable = false)
    private BigDecimal netBalance;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    public void onSave() {
        this.lastUpdated = LocalDateTime.now();
    }
}