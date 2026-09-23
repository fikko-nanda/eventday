package com.example.eventday.repository;

import com.example.eventday.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    @Query("SELECT e FROM Event e LEFT JOIN e.organizer o WHERE " +
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

    @Query("SELECT e FROM Event e LEFT JOIN e.organizer o WHERE e.isFeatured = true AND e.status = 'PUBLISHED'")
    Page<Event> findFeaturedEvents(Pageable pageable);

    @Query("SELECT e FROM Event e LEFT JOIN e.organizer o WHERE e.eventId = :id AND e.status = 'PUBLISHED'")
    Event findPublishedEventById(@Param("id") UUID id);

    // ===== MODUL 01: HOME & SEARCH =====
    // NOTE: entity memakai venueName (bukan location) dan status PUBLISHED (bukan ACTIVE)
    @Query("SELECT DISTINCT e.venueName FROM Event e WHERE e.venueName IS NOT NULL AND e.status = 'PUBLISHED' ORDER BY e.venueName ASC")
    List<String> findDistinctLocations();

    @Query("SELECT DISTINCT e.category FROM Event e WHERE e.category IS NOT NULL AND e.status = 'PUBLISHED' ORDER BY e.category ASC")
    List<String> findDistinctCategories();

    @Query("SELECT e FROM Event e LEFT JOIN e.organizer o WHERE "
            + "e.status = 'PUBLISHED' AND "
            + "(CAST(:keyword AS string) IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) "
            + "OR LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) "
            + "OR LOWER(e.venueName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) AND "
            + "(CAST(:category AS string) IS NULL OR e.category = :category) AND "
            + "(CAST(:location AS string) IS NULL OR LOWER(e.venueName) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%'))) AND "
            + "(:date IS NULL OR CAST(e.startDate AS date) = :date)")
    Page<Event> searchPublishedEvents(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("location") String location,
            @Param("date") java.time.LocalDate date,
            Pageable pageable);

    // ===== MODUL SUPERADMIN: DASHBOARD =====
    long countByStatus(String status);

    List<Event> findTop5ByOrderByCreatedAtDesc();

    // ===== MODUL ORGANIZER PORTAL =====
    List<Event> findByOrganizer_OrganizerIdOrderByCreatedAtDesc(UUID organizerId);

    List<Event> findByOrganizer_OrganizerIdAndStatusOrderByCreatedAtDesc(UUID organizerId, String status);

    List<Event> findTop5ByOrganizer_OrganizerIdOrderByCreatedAtDesc(UUID organizerId);

    long countByOrganizer_OrganizerId(UUID organizerId);

    long countByOrganizer_OrganizerIdAndStatus(UUID organizerId, String status);

    // ===== VALIDASI UNIK JUDUL EVENT =====
    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndEventIdNot(String title, UUID eventId);
}