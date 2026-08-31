package com.example.eventday.repository;

import com.example.eventday.entity.TicketItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketItemRepository extends JpaRepository<TicketItem, UUID> {
    Optional<TicketItem> findByTicketCode(String ticketCode);
    List<TicketItem> findByOrderOrderId(UUID orderId);
}