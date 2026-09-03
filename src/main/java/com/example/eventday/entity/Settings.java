package com.example.eventday.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settings_id", nullable = false)
    private Long settingsId;

    @Column(name = "settings_key", nullable = false, unique = true, length = 50, columnDefinition = "VARCHAR(50)")
    private String settingsKey;

    @Column(name = "settings_value", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private String settingsValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "create_by")
    private UUID createBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}