package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organizer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "organizer_id", updatable = false, nullable = false)
    private UUID organizerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "name_organizer", nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    private String nameOrganizer;

    @Column(name = "npwp_number", length = 25, columnDefinition = "VARCHAR(25)")
    private String npwpNumber;

    @Column(name = "akta_perusahaan", length = 255, columnDefinition = "VARCHAR(255)")
    private String aktaPerusahaan;

    @Column(name = "cv_url", length = 255, columnDefinition = "VARCHAR(255)")
    private String cvUrl;

    @Column(name = "portfolio_url", length = 255, columnDefinition = "VARCHAR(255)")
    private String portfolioUrl;

    @Column(name = "bank_name", length = 50, columnDefinition = "VARCHAR(50)")
    private String bankName;

    @Column(name = "bank_account_number", length = 35, columnDefinition = "VARCHAR(35)")
    private String bankAccountNumber;

    @Column(name = "verification_status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private String verificationStatus = "UNVERIFIED";

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
