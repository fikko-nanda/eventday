package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reset_id", updatable = false, nullable = false)
    private UUID resetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255, columnDefinition = "VARCHAR(255)")
    private String token;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    // DB existing column (some manual migrations use expires_at) — keep in sync
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "used")
    @Builder.Default
    private Boolean used = false;

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

    @PrePersist
    @PreUpdate
    private void syncColumns() {
        if (expiredAt != null && expiresAt == null) {
            expiresAt = expiredAt;
        }
        if (expiresAt != null && expiredAt == null) {
            expiredAt = expiresAt;
        }
        if (used == null) used = false;
    }
}
