package com.example.eventday.repository;

import com.example.eventday.entity.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizerRepository extends JpaRepository<Organizer, UUID> {
    List<Organizer> findByVerificationStatus(String verificationStatus);
    List<Organizer> findAllByOrderByCreatedAtDesc();
}