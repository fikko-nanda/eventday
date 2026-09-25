package com.example.eventday.service.organizer;

import com.example.eventday.entity.Event;
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
        Organizer org = helperService.resolveCurrentOrganizer();
        if (org == null) {
            return Map.of("total_revenue", BigDecimal.ZERO, "active_events", 0, "total_events", 0, "tickets_sold", 0);
        }

        long activeEvents = eventRepository.countByOrganizer_OrganizerIdAndStatus(org.getOrganizerId(), "PUBLISHED");
        long totalEvents = eventRepository.countByOrganizer_OrganizerId(org.getOrganizerId());

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
        metrics.put("total_events", totalEvents);
        metrics.put("tickets_sold", ticketsSold);
        metrics.put("organizer_id", org.getOrganizerId().toString());
        return metrics;
    }

    public List<Map<String, Object>> getRecentEvents() {
        Organizer org = helperService.resolveCurrentOrganizer();
        if (org == null) return Collections.emptyList();

        return eventRepository.findTop5ByOrganizer_OrganizerIdOrderByCreatedAtDesc(org.getOrganizerId())
                .stream()
                .map(this::mapEventToResponse)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getRecentTransactions() {
        Organizer org = helperService.resolveCurrentOrganizer();
        if (org == null) return Collections.emptyList();

        return orderRepository.findAll().stream()
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
    }

    private Map<String, Object> mapEventToResponse(Event event) {
        Map<String, Object> map = new HashMap<>();
        map.put("event_id", event.getEventId().toString());
        map.put("title", event.getTitle());
        map.put("description", event.getDescription());
        map.put("category", event.getCategory());
        map.put("venue_name", event.getVenueName());
        map.put("banner_url", event.getBannerUrl());
        map.put("facility", event.getFacility());
        map.put("lineup", event.getLineup());
        map.put("start_date", event.getStartDate() != null ? event.getStartDate().toString() : null);
        map.put("end_date", event.getEndDate() != null ? event.getEndDate().toString() : null);
        map.put("status", event.getStatus());
        map.put("is_featured", event.getIsFeatured());
        map.put("created_at", event.getCreatedAt() != null ? event.getCreatedAt().toString() : null);
        return map;
    }
}