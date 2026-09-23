package com.example.eventday.repository;

import com.example.eventday.entity.Event;
import com.example.eventday.entity.TicketTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketTierRepository extends JpaRepository<TicketTier, UUID> {

    List<TicketTier> findByEvent(Event event);

    @Modifying
    @Query("UPDATE TicketTier t SET t.availableQuota = t.availableQuota - :qty " +
           "WHERE t.tierId = :tierId AND t.availableQuota >= :qty")
    int decrementAvailableQuota(@Param("tierId") UUID tierId, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE TicketTier t SET t.availableQuota = t.availableQuota + :qty " +
           "WHERE t.tierId = :tierId")
    int incrementAvailableQuota(@Param("tierId") UUID tierId, @Param("qty") int qty);
}