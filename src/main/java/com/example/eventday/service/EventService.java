package com.example.eventday.service;

import com.example.eventday.dto.EventCatalogResponse;
import com.example.eventday.dto.EventDetailResponse;
import com.example.eventday.entity.Event;
import com.example.eventday.entity.TicketTier;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.TicketTierRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_DISPLAY_ID = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.of("id", "ID"));
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<String, String> CATEGORY_LABEL = Map.of(
            "MUSIC_FESTIVAL", "Musik",
            "CONFERENCE", "Konferensi",
            "EXHIBITION", "Pameran",
            "CULINARY", "Kuliner"
    );

    private static final Map<String, String> STATUS_LABEL = Map.of(
            "PUBLISHED", "Tersedia",
            "COMPLETED", "Selesai",
            "CANCELLED", "Dibatalkan"
    );

    public EventCatalogResponse getEvents(String category, String search, String location,
                                          int page, int size, String sort) {
        Sort springSort = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, springSort);

        String normalizedCategory = normalizeCategory(category);

        Page<Event> eventPage = eventRepository.findPublishedEvents(
                normalizedCategory, search, location, pageable);

        List<EventCatalogResponse.EventItem> items = eventPage.getContent().stream()
                .map(this::toCatalogItem)
                .collect(Collectors.toList());

        return EventCatalogResponse.builder()
                .content(items)
                .page(eventPage.getNumber())
                .size(eventPage.getSize())
                .totalElements(eventPage.getTotalElements())
                .totalPages(eventPage.getTotalPages())
                .build();
    }

    public EventCatalogResponse getFeaturedEvents() {
        Pageable pageable = PageRequest.of(0, 3);
        Page<Event> eventPage = eventRepository.findFeaturedEvents(pageable);

        List<EventCatalogResponse.EventItem> items = eventPage.getContent().stream()
                .map(this::toCatalogItem)
                .collect(Collectors.toList());

        return EventCatalogResponse.builder()
                .content(items)
                .page(0)
                .size(items.size())
                .totalElements(items.size())
                .totalPages(1)
                .build();
    }

    public EventDetailResponse getEventDetail(UUID eventId) {
        Event event = eventRepository.findPublishedEventById(eventId);
        if (event == null) {
            throw new RuntimeException("Event tidak ditemukan");
        }

        List<TicketTier> tiers = ticketTierRepository.findByEvent(event);

        List<EventDetailResponse.TicketItem> ticketItems = tiers.stream()
                .map(this::toTicketItem)
                .collect(Collectors.toList());

        // Mengambil teks paragraf langsung dari entity Event
        String facilities = event.getFacility();

        List<EventDetailResponse.LineupItem> lineup = parseLineup(event.getLineup());

        return EventDetailResponse.builder()
                .id(event.getEventId().toString())
                .title(event.getTitle())
                .category(event.getCategory())
                .categoryLabel(CATEGORY_LABEL.getOrDefault(event.getCategory(), event.getCategory()))
                .date(event.getStartDate())
                .dateDisplay(event.getStartDate().format(DATE_DISPLAY_ID))
                .location(event.getVenueName())
                .description(event.getDescription())
                .image(event.getBannerUrl())
                .status(event.getStatus())
                .statusLabel(STATUS_LABEL.getOrDefault(event.getStatus(), event.getStatus()))
                .facilities(facilities)
                .lineup(lineup)
                .tickets(ticketItems)
                .build();
    }

    private EventCatalogResponse.EventItem toCatalogItem(Event event) {
        BigDecimal minPrice = getMinPrice(event);

        return EventCatalogResponse.EventItem.builder()
                .id(event.getEventId().toString())
                .title(event.getTitle())
                .category(event.getCategory())
                .categoryLabel(CATEGORY_LABEL.getOrDefault(event.getCategory(), event.getCategory()))
                .date(event.getStartDate())
                .dateDisplay(event.getStartDate().format(DATE_DISPLAY))
                .time(event.getStartDate().format(TIME_FORMAT))
                .location(event.getVenueName())
                .price(minPrice)
                .priceDisplay(formatPrice(minPrice))
                .image(event.getBannerUrl())
                .status("PUBLISHED".equals(event.getStatus()) ? "AVAILABLE" : event.getStatus())
                .isFeatured(event.getIsFeatured())
                .build();
    }

    private EventDetailResponse.TicketItem toTicketItem(TicketTier tier) {
        return EventDetailResponse.TicketItem.builder()
                .id(tier.getTierId().toString())
                .name(tier.getTierName())
                .label(tier.getTierName())
                .price(tier.getPrice())
                .priceDisplay(formatPrice(tier.getPrice()))
                .quota(tier.getTotalQuota())
                .remaining(tier.getAvailableQuota())
                .saleStart(tier.getCreatedAt())
                .saleEnd(null)
                .build();
    }

    private BigDecimal getMinPrice(Event event) {
        List<TicketTier> tiers = ticketTierRepository.findByEvent(event);
        return tiers.stream()
                .map(TicketTier::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return "Rp 0";
        return "Rp " + String.format("%,d", price.longValue()).replace(",", ".");
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank() || "Semua".equalsIgnoreCase(category) || "ALL".equalsIgnoreCase(category)) {
            return null;
        }
        return category.toUpperCase().replace(" ", "_");
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "startDate");
        }
        return switch (sort.toLowerCase()) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "startDate");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "startDate");
            case "date_asc" -> Sort.by(Sort.Direction.ASC, "startDate");
            default -> Sort.by(Sort.Direction.DESC, "startDate");
        };
    }

    private List<EventDetailResponse.LineupItem> parseLineup(String rawLineup) {
        if (rawLineup == null || rawLineup.isBlank()) {
            return List.of();
        }
        try {
            return new ObjectMapper().readValue(rawLineup, new TypeReference<List<EventDetailResponse.LineupItem>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}