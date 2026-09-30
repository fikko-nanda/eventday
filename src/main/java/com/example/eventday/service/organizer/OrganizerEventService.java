package com.example.eventday.service.organizer;

import com.example.eventday.entity.Organizer;
import com.example.eventday.entity.Event;
import com.example.eventday.entity.TicketTier;
import com.example.eventday.model.Category;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.TicketTierRepository;
import com.example.eventday.service.FileStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        // Validasi judul unik (case-insensitive)
        String rawTitle = (String) payload.getOrDefault("title", "Untitled Event");
        String title = rawTitle != null ? rawTitle.trim() : "Untitled Event";
        if (eventRepository.existsByTitleIgnoreCase(title)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Judul event '" + title + "' sudah digunakan. Silakan gunakan judul lain.");
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            payload.put("banner_url", fileStorageService.saveImage(bannerFile, "event-banners"));
        }

        LocalDateTime start = parseDateTime(payload.get("startDate") != null ? payload.get("startDate") : payload.get("start_date"));
        if (start == null) start = LocalDateTime.now().plusDays(7);
        LocalDateTime end = parseDateTime(payload.get("endDate") != null ? payload.get("endDate") : payload.get("end_date"));
        if (end == null) end = start.plusDays(1);

        Event event = Event.builder()
                .organizer(org)
                .title(title)
                .description((String) payload.get("description"))
                .category(parseCategory((String) payload.getOrDefault("category", "Music")))
                .venueName((String) payload.getOrDefault("venue_name", payload.getOrDefault("venueName", "TBA")))
                .bannerUrl((String) payload.getOrDefault("banner_url", payload.get("bannerUrl")))
                .facility(payload.get("facilities") != null ? String.join(", ", (List<String>) payload.get("facilities")) : "")
                .lineup(normalizeLineup(payload.get("lineup")))
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
    public Map<String, Object> updateEvent(UUID eventId, Map<String, Object> payload, MultipartFile bannerFile) {
        Organizer org = helperService.resolveCurrentOrganizer();
        
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event tidak ditemukan"));

        // Ownership Validation
        if (event.getOrganizer() == null || !event.getOrganizer().getOrganizerId().equals(org.getOrganizerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Akses ditolak: Anda bukan pemilik event ini");
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            payload.put("banner_url", fileStorageService.saveImage(bannerFile, "event-banners"));
        }

        // Validasi judul unik saat edit (mengecualikan event yang sedang diedit)
        if (payload.containsKey("title")) {
            String newTitle = String.valueOf(payload.get("title")).trim();
            if (eventRepository.existsByTitleIgnoreCaseAndEventIdNot(newTitle, event.getEventId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Judul event '" + newTitle + "' sudah digunakan oleh event lain.");
            }
            event.setTitle(newTitle);
        }

        if (payload.containsKey("description")) event.setDescription(str(payload.get("description")));
        if (payload.containsKey("category")) event.setCategory(parseCategory(str(payload.get("category"))));
        if (payload.containsKey("venueName")) event.setVenueName(str(payload.get("venueName")));
        if (payload.containsKey("venue_name")) event.setVenueName(str(payload.get("venue_name")));
        if (payload.containsKey("location") && !payload.containsKey("venueName") && !payload.containsKey("venue_name")) {
            event.setVenueName(str(payload.get("location")));
        }
        if (payload.containsKey("bannerUrl")) event.setBannerUrl(str(payload.get("bannerUrl")));
        if (payload.containsKey("banner_url")) event.setBannerUrl(str(payload.get("banner_url")));
        if (payload.containsKey("facility")) event.setFacility(str(payload.get("facility")));
        if (payload.containsKey("facilities") && payload.get("facilities") instanceof List<?> facilities) {
            event.setFacility(facilities.stream().filter(Objects::nonNull).map(String::valueOf)
                    .collect(Collectors.joining(", ")));
        }
        if (payload.containsKey("lineup")) event.setLineup(normalizeLineup(payload.get("lineup")));

        if (payload.containsKey("startDate") || payload.containsKey("start_date") || payload.containsKey("eventDate")) {
            Object raw = payload.get("startDate") != null ? payload.get("startDate")
                    : (payload.get("start_date") != null ? payload.get("start_date") : payload.get("eventDate"));
            LocalDateTime sd = parseDateTime(raw);
            if (sd != null) event.setStartDate(sd);
        }
        if (payload.containsKey("endDate") || payload.containsKey("end_date")) {
            LocalDateTime ed = parseDateTime(payload.get("endDate") != null ? payload.get("endDate") : payload.get("end_date"));
            if (ed != null) event.setEndDate(ed);
        }

        // Update ticket tiers if provided (null/kosong → pertahankan tier lama)
        Object tiersObj = payload.get("ticketTiers") != null ? payload.get("ticketTiers") : payload.get("ticket_tiers");
        if (tiersObj instanceof List<?> tiersRaw && !tiersRaw.isEmpty()) {
            // Delete existing tiers
            ticketTierRepository.deleteByEvent_EventId(event.getEventId());

            for (Object item : tiersRaw) {
                if (!(item instanceof Map<?, ?> rawMap)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> tierData = (Map<String, Object>) rawMap;

                TicketTier tier = new TicketTier();
                tier.setEvent(event);
                Object nameObj = tierData.get("tier_name") != null ? tierData.get("tier_name")
                        : (tierData.get("tierName") != null ? tierData.get("tierName") : tierData.get("name"));
                tier.setTierName(nameObj != null ? String.valueOf(nameObj) : "Regular");
                tier.setPrice(parseBigDecimal(tierData.get("price")));
                int quota = parseIntSafe(tierData.get("totalQuota") != null ? tierData.get("totalQuota")
                        : (tierData.get("quota") != null ? tierData.get("quota") : tierData.get("total_quota")), 0);
                tier.setTotalQuota(quota);
                int available = parseIntSafe(tierData.get("availableQuota") != null ? tierData.get("availableQuota")
                        : tierData.get("available_quota") != null ? tierData.get("available_quota") : quota, quota);
                tier.setAvailableQuota(available);
                tier.setCreatedAt(LocalDateTime.now());
                tier.setCreateBy(helperService.currentUserId());
                ticketTierRepository.save(tier);
            }
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

    private String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v);
        return "null".equalsIgnoreCase(s) ? null : s;
    }

    private BigDecimal parseBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(v).trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private int parseIntSafe(Object v, int fallback) {
        if (v == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Parsing kategori yang KEBAL ERROR untuk response mapping.
     * Selalu kembalikan kode kategori String yang valid (tidak pernah throw),
     * sehingga 1 baris data legacy tidak meruntuhkan seluruh list (400).
     */
    private String parseCategorySafely(Object rawCategory) {
        if (rawCategory == null) return "MUSIC_FESTIVAL";
        String val = rawCategory.toString().trim().toUpperCase();
        switch (val) {
            case "ENTERTAINMENT":
            case "MUSIK":
            case "MUSIC":
            case "KONSER":
            case "MUSIC_FESTIVAL":
                return "MUSIC_FESTIVAL";
            case "SEMINAR":
            case "SEMINAR_WORKSHOP":
            case "WORKSHOP":
            case "CONFERENCE":
                return "CONFERENCE";
            case "PAMERAN":
            case "EXHIBITION":
                return "EXHIBITION";
            case "KULINER":
            case "CULINARY":
                return "CULINARY";
            default:
                return "MUSIC_FESTIVAL";
        }
    }

    // Konversi String kategori dari FE → enum Category.
    // TIDAK PERNAH memanggil Category.valueOf secara mentah — delegasi ke
    // Category.fromString() yang kebal error (try-catch di dalam).
    // null/kosong → OTHER agar kolom NOT NULL implisit tetap terisi.
    private Category parseCategory(String raw) {
        if (raw == null || raw.isBlank()) return Category.OTHER;
        Category parsed = Category.fromString(raw);
        return parsed != null ? parsed : Category.OTHER;
    }

    // Normalisasi lineup SEBELUM simpan ke entity (selalu JSON string):
    // - List (dari JSON array) → simpan JSON string-nya
    // - JsonNode Array → simpan string JSON-nya
    // - JsonNode Text / String "Artis A, Artis B" → pecah koma → [{"name":"Artis A","image":""}, ...] → JSON string
    // - null / kosong → null
    private String normalizeLineup(Object raw) {
        if (raw == null) return null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            if (raw instanceof JsonNode node) {
                if (node.isNull()) return null;
                if (node.isArray()) return node.toString();
                String text = node.isTextual() ? node.asText() : node.toString();
                return normalizeLineup(text);
            }
            if (raw instanceof List<?> list) {
                if (list.isEmpty()) return null;
                List<Map<String, String>> items = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) {
                        Object nameObj = m.get("name");
                        Object imgObj = m.get("image");
                        String name = nameObj != null ? String.valueOf(nameObj).trim() : "";
                        if (!name.isEmpty()) {
                            Map<String, String> entry = new LinkedHashMap<>();
                            entry.put("name", name);
                            entry.put("image", imgObj != null ? String.valueOf(imgObj) : "");
                            items.add(entry);
                        }
                    } else if (item != null) {
                        String name = String.valueOf(item).trim();
                        if (!name.isEmpty()) {
                            Map<String, String> entry = new LinkedHashMap<>();
                            entry.put("name", name);
                            entry.put("image", "");
                            items.add(entry);
                        }
                    }
                }
                if (items.isEmpty()) return null;
                return mapper.writeValueAsString(items);
            }
            String text = String.valueOf(raw).trim();
            if (text.isEmpty() || "null".equalsIgnoreCase(text)) return null;
            // Sudah JSON array string → validasi lalu simpan apa adanya
            if (text.startsWith("[")) {
                try {
                    mapper.readTree(text);
                    return text;
                } catch (Exception ignored) {
                    // bukan JSON valid → lanjut pecah koma
                }
            }
            List<Map<String, String>> items = new ArrayList<>();
            for (String part : text.split(",")) {
                String name = part.trim();
                if (!name.isEmpty()) {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("name", name);
                    entry.put("image", "");
                    items.add(entry);
                }
            }
            if (items.isEmpty()) return null;
            return mapper.writeValueAsString(items);
        } catch (Exception e) {
            log.warn("Gagal normalisasi lineup EO: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> mapEventToResponse(Event event) {
        Map<String, Object> map = new HashMap<>();
        map.put("event_id", event.getEventId().toString());
        map.put("organizer_id", event.getOrganizer() != null ? event.getOrganizer().getOrganizerId().toString() : null);
        map.put("organizerId", event.getOrganizer() != null ? event.getOrganizer().getOrganizerId().toString() : null);
        map.put("title", event.getTitle());
        map.put("description", event.getDescription());
        map.put("category", parseCategorySafely(event.getCategory()));
        map.put("venue_name", event.getVenueName());
        map.put("banner_url", event.getBannerUrl());
        map.put("facility", event.getFacility() != null ? event.getFacility() : "");
        map.put("lineup", parseLineupToMap(event.getLineup()));
        map.put("start_date", event.getStartDate() != null ? event.getStartDate().toString() : null);
        map.put("end_date", event.getEndDate() != null ? event.getEndDate().toString() : null);
        map.put("status", event.getStatus());
        map.put("is_featured", event.getIsFeatured());
        map.put("created_at", event.getCreatedAt() != null ? event.getCreatedAt().toString() : null);
        return map;
    }

    // Parse lineup dari DB (String) → List of Map (kompatibel dengan frontend)
    private List<Map<String, String>> parseLineupToMap(String rawLineup) {
        if (rawLineup == null || rawLineup.isBlank()) {
            return new ArrayList<>();
        }
        try {
            // Jika JSON, parse langsung
            if (rawLineup.trim().startsWith("[")) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(rawLineup, List.class);
                return parsed;
            }
            // Fallback: pecah koma
            return java.util.Arrays.stream(rawLineup.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(name -> {
                        Map<String, String> item = new HashMap<>();
                        item.put("name", name);
                        item.put("image", "");
                        return item;
                    })
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            // Fallback final: kembalikan sebagai list string tunggal
            List<Map<String, String>> list = new ArrayList<>();
            Map<String, String> single = new HashMap<>();
            single.put("name", rawLineup);
            single.put("image", "");
            list.add(single);
            return list;
        }
    }
}