package com.example.eventday.repository;

import com.example.eventday.entity.Event;
import com.example.eventday.entity.Event.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    
    List<Event> findByStatus(EventStatus status);
    
    // Tipe parameter userId diubah menjadi UUID
    List<Event> findByOrganizerUserId(UUID userId);
}