package com.example.eventday.repository;

import com.example.eventday.entity.RefundRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<RefundRequestEntity, UUID> {

    @Query("SELECT r FROM RefundRequestEntity r WHERE r.customerId = :customerId ORDER BY r.createdAt DESC")
    List<RefundRequestEntity> findByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT r FROM RefundRequestEntity r WHERE r.organizerId = :organizerId ORDER BY r.createdAt DESC")
    List<RefundRequestEntity> findByOrganizerId(@Param("organizerId") UUID organizerId);

    @Query("SELECT r FROM RefundRequestEntity r WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<RefundRequestEntity> findByStatus(@Param("status") String status);

    @Query("SELECT r FROM RefundRequestEntity r WHERE r.organizerId = :organizerId AND r.status = :status ORDER BY r.createdAt DESC")
    List<RefundRequestEntity> findByOrganizerIdAndStatus(
            @Param("organizerId") UUID organizerId,
            @Param("status") String status);

    @Query("SELECT r FROM RefundRequestEntity r ORDER BY r.createdAt DESC")
    List<RefundRequestEntity> findAllByOrderByCreatedAtDesc();
}
