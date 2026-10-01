package com.example.eventday.repository;

import com.example.eventday.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC")
    List<AuditLog> findAllForExport();

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(a.actorName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.detail) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:action IS NULL OR :action = '' OR a.action = :action) AND " +
           "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.createdAt <= :endDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByFilters(
            @Param("search") String search,
            @Param("action") String action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
