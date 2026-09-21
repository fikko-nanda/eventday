package com.example.eventday.service.admin;

import com.example.eventday.dto.admin.AdminTicketListResponse;
import com.example.eventday.dto.admin.AdminTicketResponse;
import com.example.eventday.entity.Event;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.TicketItem;
import com.example.eventday.entity.TicketTier;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.TicketItemRepository;
import com.example.eventday.repository.TicketTierRepository;
import com.example.eventday.service.AuditLogService;
import com.example.eventday.service.TicketService;
import com.example.eventday.util.CsvUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTicketService {

    private final TicketItemRepository ticketItemRepository;
    private final OrderRepository orderRepository;
    private final TicketTierRepository ticketTierRepository;
    private final EventRepository eventRepository;
    private final TicketService ticketService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<AdminTicketListResponse> getTickets(UUID eventId, String status, UUID userId,
                                                     LocalDateTime dateFrom, LocalDateTime dateTo,
                                                     int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<TicketItem> spec = buildTicketSpec(eventId, status, userId, dateFrom, dateTo);
        Page<TicketItem> tickets = ticketItemRepository.findAll(spec, pageable);
        return tickets.map(this::mapToListResponse);
    }

    @Transactional(readOnly = true)
    public AdminTicketResponse getTicketDetail(UUID ticketItemId) {
        TicketItem ticket = ticketItemRepository.findById(ticketItemId)
                .orElseThrow(() -> new RuntimeException("Tiket tidak ditemukan"));
        return mapToDetailResponse(ticket);
    }

    @Transactional
    public List<AdminTicketResponse> generateTickets(UUID orderId, UUID adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order tidak ditemukan"));

        List<TicketItem> tickets = ticketService.generateTicketsForOrder(order);

        auditLogService.log(adminId, "ADMIN", "GENERATE_TICKETS",
                "Generate " + tickets.size() + " tiket untuk order: " + orderId);

        return tickets.stream().map(this::mapToDetailResponse).toList();
    }

    @Transactional
    public void revokeTicket(UUID ticketItemId, UUID adminId) {
        TicketItem ticket = ticketItemRepository.findById(ticketItemId)
                .orElseThrow(() -> new RuntimeException("Tiket tidak ditemukan"));

        if ("CHECKED_IN".equals(ticket.getCheckInStatus())) {
            throw new IllegalStateException("Tiket yang sudah check-in tidak dapat direvoke");
        }
        if ("REVOKED".equals(ticket.getCheckInStatus())) {
            throw new IllegalStateException("Tiket sudah dalam status REVOKED");
        }

        ticket.setCheckInStatus("REVOKED");
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setUpdatedBy(adminId);
        ticketItemRepository.save(ticket);

        auditLogService.log(adminId, "ADMIN", "REVOKE_TICKET",
                "Tiket " + ticketItemId + " di-revoke");
    }

    @Transactional
    public void checkInTicket(UUID ticketItemId, UUID adminId) {
        TicketItem ticket = ticketItemRepository.findById(ticketItemId)
                .orElseThrow(() -> new RuntimeException("Tiket tidak ditemukan"));

        if ("CHECKED_IN".equals(ticket.getCheckInStatus())) {
            throw new IllegalStateException("Tiket sudah dalam status CHECKED_IN");
        }
        if ("REVOKED".equals(ticket.getCheckInStatus())) {
            throw new IllegalStateException("Tiket yang sudah direvoke tidak dapat di-check-in");
        }

        ticket.setCheckInStatus("CHECKED_IN");
        ticket.setCheckInAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setUpdatedBy(adminId);
        ticketItemRepository.save(ticket);

        auditLogService.log(adminId, "ADMIN", "CHECKIN_TICKET",
                "Tiket " + ticketItemId + " di-check-in");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEventTicketInventory(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event tidak ditemukan"));
        List<TicketTier> tiers = ticketTierRepository.findByEvent(event);
        List<Order> allOrders = orderRepository.findByEvent(event);

        List<Order> paidOrders = allOrders.stream()
                .filter(o -> "PAID".equals(o.getStatus()))
                .toList();

        List<Map<String, Object>> tierInventory = new ArrayList<>();
        for (TicketTier tier : tiers) {
            UUID tierId = tier.getTierId();
            long soldCount = paidOrders.stream()
                    .filter(o -> tierId.equals(o.getTicketTier() != null ? o.getTicketTier().getTierId() : null))
                    .mapToInt(Order::getQuantity)
                    .sum();

            tierInventory.add(Map.of(
                    "tierId", tier.getTierId(),
                    "tierName", tier.getTierName(),
                    "totalQuota", tier.getTotalQuota(),
                    "availableQuota", tier.getAvailableQuota(),
                    "soldCount", soldCount
            ));
        }

        return Map.of("eventId", eventId, "eventTitle", event.getTitle(), "tiers", tierInventory);
    }

    public byte[] exportTicketsCsv(UUID eventId, String status, LocalDateTime dateFrom, LocalDateTime dateTo) {
        Specification<TicketItem> spec = buildTicketSpec(eventId, status, null, dateFrom, dateTo);
        List<TicketItem> tickets = ticketItemRepository.findAll(spec);

        StringBuilder sb = new StringBuilder();
        CsvUtil.appendCsvRow(sb, "ID", "Kode Tiket", "Event", "Tier", "Harga", "Pengunjung", "Email", "NIK", "Status Check-in", "Waktu Check-in", "Dibuat");

        for (TicketItem t : tickets) {
            String eventTitle = t.getOrder() != null && t.getOrder().getEvent() != null
                    ? t.getOrder().getEvent().getTitle() : "-";
            String tierName = t.getTier() != null ? t.getTier().getTierName() : "-";
            String tierPrice = t.getTier() != null ? t.getTier().getPrice().toString() : "0";

            CsvUtil.appendCsvRow(sb,
                    t.getTicketItemId().toString(),
                    t.getTicketItemId().toString(),
                    CsvUtil.escape(eventTitle),
                    CsvUtil.escape(tierName),
                    tierPrice,
                    CsvUtil.escape(t.getAttendeeName()),
                    CsvUtil.escape(t.getAttendeeEmail()),
                    CsvUtil.escape(t.getAttendeeNik()),
                    t.getCheckInStatus(),
                    t.getCheckInAt() != null ? t.getCheckInAt().toString() : "",
                    t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        }
        return CsvUtil.toBytes(sb.toString());
    }

    private Specification<TicketItem> buildTicketSpec(UUID eventId, String status, UUID userId,
                                                       LocalDateTime dateFrom, LocalDateTime dateTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (eventId != null) {
                predicates.add(cb.equal(root.get("order").get("event").get("eventId"), eventId));
            }
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("checkInStatus"), status.toUpperCase()));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("order").get("customer").get("userId"), userId));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AdminTicketListResponse mapToListResponse(TicketItem t) {
        Order order = t.getOrder();
        return AdminTicketListResponse.builder()
                .ticketItemId(t.getTicketItemId())
                .ticketCode(t.getTicketItemId().toString())
                .eventId(order != null && order.getEvent() != null ? order.getEvent().getEventId() : null)
                .eventTitle(order != null && order.getEvent() != null ? order.getEvent().getTitle() : "-")
                .tierName(t.getTier() != null ? t.getTier().getTierName() : "-")
                .tierPrice(t.getTier() != null ? t.getTier().getPrice() : null)
                .attendeeName(t.getAttendeeName())
                .attendeeEmail(t.getAttendeeEmail())
                .checkInStatus(t.getCheckInStatus())
                .checkInAt(t.getCheckInAt())
                .orderId(order != null ? order.getOrderId() : null)
                .orderNumber(order != null ? "ORD-" + order.getOrderId().toString().substring(0, 8).toUpperCase() : "-")
                .createdAt(t.getCreatedAt())
                .build();
    }

    private AdminTicketResponse mapToDetailResponse(TicketItem t) {
        Order order = t.getOrder();
        String customerName = order != null && order.getCustomer() != null ? order.getCustomer().getName() : "-";
        String customerEmail = order != null && order.getCustomer() != null ? order.getCustomer().getEmail() : "-";

        return AdminTicketResponse.builder()
                .ticketItemId(t.getTicketItemId())
                .ticketCode(t.getTicketItemId().toString())
                .orderId(order != null ? order.getOrderId() : null)
                .orderNumber(order != null ? "ORD-" + order.getOrderId().toString().substring(0, 8).toUpperCase() : "-")
                .eventId(order != null && order.getEvent() != null ? order.getEvent().getEventId() : null)
                .eventTitle(order != null && order.getEvent() != null ? order.getEvent().getTitle() : "-")
                .eventVenue(order != null && order.getEvent() != null ? order.getEvent().getVenueName() : "-")
                .eventDate(order != null && order.getEvent() != null ? order.getEvent().getStartDate() : null)
                .tierName(t.getTier() != null ? t.getTier().getTierName() : "-")
                .tierPrice(t.getTier() != null ? t.getTier().getPrice() : null)
                .attendeeName(t.getAttendeeName())
                .attendeeEmail(t.getAttendeeEmail())
                .attendeeNik(t.getAttendeeNik())
                .checkInStatus(t.getCheckInStatus())
                .checkInAt(t.getCheckInAt())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
