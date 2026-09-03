package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auth")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auth {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "auth_id", updatable = false, nullable = false)
    private UUID authId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String password;

    @Column(name = "auth_google", length = 20, columnDefinition = "VARCHAR(20)")
    private String authGoogle;

    @Column(name = "akses_token", columnDefinition = "TEXT")
    private String aksesToken;

    @Column(name = "expired_token")
    private LocalDateTime expiredToken;

    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private String status = "INACTIVE";

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
