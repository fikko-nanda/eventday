package com.example.eventday.repository;

import com.example.eventday.model.Attendee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {
    List<Attendee> findByOrderId(String orderId);
}