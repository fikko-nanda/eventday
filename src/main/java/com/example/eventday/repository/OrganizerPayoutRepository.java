package com.example.eventday.repository;

import com.example.eventday.entity.OrganizerPayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}