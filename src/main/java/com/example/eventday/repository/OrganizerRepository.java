package com.example.eventday.repository;

import com.example.eventday.entity.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizerRepository extends JpaRepository<Organizer, UUID> {

    Optional<Organizer> findFirstByOrderByCreatedAtAsc();

    List<Organizer> findByVerificationStatus(String verificationStatus);
    List<Organizer> findAllByOrderByCreatedAtDesc();

    Optional<Organizer> findByUserUserId(UUID userId);
}