package com.example.eventday.service;

import com.example.eventday.dto.EventCardResponse;
import com.example.eventday.dto.HeroBannerResponse;
import com.example.eventday.entity.Event;
import com.example.eventday.entity.TicketTier;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.TicketTierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeSearchService {

    private final EventRepository eventRepository;
    private final TicketTierRepository ticketTierRepository;

    private static final DateTimeFormatter DATE_DISPLAY =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private static final Map<String, String> CATEGORY_LABEL = Map.of(
            "MUSIC_FESTIVAL", "Musik",
            "CONFERENCE", "Konferensi",
            "EXHIBITION", "Pameran",
            "CULINARY", "Kuliner");

    public List<HeroBannerResponse> getHeroBanners() {
        Page<Event> page = eventRepository.findFeaturedEvents(PageRequest.of(0, 5));
        if (page == null || page.getContent() == null) {
            return Collections.emptyList();
        }
        return page.getContent().stream()
                .map(e -> HeroBannerResponse.builder()
                        .id(e.getEventId() != null ? e.getEventId().toString() : null)
                        .title(e.getTitle())
                        .bannerUrl(e.getBannerUrl())
                        .eventDate(e.getStartDate())
                        .targetUrl(e.getEventId() != null ? "/events/" + e.getEventId() : null)
                        .build())
                .collect(Collectors.toList());
    }

    public Page<EventCardResponse> getHomeEventCards(Pageable pageable) {
        Page<Event> page = eventRepository.findPublishedEvents(null, null, null, pageable);
        if (page == null) {
            return Page.empty(pageable);
        }
        return page.map(this::toCard);
    }

    public List<String> getLocations() {
        List<String> locations = eventRepository.findDistinctLocations();
        return locations != null ? locations : Collections.emptyList();
    }

    public List<String> getCategories() {
        List<String> categories = eventRepository.findDistinctCategories();
        return categories != null ? categories : Collections.emptyList();
    }

    public Page<EventCardResponse> searchEvents(String keyword, String category,
                                               String location, LocalDate date,
                                               Pageable pageable) {
        String normalizedCategory = normalizeCategory(category);
        String normalizedKeyword = blankToNull(keyword);
        String normalizedLocation = blankToNull(location);
        Page<Event> page = eventRepository.searchPublishedEvents(
                normalizedKeyword, normalizedCategory, normalizedLocation, date, pageable);
        if (page == null) {
            return Page.empty(pageable);
        }
        return page.map(this::toCard);
    }

    public Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "startDate");
        }
        return switch (sort.toLowerCase()) {
            case "price_asc", "price_desc" -> Sort.by(Sort.Direction.DESC, "startDate");
            case "date_asc" -> Sort.by(Sort.Direction.ASC, "startDate");
            default -> Sort.by(Sort.Direction.DESC, "startDate");
        };
    }

    private EventCardResponse toCard(Event event) {
        BigDecimal lowest = getLowestPrice(event);
        return EventCardResponse.builder()
                .id(event.getEventId() != null ? event.getEventId().toString() : null)
                .title(event.getTitle())
                .posterUrl(event.getBannerUrl())
                .location(event.getVenueName())
                .category(event.getCategory())
                .categoryLabel(CATEGORY_LABEL.getOrDefault(event.getCategory(), event.getCategory()))
                .startDate(event.getStartDate())
                .dateDisplay(event.getStartDate() != null ? event.getStartDate().format(DATE_DISPLAY) : null)
                .lowestPrice(lowest)
                .priceDisplay(formatPrice(lowest))
                .build();
    }

    private BigDecimal getLowestPrice(Event event) {
        try {
            List<TicketTier> tiers = ticketTierRepository.findByEvent(event);
            if (tiers == null || tiers.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return tiers.stream()
                    .map(TicketTier::getPrice)
                    .filter(p -> p != null)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "Rp 0";
        }
        return "Rp " + String.format("%,d", price.longValue()).replace(",", ".");
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()
                || "Semua".equalsIgnoreCase(category) || "ALL".equalsIgnoreCase(category)) {
            return null;
        }
        return category.toUpperCase().replace(" ", "_");
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
