package com.example.eventday.repository;

import com.example.eventday.entity.RescheduleRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RescheduleRequestRepository extends JpaRepository<RescheduleRequest, UUID> {
    List<RescheduleRequest> findByEventEventId(UUID eventId);
}