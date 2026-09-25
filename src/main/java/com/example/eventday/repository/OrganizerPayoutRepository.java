package com.example.eventday.repository;

import com.example.eventday.entity.OrganizerPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizerPayoutRepository extends JpaRepository<OrganizerPayout, UUID> {

    // Method untuk mengambil pencairan berdasarkan ID Organizer
    List<OrganizerPayout> findByOrganizerOrganizerId(UUID organizerId);

    // Method untuk filter status pencairan dari sisi admin
    List<OrganizerPayout> findByStatusIgnoreCase(String status);

    // Method untuk mengambil semua daftar pencairan diurutkan dari yang terbaru
    List<OrganizerPayout> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM OrganizerPayout p WHERE p.organizer.organizerId = :organizerId AND p.status IN ('APPROVED','SUCCESS','COMPLETED')")
    BigDecimal sumApprovedPayoutByOrganizer(@Param("organizerId") UUID organizerId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM OrganizerPayout p WHERE p.organizer.organizerId = :organizerId AND p.status IN ('PENDING','PENDING_APPROVAL')")
    BigDecimal sumPendingPayoutByOrganizer(@Param("organizerId") UUID organizerId);
}