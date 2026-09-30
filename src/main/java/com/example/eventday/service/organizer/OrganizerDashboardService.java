package com.example.eventday.service.organizer;

import com.example.eventday.entity.Event;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.Organizer;
import com.example.eventday.entity.User;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.OrganizerRepository;
import com.example.eventday.repository.TicketItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerDashboardService {

    private final OrganizerHelperService helperService;
    private final OrganizerRepository organizerRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketItemRepository ticketItemRepository;
    private final OrganizerBalanceService balanceService;

    public Map<String, Object> getOrganizerDashboard() {
        UUID uid = helperService.currentUserId();
        if (uid != null) {
            Optional<Organizer> opt = organizerRepository.findByUserUserId(uid);
            if (opt.isPresent()) {
                Organizer org = opt.get();
                List<Event> myEvents = eventRepository.findByOrganizer_OrganizerIdOrderByCreatedAtDesc(org.getOrganizerId());
                long activeEvents = myEvents.stream().filter(e -> "PUBLISHED".equals(e.getStatus())).count();

                Map<String, BigDecimal> breakdown = balanceService.calculateBalanceBreakdown(org.getOrganizerId());
                BigDecimal available = breakdown.get("available_balance");

                long ticketsSold = orderRepository.findAll().stream()
                        .filter(o -> o.getEvent() != null && o.getEvent().getOrganizer() != null
                                && o.getEvent().getOrganizer().getOrganizerId().equals(org.getOrganizerId()))
                        .mapToLong(o -> o.getQuantity() != null ? o.getQuantity() : 0)
                        .sum();

                Map<String, Object> metrics = new HashMap<>();
                metrics.put("total_revenue", available);
                metrics.put("gross_revenue", breakdown.get("gross_revenue"));
                metrics.put("total_refund", breakdown.get("total_refund"));
                metrics.put("total_payout", breakdown.get("total_payout"));
                metrics.put("available_balance", available);
                metrics.put("active_events", activeEvents);
                metrics.put("total_events", myEvents.size());
                metrics.put("tickets_sold", ticketsSold);
                metrics.put("organizer_id", org.getOrganizerId().toString());
                metrics.put("real", true);
                return metrics;
            }
        }
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_revenue", BigDecimal.ZERO);
        metrics.put("active_events", 0);
        metrics.put("tickets_sold", 0);
        metrics.put("mock", true);
        return metrics;
    }

    public Map<String, Object> getOrganizerDashboardMetrics() {
        try {
            Organizer org = helperService.findCurrentOrganizer();
            if (org == null || org.getOrganizerId() == null) {
                return zeroMetrics();
            }

            long activeEvents = eventRepository.countByOrganizer_OrganizerIdAndStatus(org.getOrganizerId(), "PUBLISHED");
            long totalEvents = eventRepository.countByOrganizer_OrganizerId(org.getOrganizerId());

            Map<String, BigDecimal> breakdown = balanceService.calculateBalanceBreakdown(org.getOrganizerId());
            BigDecimal available = breakdown != null && breakdown.get("available_balance") != null
                    ? breakdown.get("available_balance") : BigDecimal.ZERO;

            List<Order> allOrders = orderRepository.findAll();
            long ticketsSold = 0L;
            if (allOrders != null && !allOrders.isEmpty()) {
                ticketsSold = allOrders.stream()
                        .filter(o -> o.getEvent() != null && o.getEvent().getOrganizer() != null
                                && o.getEvent().getOrganizer().getOrganizerId().equals(org.getOrganizerId()))
                        .mapToLong(o -> o.getQuantity() != null ? o.getQuantity() : 0)
                        .sum();
            }

            Map<String, Object> metrics = new HashMap<>();
            metrics.put("total_revenue", available);
            metrics.put("gross_revenue", breakdown != null && breakdown.get("gross_revenue") != null
                    ? breakdown.get("gross_revenue") : BigDecimal.ZERO);
            metrics.put("total_refund", breakdown != null && breakdown.get("total_refund") != null
                    ? breakdown.get("total_refund") : BigDecimal.ZERO);
            metrics.put("total_payout", breakdown != null && breakdown.get("total_payout") != null
                    ? breakdown.get("total_payout") : BigDecimal.ZERO);
            metrics.put("available_balance", available);
            metrics.put("active_events", activeEvents);
            metrics.put("total_events", totalEvents);
            metrics.put("tickets_sold", ticketsSold);
            metrics.put("organizer_id", org.getOrganizerId().toString());
            return metrics;
        } catch (RuntimeException e) {
            log.warn("Gagal memuat metrik dashboard EO, kembalikan default 0: {}", e.getMessage());
            return zeroMetrics();
        }
    }

    /**
     * Fallback aman untuk /metrics: semua angka 0 (bentuk response sama
     * seperti sukses agar FE tidak perlu branch khusus).
     */
    private Map<String, Object> zeroMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_revenue", BigDecimal.ZERO);
        metrics.put("gross_revenue", BigDecimal.ZERO);
        metrics.put("total_refund", BigDecimal.ZERO);
        metrics.put("total_payout", BigDecimal.ZERO);
        metrics.put("available_balance", BigDecimal.ZERO);
        metrics.put("active_events", 0L);
        metrics.put("total_events", 0L);
        metrics.put("tickets_sold", 0L);
        metrics.put("organizer_id", null);
        return metrics;
    }

    public List<Map<String, Object>> getRecentEvents() {
        try {
            Organizer org = helperService.findCurrentOrganizer();
            if (org == null || org.getOrganizerId() == null) {
                return Collections.emptyList();
            }

            List<Event> events = eventRepository.findTop5ByOrganizer_OrganizerIdOrderByCreatedAtDesc(org.getOrganizerId());
            if (events == null || events.isEmpty()) {
                return Collections.emptyList();
            }
            return events.stream()
                    .filter(Objects::nonNull)
                    .map(this::mapEventToResponse)
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            log.warn("Gagal memuat recent-events EO, kembalikan list kosong: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getRecentTransactions() {
        try {
            Organizer org = helperService.findCurrentOrganizer();
            if (org == null || org.getOrganizerId() == null) {
                return Collections.emptyList();
            }

            List<Order> allOrders = orderRepository.findAll();
            if (allOrders == null || allOrders.isEmpty()) {
                return Collections.emptyList();
            }

            return allOrders.stream()
                .filter(Objects::nonNull)
                .filter(o -> o.getEvent() != null && o.getEvent().getOrganizer() != null
                        && o.getEvent().getOrganizer().getOrganizerId().equals(org.getOrganizerId()))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(5)
                .map(o -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("order_id", o.getOrderId() != null ? o.getOrderId().toString() : null);
                    m.put("event_title", o.getEvent() != null ? o.getEvent().getTitle() : "-");
                    m.put("amount", o.getTotalAmount());
                    m.put("status", o.getStatus());
                    m.put("created_at", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
                    
                    User customer = o.getCustomer();
                    if (customer != null && customer.getUserId() != null) {
                        m.put("customer_id", customer.getUserId().toString());
                        m.put("customer_name", customer.getName());
                        m.put("customer_email", customer.getEmail());
                        // Alternatif: jika pembeli mengisi data attendee terpisah, gunakan attendee pertama
                        // List<TicketItem> tickets = ticketItemRepository.findByOrderOrderId(o.getOrderId());
                        // if (!tickets.isEmpty()) {
                        //     Attendee att = tickets.get(0).getAttendee();
                        //     if (att != null && att.getFullName() != null && !att.getFullName().isBlank()) {
                        //         m.put("customer_name", att.getFullName());
                        //     }
                        // }
                    }
                    return m;
                })
                .collect(Collectors.toList());
        } catch (RuntimeException e) {
            log.warn("Gagal memuat recent-transactions EO, kembalikan list kosong: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, Object> mapEventToResponse(Event event) {
        Map<String, Object> map = new HashMap<>();
        map.put("event_id", event.getEventId().toString());
        map.put("title", event.getTitle());
        map.put("description", event.getDescription());
        map.put("category", safeCategoryCode(event.getCategory()));
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

    /**
     * Konversi category entity → kode String yang aman untuk response.
     * Tidak pernah memanggil Category.valueOf secara mentah dan tidak pernah
     * throw: nilai tak dikenal/baris legacy → "MUSIC_FESTIVAL".
     */
    private String safeCategoryCode(Object rawCategory) {
        try {
            if (rawCategory == null) return "MUSIC_FESTIVAL";
            com.example.eventday.model.Category parsed =
                    com.example.eventday.model.Category.fromString(rawCategory.toString());
            return parsed != null ? parsed.name() : "MUSIC_FESTIVAL";
        } catch (Exception e) {
            return "MUSIC_FESTIVAL";
        }
    }

    private List<Map<String, String>> parseLineupToMap(String rawLineup) {
        if (rawLineup == null || rawLineup.isBlank()) {
            return new ArrayList<>();
        }
        try {
            if (rawLineup.trim().startsWith("[")) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(rawLineup, List.class);
                return parsed;
            }
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
            List<Map<String, String>> list = new ArrayList<>();
            Map<String, String> single = new HashMap<>();
            single.put("name", rawLineup);
            single.put("image", "");
            list.add(single);
            return list;
        }
    }
}