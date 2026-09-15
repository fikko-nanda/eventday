package com.example.eventday.repository;

import com.example.eventday.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByActorIdOrderByCreatedAtDesc(UUID actorId);

    // Tambahkan method ini untuk Superadmin Audit Logs (paginated)
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}