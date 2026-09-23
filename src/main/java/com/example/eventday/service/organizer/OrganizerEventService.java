package com.example.eventday.service.organizer;

import com.example.eventday.entity.Organizer;
import com.example.eventday.entity.Event;
import com.example.eventday.entity.TicketTier;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.TicketTierRepository;
import com.example.eventday.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerEventService {

    private final OrganizerHelperService helperService;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketTierRepository ticketTierRepository;
    private final FileStorageService fileStorageService;

    public Map<String, Object> uploadBanner(MultipartFile file) {
        String url = fileStorageService.saveImage(file, "event-banners");
        Map<String, Object> data = new HashMap<>();
        data.put("banner_url", url);
        data.put("file_name", file.getOriginalFilename());
        data.put("file_size", file.getSize());
        return data;
    }

    public List<Map<String, Object>> getOrganizerEvents() {
        Organizer org = helperService.resolveCurrentOrganizer();
        if (org == null) return Collections.emptyList();

        return eventRepository.findByOrganizer_OrganizerIdOrderByCreatedAtDesc(org.getOrganizerId())
                .stream()
                .map(this::mapEventToResponse)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getDraftEvents() {
        Organizer org = helperService.resolveCurrentOrganizer();
        if (org == null) return Collections.emptyList();

        return eventRepository.findByOrganizer_OrganizerIdAndStatusOrderByCreatedAtDesc(org.getOrganizerId(), "DRAFT")
                .stream()
                .map(this::mapEventToResponse)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getEventDetailById(UUID eventId) {
        Organizer org = helperService.resolveCurrentOrganizer();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event tidak ditemukan"));

        // Ownership Validation
        if (event.getOrganizer() == null || !event.getOrganizer().getOrganizerId().equals(org.getOrganizerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Akses ditolak: Anda bukan pemilik event ini");
        }

        return mapEventToResponse(event);
    }

    @Transactional
    public Map<String, Object> createEvent(Map<String, Object> payload, MultipartFile bannerFile) {
        Organizer org = helperService.resolveCurrentOrganizer();
        UUID currentUid = helperService.currentUserId();

        if (bannerFile != null && !bannerFile.isEmpty()) {
            payload.put("banner_url", fileStorageService.saveImage(bannerFile, "event-banners"));
        }

        LocalDateTime start = parseDateTime(payload.get("startDate") != null ? payload.get("startDate") : payload.get("start_date"));
        if (start == null) start = LocalDateTime.now().plusDays(7);
        LocalDateTime end = parseDateTime(payload.get("endDate") != null ? payload.get("endDate") : payload.get("end_date"));
        if (end == null) end = start.plusDays(1);

        Event event = Event.builder()
                 .organizer(org)
                 .title((String) payload.getOrDefault("title", "Untitled Event"))
                 .description((String) payload.get("description"))
                 .category((String) payload.getOrDefault("category", "Music"))
                 .venueName((String) payload.getOrDefault("venue_name", payload.getOrDefault("venueName", "TBA")))
                 .bannerUrl((String) payload.getOrDefault("banner_url", payload.get("bannerUrl")))
                 .facility(payload.get("facilities") != null ? String.join(", ", (List<String>) payload.get("facilities")) : "")
                 .lineup((String) payload.get("lineup"))
                 .startDate(start)
                 .endDate(end)
                 .status("DRAFT")
                 .isFeatured(false)
                 .createBy(currentUid)
                 .build();

        Event saved = eventRepository.save(event);

        // --- PROSES SIMPAN TICKET TIERS ---
        List<Map<String, Object>> ticketsResponse = new ArrayList<>();
        Object ticketsRaw = payload.get("tickets");

        if (ticketsRaw instanceof List<?> ticketList) {
            for (Object item : ticketList) {
                if (item instanceof Map<?, ?> rawMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tMap = (Map<String, Object>) rawMap;

                    Object nameObj = tMap.get("tier_name");
                    if (nameObj == null) nameObj = tMap.get("tierName");
                    String tierName = nameObj != null ? String.valueOf(nameObj) : "Regular";

                    BigDecimal price = BigDecimal.ZERO;
                    if (tMap.get("price") != null) {
                        try {
                            price = new BigDecimal(String.valueOf(tMap.get("price")));
                        } catch (Exception ignored) {}
                    }

                    int totalQuota = 0;
                    Object quotaRaw = tMap.get("total_quota");
                    if (quotaRaw == null) quotaRaw = tMap.get("totalQuota");
                    if (quotaRaw == null) quotaRaw = tMap.get("quota");
                    if (quotaRaw != null) {
                        try {
                            totalQuota = Integer.parseInt(String.valueOf(quotaRaw));
                        } catch (Exception ignored) {}
                    }

                    TicketTier tier = TicketTier.builder()
                            .event(saved)
                            .tierName(tierName)
                            .price(price)
                            .totalQuota(totalQuota)
                            .availableQuota(totalQuota)
                            .createBy(currentUid)
                            .build();

                    TicketTier savedTier = ticketTierRepository.save(tier);

                    Map<String, Object> tierData = new HashMap<>();
                    tierData.put("tier_id", savedTier.getTierId() != null ? savedTier.getTierId().toString() : null);
                    tierData.put("tier_name", savedTier.getTierName());
                    tierData.put("price", savedTier.getPrice());
                    tierData.put("total_quota", savedTier.getTotalQuota());
                    tierData.put("available_quota", savedTier.getAvailableQuota());
                    ticketsResponse.add(tierData);
                }
            }
        }

        Map<String, Object> response = mapEventToResponse(saved);
        response.put("tickets", ticketsResponse);
        return response;
    }

    @Transactional
    public Map<String, Object> updateEvent(Map<String, Object> payload, MultipartFile bannerFile) {
        Organizer org = helperService.resolveCurrentOrganizer();
        String idStr = String.valueOf(payload.getOrDefault("eventId", payload.getOrDefault("event_id", payload.get("id"))));
        if (idStr == null || "null".equals(idStr)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event ID wajib diisi");
        }

        Event event = eventRepository.findById(UUID.fromString(idStr))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event tidak ditemukan"));

        // Ownership Validation
        if (event.getOrganizer() == null || !event.getOrganizer().getOrganizerId().equals(org.getOrganizerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Akses ditolak: Anda bukan pemilik event ini");
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            payload.put("banner_url", fileStorageService.saveImage(bannerFile, "event-banners"));
        }

        if (payload.containsKey("title")) event.setTitle((String) payload.get("title"));
        if (payload.containsKey("description")) event.setDescription((String) payload.get("description"));
        if (payload.containsKey("category")) event.setCategory((String) payload.get("category"));
        if (payload.containsKey("venueName")) event.setVenueName((String) payload.get("venueName"));
        if (payload.containsKey("venue_name")) event.setVenueName((String) payload.get("venue_name"));
        if (payload.containsKey("bannerUrl")) event.setBannerUrl((String) payload.get("bannerUrl"));
        if (payload.containsKey("banner_url")) event.setBannerUrl((String) payload.get("banner_url"));
        if (payload.containsKey("facility")) event.setFacility((String) payload.get("facility"));
        if (payload.containsKey("lineup")) event.setLineup((String) payload.get("lineup"));

        if (payload.containsKey("startDate") || payload.containsKey("start_date")) {
            LocalDateTime sd = parseDateTime(payload.get("startDate") != null ? payload.get("startDate") : payload.get("start_date"));
            if (sd != null) event.setStartDate(sd);
        }
        if (payload.containsKey("endDate") || payload.containsKey("end_date")) {
            LocalDateTime ed = parseDateTime(payload.get("endDate") != null ? payload.get("endDate") : payload.get("end_date"));
            if (ed != null) event.setEndDate(ed);
        }

        event.setUpdatedAt(LocalDateTime.now());
        event.setUpdatedBy(helperService.currentUserId());
        Event saved = eventRepository.save(event);
        return mapEventToResponse(saved);
    }

    @Transactional
    public Map<String, Object> publishEvent(Map<String, Object> payload) {
        Organizer org = helperService.resolveCurrentOrganizer();
        String idStr = String.valueOf(payload.getOrDefault("eventId", payload.getOrDefault("event_id", payload.get("id"))));
        if (idStr == null || "null".equals(idStr)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event ID wajib diisi");
        }

        Event event = eventRepository.findById(UUID.fromString(idStr))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event tidak ditemukan"));

        // Ownership Validation
        if (event.getOrganizer() == null || !event.getOrganizer().getOrganizerId().equals(org.getOrganizerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Akses ditolak: Anda bukan pemilik event ini");
        }

        event.setStatus("PENDING_APPROVAL");
        event.setUpdatedAt(LocalDateTime.now());
        event.setUpdatedBy(helperService.currentUserId());
        Event saved = eventRepository.save(event);

        Map<String, Object> res = new HashMap<>();
        res.put("event_id", saved.getEventId().toString());
        res.put("title", saved.getTitle());
        res.put("status", saved.getStatus());
        res.put("message", "Event berhasil dikirim untuk persetujuan admin");
        return res;
    }

    public Map<String, Object> getEventSalesSummary(UUID eventId) {
        Organizer org = helperService.resolveCurrentOrganizer();
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event tidak ditemukan"));

        // Ownership Validation
        if (event.getOrganizer() == null || !event.getOrganizer().getOrganizerId().equals(org.getOrganizerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Akses ditolak: Anda bukan pemilik event ini");
        }

        long ticketsSold = orderRepository.findAll().stream()
                .filter(o -> o.getEvent() != null && o.getEvent().getEventId().equals(eventId))
                .mapToLong(o -> o.getQuantity() != null ? o.getQuantity() : 0)
                .sum();

        double revenue = orderRepository.findAll().stream()
                .filter(o -> o.getEvent() != null && o.getEvent().getEventId().equals(eventId))
                .filter(o -> "PAID".equals(o.getStatus()))
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0)
                .sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("event_id", event.getEventId().toString());
        summary.put("title", event.getTitle());
        summary.put("status", event.getStatus());
        summary.put("tickets_sold", ticketsSold);
        summary.put("total_revenue", revenue);
        return summary;
    }

    private LocalDateTime parseDateTime(Object raw) {
        if (raw == null) return null;
        String s = String.valueOf(raw).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
        s = s.replace(" ", "T");
        // handle date-only e.g. 2026-09-23
        if (s.length() == 10) s = s + "T00:00:00";
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd"};
        for (String pat : patterns) {
            try {
                if (pat.equals("yyyy-MM-dd")) return java.time.LocalDate.parse(s.substring(0, 10)).atStartOfDay();
                return LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern(pat));
            } catch (Exception ignored) {}
        }
        try { return LocalDateTime.parse(s); } catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format tanggal tidak valid: " + raw); }
    }

    private Map<String, Object> mapEventToResponse(Event event) {
        Map<String, Object> map = new HashMap<>();
        map.put("event_id", event.getEventId().toString());
        map.put("organizer_id", event.getOrganizer() != null ? event.getOrganizer().getOrganizerId().toString() : null);
        map.put("organizerId", event.getOrganizer() != null ? event.getOrganizer().getOrganizerId().toString() : null);
        map.put("title", event.getTitle());
        map.put("description", event.getDescription());
        map.put("category", event.getCategory());
        map.put("venue_name", event.getVenueName());
        map.put("banner_url", event.getBannerUrl());
        map.put("facility", event.getFacility() != null ? event.getFacility() : "");
        map.put("lineup", event.getLineup() != null ? event.getLineup() : "");
        map.put("start_date", event.getStartDate() != null ? event.getStartDate().toString() : null);
        map.put("end_date", event.getEndDate() != null ? event.getEndDate().toString() : null);
        map.put("status", event.getStatus());
        map.put("is_featured", event.getIsFeatured());
        map.put("created_at", event.getCreatedAt() != null ? event.getCreatedAt().toString() : null);
        return map;
    }
}