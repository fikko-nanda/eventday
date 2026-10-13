package com.example.eventday.repository;

import com.example.eventday.model.Attendee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {
    List<Attendee> findByOrderId(String orderId);
    Optional<Attendee> findByOrderIdAndEmail(String orderId, String email);
}