package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(length = 16, columnDefinition = "VARCHAR(16)")
    private String nik;

    @Column(nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    private String name;

    @Column(nullable = false, unique = true, length = 100, columnDefinition = "VARCHAR(100)")
    private String email;

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private String phone;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    @Builder.Default
    private Role role = Role.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, columnDefinition = "VARCHAR(255)")
    @Builder.Default
    private KycStatus kycStatus = KycStatus.UNVERIFIED;

    @Column(name = "kyc_document_url", columnDefinition = "VARCHAR(255)")
    private String kycDocumentUrl;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role {
        ADMIN, ORGANIZER, CUSTOMER
    }

    public enum KycStatus {
        UNVERIFIED, PENDING, VERIFIED, REJECTED
    }
}