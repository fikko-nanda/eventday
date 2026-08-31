package com.example.eventday.repository;

import com.example.eventday.entity.TicketTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketTierRepository extends JpaRepository<TicketTier, UUID> {
    List<TicketTier> findByEventEventId(UUID eventId);
}