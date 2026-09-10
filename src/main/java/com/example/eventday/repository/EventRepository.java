package com.example.eventday.repository;

import com.example.eventday.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("SELECT e FROM Event e WHERE " +
           "(CAST(:category AS string) IS NULL OR e.category = :category) AND " +
           "(CAST(:search AS string) IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
           "OR LOWER(e.venueName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) AND " +
           "(CAST(:location AS string) IS NULL OR LOWER(e.venueName) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%'))) AND " +
           "e.status = 'PUBLISHED'")
    Page<Event> findPublishedEvents(
            @Param("category") String category,
            @Param("search") String search,
            @Param("location") String location,
            Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.isFeatured = true AND e.status = 'PUBLISHED'")
    Page<Event> findFeaturedEvents(Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.eventId = :id AND e.status = 'PUBLISHED'")
    Event findPublishedEventById(@Param("id") UUID id);
}
