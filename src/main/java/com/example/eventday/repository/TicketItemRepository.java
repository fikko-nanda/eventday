package com.example.eventday.repository;

import com.example.eventday.entity.TicketItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketItemRepository extends JpaRepository<TicketItem, UUID>, JpaSpecificationExecutor<TicketItem> {

    List<TicketItem> findByOrderCustomerUserIdOrderByCreatedAtDesc(UUID customerId);

    List<TicketItem> findByOrderCustomerEmailOrderByCreatedAtDesc(String email);

    List<TicketItem> findByAttendeeEmailOrOrderCustomerEmailOrderByCreatedAtDesc(String attendeeEmail, String customerEmail);

    // Query spesifik berdasarkan attendeeEmail (Case-Insensitive)
    List<TicketItem> findByAttendeeEmailIgnoreCaseOrderByCreatedAtDesc(String attendeeEmail);

    // Query pencarian fleksibel di kolom attendeeEmail ATAU order.customer.email (Case-Insensitive)
    List<TicketItem> findByAttendeeEmailIgnoreCaseOrOrderCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String attendeeEmail, String customerEmail);

    List<TicketItem> findByOrderOrderId(UUID orderId);
}