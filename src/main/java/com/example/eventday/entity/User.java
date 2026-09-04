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

    @Column(nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    private String name;

    @Column(nullable = false, unique = true, length = 150, columnDefinition = "VARCHAR(150)")
    private String email;

    @Column(length = 15, columnDefinition = "VARCHAR(15)")
    private String phone;

    @Column(length = 16, columnDefinition = "VARCHAR(16)")
    private String nik;

    @Column(nullable = false, unique = true, length = 20, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private String username = UUID.randomUUID().toString().substring(0, 20);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    @Builder.Default
    private Role role = Role.CUSTOMER;

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

    public enum Role {
        CUSTOMER, ORGANIZER, ADMIN
    }
}
