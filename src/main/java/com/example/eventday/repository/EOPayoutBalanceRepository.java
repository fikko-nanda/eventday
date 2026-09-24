package com.example.eventday.repository;

import com.example.eventday.entity.EOPayoutBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EOPayoutBalanceRepository extends JpaRepository<EOPayoutBalance, Long> {
    Optional<EOPayoutBalance> findByOrganizerIdAndEventId(UUID organizerId, UUID eventId);
}