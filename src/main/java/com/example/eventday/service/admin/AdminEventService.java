package com.example.eventday.service.admin;

import com.example.eventday.dto.CreateEventRequest;
import com.example.eventday.dto.admin.*;
import com.example.eventday.entity.*;
import com.example.eventday.repository.*;
import com.example.eventday.service.AuditLogService;
import com.example.eventday.service.FileStorageService;
import com.example.eventday.util.CsvUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;
    private final OrderRepository orderRepository;
    private final OrganizerRepository organizerRepository;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public Page<AdminEventListResponse> getEvents(String status, String category, UUID organizerId,
                                                   LocalDateTime dateFrom, LocalDateTime dateTo,
                                                   String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Event> spec = buildEventSpec(status, category, organizerId, dateFrom, dateTo, search);
        Page<Event> events = eventRepository.findAll(spec, pageable);
        return events.map(this::mapToListResponse);
    }

    @Transactional(readOnly = true)
    public AdminEventResponse getEventDetail(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan"));
        return mapToDetailResponse(event);
    }

    @Transactional
    public AdminEventResponse createEvent(CreateEventRequest request, MultipartFile bannerFile, UUID adminId) {
        if (bannerFile != null && !bannerFile.isEmpty()) {
            request.setBannerUrl(fileStorageService.saveImage(bannerFile, "event-banners"));
        }

        // Isolasi: organizerId opsional dari payload admin
        // Jika dikirim → assign ke organizer tsb (untuk assign event ke EO). Jika null → event murni Admin (organizer=null, tampil via LEFT JOIN)
        Organizer organizer = null;
        if (request.getOrganizerId() != null) {
            organizer = organizerRepository.findById(request.getOrganizerId())
                    .orElseThrow(() -> new RuntimeException("Organizer tidak ditemukan: " + request.getOrganizerId()));
        }

        Event event = Event.builder()
                .organizer(organizer)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .venueName(request.getVenueName())
                .bannerUrl(request.getBannerUrl())
                .facility(request.getFacilities() != null ? String.join(", ", request.getFacilities()) : "")
                .startDate(request.getEventDate() != null ? request.getEventDate() : LocalDateTime.now().plusDays(7))
                .endDate(request.getEventDate() != null ? request.getEventDate().plusHours(8) : LocalDateTime.now().plusDays(7).plusHours(8))
                .status("PUBLISHED")
                .isFeatured(false)
                .createBy(adminId)
                .build();

        Event savedEvent = eventRepository.save(event);

        if (request.getTicketTiers() != null && !request.getTicketTiers().isEmpty()) {
            for (CreateEventRequest.TicketTierDto tierDto : request.getTicketTiers()) {
                TicketTier tier = TicketTier.builder()
                        .event(savedEvent)
                        .tierName(tierDto.getName())
                        .price(tierDto.getPrice())
                        .totalQuota(tierDto.getQuota())
                        .availableQuota(tierDto.getQuota())
                        .createBy(adminId)
                        .build();
                ticketTierRepository.save(tier);
            }
        }

        auditLogService.log(adminId, "ADMIN", "CREATE_EVENT",
                "Membuat event: " + savedEvent.getTitle() + " (" + savedEvent.getEventId() + ") - Status: PUBLISHED");

        return mapToDetailResponse(savedEvent);
    }

    @Transactional
    public AdminEventResponse updateEvent(UUID eventId, CreateEventRequest request, MultipartFile bannerFile, UUID adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan"));
        // Update organizer if organizerId provided; if null → no change to avoid accidental removal
        if (request.getOrganizerId() != null) {
            Organizer newOrg = organizerRepository.findById(request.getOrganizerId())
                    .orElseThrow(() -> new RuntimeException("Organizer tidak ditemukan: " + request.getOrganizerId()));
            event.setOrganizer(newOrg);
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            request.setBannerUrl(fileStorageService.saveImage(bannerFile, "event-banners"));
        }

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setVenueName(request.getVenueName());
        if (request.getEventDate() != null) {
            event.setStartDate(request.getEventDate());
            event.setEndDate(request.getEventDate().plusHours(8));
        }
        event.setBannerUrl(request.getBannerUrl());
        event.setFacility(request.getFacilities() != null ? String.join(", ", request.getFacilities()) : null);
        event.setUpdatedAt(LocalDateTime.now());
        event.setUpdatedBy(adminId);

        Event savedEvent = eventRepository.save(event);

        auditLogService.log(adminId, "ADMIN", "UPDATE_EVENT",
                "Memperbarui event: " + savedEvent.getTitle() + " (" + savedEvent.getEventId() + ")");

        return mapToDetailResponse(savedEvent);
    }

    @Transactional
    public void updateEventStatus(UUID eventId, String newStatus, UUID adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan"));

        String statusUpper = newStatus.toUpperCase();

        // No-op jika status sudah sama
        if (statusUpper.equals(event.getStatus())) {
            return;
        }

        validateStatusTransition(event.getStatus(), statusUpper);

        // Hanya periksa tier saat approve dari PENDING_APPROVAL → PUBLISHED
        if ("PUBLISHED".equals(statusUpper) && "PENDING_APPROVAL".equals(event.getStatus())) {
            List<TicketTier> tiers = ticketTierRepository.findByEvent(event);
            if (tiers.isEmpty()) {
                throw new IllegalStateException("Event harus memiliki minimal 1 ticket tier untuk dipublikasikan");
            }
        }

        event.setStatus(statusUpper);
        event.setUpdatedAt(LocalDateTime.now());
        event.setUpdatedBy(adminId);
        eventRepository.save(event);

        auditLogService.log(adminId, "ADMIN", "UPDATE_EVENT_STATUS",
                "Status event " + event.getTitle() + " diubah menjadi: " + statusUpper);
    }

    @Transactional
    public void publishEvent(UUID eventId, UUID adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan"));

        if ("PUBLISHED".equals(event.getStatus())) {
            return; // Sudah publish, no-op
        }

        // DRAFT / PENDING_APPROVAL → PUBLISHED langsung tanpa check tier
        event.setStatus("PUBLISHED");
        event.setUpdatedAt(LocalDateTime.now());
        event.setUpdatedBy(adminId);
        eventRepository.save(event);

        auditLogService.log(adminId, "ADMIN", "PUBLISH_EVENT",
                "Event " + event.getTitle() + " dipublikasikan langsung (admin)");
    }

    @Transactional
    public void deleteEvent(UUID eventId, UUID adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan"));

        event.setStatus("DELETED");
        event.setUpdatedAt(LocalDateTime.now());
        event.setUpdatedBy(adminId);
        eventRepository.save(event);

        auditLogService.log(adminId, "ADMIN", "DELETE_EVENT",
                "Menghapus event: " + event.getTitle() + " (" + event.getEventId() + ")");
    }

    @Transactional
    public void approveEOEvent(UUID eventId, UUID adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan"));

        if (!"PENDING_APPROVAL".equals(event.getStatus())) {
            throw new IllegalStateException("Event tidak dalam status menunggu persetujuan (status saat ini: " + event.getStatus() + ")");
        }

        List<TicketTier> tiers = ticketTierRepository.findByEvent(event);
        if (tiers.isEmpty()) {
            throw new IllegalStateException("Event harus memiliki minimal 1 ticket tier untuk disetujui");
        }

        event.setStatus("PUBLISHED");
        event.setUpdatedAt(LocalDateTime.now());
        event.setUpdatedBy(adminId);
        eventRepository.save(event);

        auditLogService.log(adminId, "ADMIN", "APPROVE_EO_EVENT",
                "Menyetujui event EO: " + event.getTitle() + " (" + event.getEventId() + ")");
    }

    @Transactional
    public void rejectEOEvent(UUID eventId, String rejectionReason, UUID adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan"));

        if (!"PENDING_APPROVAL".equals(event.getStatus())) {
            throw new IllegalStateException("Event tidak dalam status menunggu persetujuan (status saat ini: " + event.getStatus() + ")");
        }

        event.setStatus("REJECTED");
        event.setUpdatedAt(LocalDateTime.now());
        event.setUpdatedBy(adminId);
        eventRepository.save(event);

        String reason = (rejectionReason != null && !rejectionReason.isBlank()) ? rejectionReason : "Tidak disebutkan";
        auditLogService.log(adminId, "ADMIN", "REJECT_EO_EVENT",
                "Menolak event EO: " + event.getTitle() + " (" + event.getEventId() + ") - Alasan: " + reason);
    }

    @Transactional(readOnly = true)
    public AdminEventSalesResponse getEventSales(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan"));

        List<TicketTier> tiers = ticketTierRepository.findByEvent(event);
        List<Order> allOrders = orderRepository.findByEvent(event);

        List<Order> paidOrPendingOrders = allOrders.stream()
                .filter(o -> "PAID".equals(o.getStatus()) || "WAITING_PAYMENT".equals(o.getStatus()))
                .toList();

        long totalOrders = paidOrPendingOrders.size();

        long ticketsSold = paidOrPendingOrders.stream()
                .filter(o -> "PAID".equals(o.getStatus()))
                .mapToInt(Order::getQuantity)
                .sum();

        BigDecimal revenuePaid = paidOrPendingOrders.stream()
                .filter(o -> "PAID".equals(o.getStatus()))
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal revenuePending = paidOrPendingOrders.stream()
                .filter(o -> "WAITING_PAYMENT".equals(o.getStatus()))
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Order> paidOrders = allOrders.stream()
                .filter(o -> "PAID".equals(o.getStatus()))
                .toList();

        Map<String, AdminEventSalesResponse.TierSales> salesByTier = new LinkedHashMap<>();
        for (TicketTier tier : tiers) {
            UUID tierId = tier.getTierId();
            long tierSold = paidOrders.stream()
                    .filter(o -> tierId.equals(o.getTicketTier() != null ? o.getTicketTier().getTierId() : null))
                    .mapToInt(Order::getQuantity)
                    .sum();

            BigDecimal tierRevenue = paidOrders.stream()
                    .filter(o -> tierId.equals(o.getTicketTier() != null ? o.getTicketTier().getTierId() : null))
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            salesByTier.put(tier.getTierName(), AdminEventSalesResponse.TierSales.builder()
                    .tierName(tier.getTierName())
                    .totalQuota(tier.getTotalQuota())
                    .availableQuota(tier.getAvailableQuota())
                    .soldCount(tierSold)
                    .revenue(tierRevenue)
                    .build());
        }

        return AdminEventSalesResponse.builder()
                .totalOrders(totalOrders)
                .totalTicketsSold(ticketsSold)
                .totalRevenuePaid(revenuePaid)
                .totalRevenuePending(revenuePending)
                .salesByTier(salesByTier)
                .build();
    }

    public byte[] exportEventsCsv(String status, String category, UUID organizerId,
                                  LocalDateTime dateFrom, LocalDateTime dateTo, String search) {
        Specification<Event> spec = buildEventSpec(status, category, organizerId, dateFrom, dateTo, search);
        List<Event> events = eventRepository.findAll(spec);

        StringBuilder sb = new StringBuilder();
        CsvUtil.appendCsvRow(sb, "ID", "Judul", "Kategori", "Venue", "Tanggal Mulai", "Tanggal Selesai", "Status", "Featured", "Organizer", "Dibuat");

        for (Event e : events) {
            CsvUtil.appendCsvRow(sb,
                    e.getEventId().toString(),
                    CsvUtil.escape(e.getTitle()),
                    CsvUtil.escape(e.getCategory()),
                    CsvUtil.escape(e.getVenueName()),
                    e.getStartDate() != null ? e.getStartDate().toString() : "",
                    e.getEndDate() != null ? e.getEndDate().toString() : "",
                    e.getStatus(),
                    e.getIsFeatured() != null && e.getIsFeatured() ? "Ya" : "Tidak",
                    CsvUtil.escape(getOrganizerName(e.getOrganizer())),
                    e.getCreatedAt() != null ? e.getCreatedAt().toString() : "");
        }
        return CsvUtil.toBytes(sb.toString());
    }

    private Specification<Event> buildEventSpec(String status, String category, UUID organizerId,
                                                 LocalDateTime dateFrom, LocalDateTime dateTo, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), status.toUpperCase()));
            }
            if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (organizerId != null) {
                predicates.add(cb.equal(root.get("organizer").get("organizerId"), organizerId));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), dateTo));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate venueMatch = cb.like(cb.lower(root.get("venueName")), pattern);
                predicates.add(cb.or(titleMatch, venueMatch));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateStatusTransition(String current, String next) {
        Set<String> valid = switch (current) {
            case "DRAFT" -> Set.of("PUBLISHED", "CANCELLED", "DELETED");
            case "PENDING_APPROVAL" -> Set.of("PUBLISHED", "REJECTED", "DRAFT", "CANCELLED", "DELETED");
            case "PUBLISHED" -> Set.of("DRAFT", "CANCELLED", "COMPLETED", "DELETED");
            case "REJECTED" -> Set.of("DRAFT", "DELETED");
            case "CANCELLED" -> Set.of("DELETED");
            case "DELETED" -> Set.of();
            default -> Set.of("DRAFT", "PUBLISHED", "CANCELLED", "DELETED");
        };
        if (!valid.contains(next)) {
            throw new IllegalStateException("Transisi status dari " + current + " ke " + next + " tidak diizinkan");
        }
    }


    private AdminEventListResponse mapToListResponse(Event e) {
        return AdminEventListResponse.builder()
                .eventId(e.getEventId())
                .title(e.getTitle())
                .category(e.getCategory())
                .venueName(e.getVenueName())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .status(e.getStatus())
                .isFeatured(e.getIsFeatured())
                .organizerId(e.getOrganizer() != null ? e.getOrganizer().getOrganizerId() : null)
                .organizerName(getOrganizerName(e.getOrganizer()))
                .bannerUrl(e.getBannerUrl())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private AdminEventResponse mapToDetailResponse(Event e) {
        List<TicketTier> tiers = ticketTierRepository.findByEvent(e);
        List<Order> allOrders = orderRepository.findByEvent(e);

        List<Order> paidOrders = allOrders.stream()
                .filter(o -> "PAID".equals(o.getStatus()))
                .toList();

        List<AdminEventResponse.TierInfo> tierInfos = tiers.stream().map(t -> {
            UUID tierId = t.getTierId();
            long sold = paidOrders.stream()
                    .filter(o -> tierId.equals(o.getTicketTier() != null ? o.getTicketTier().getTierId() : null))
                    .mapToInt(Order::getQuantity)
                    .sum();
            return AdminEventResponse.TierInfo.builder()
                    .tierId(t.getTierId())
                    .tierName(t.getTierName())
                    .price(t.getPrice())
                    .totalQuota(t.getTotalQuota())
                    .availableQuota(t.getAvailableQuota())
                    .soldCount(sold)
                    .build();
        }).toList();

        return AdminEventResponse.builder()
                .eventId(e.getEventId())
                .organizerId(e.getOrganizer() != null ? e.getOrganizer().getOrganizerId() : null)
                .organizerName(getOrganizerName(e.getOrganizer()))
                .title(e.getTitle())
                .description(e.getDescription())
                .category(e.getCategory())
                .venueName(e.getVenueName())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .status(e.getStatus())
                .isFeatured(e.getIsFeatured())
                .bannerUrl(e.getBannerUrl())
                .facility(e.getFacility() != null ? e.getFacility() : "")
                .lineup(e.getLineup())
                .ticketTiers(tierInfos)
                .createdAt(e.getCreatedAt())
                .build();
    }

    private String getOrganizerName(Organizer organizer) {
        if (organizer == null) return "EventDay Official";
        return organizer.getNameOrganizer() != null ? organizer.getNameOrganizer() : "EventDay Official";
    }
}
