package com.example.eventday.service.admin;

import com.example.eventday.dto.admin.AdminTransactionListResponse;
import com.example.eventday.dto.admin.AdminTransactionResponse;
import com.example.eventday.dto.admin.AdminTransactionStatusRequest;
import com.example.eventday.entity.Order;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.service.AuditLogService;
import com.example.eventday.service.OrderService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminTransactionService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<AdminTransactionListResponse> getTransactions(String status, UUID eventId, UUID userId,
                                                               LocalDateTime dateFrom, LocalDateTime dateTo,
                                                               BigDecimal minAmount, BigDecimal maxAmount,
                                                               int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Order> spec = buildTransactionSpec(status, eventId, userId, dateFrom, dateTo, minAmount, maxAmount);
        Page<Order> orders = orderRepository.findAll(spec, pageable);
        return orders.map(this::mapToListResponse);
    }

    @Transactional(readOnly = true)
    public AdminTransactionResponse getTransactionDetail(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Transaksi tidak ditemukan"));
        return mapToDetailResponse(order);
    }

    @Transactional
    public void updateTransactionStatus(UUID orderId, String newStatus, UUID adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Transaksi tidak ditemukan"));

        String statusUpper = newStatus.toUpperCase();
        validateTransactionStatusTransition(order.getStatus(), statusUpper);

        String oldStatus = order.getStatus();
        order.setStatus(statusUpper);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(adminId);

        if ("PAID".equals(statusUpper)) {
            order.setPaidAt(LocalDateTime.now());
        }

        if ("CANCELLED".equals(statusUpper) || "REFUNDED".equals(statusUpper)) {
            orderService.handleExpiredOrCancelledOrder(order);
        }

        orderRepository.save(order);

        auditLogService.log(adminId, "ADMIN", "UPDATE_TRANSACTION_STATUS",
                "Status transaksi " + orderId + " diubah dari " + oldStatus + " menjadi " + statusUpper
                + (order.getEvent() != null ? " | Event: " + order.getEvent().getTitle() : ""));
    }

    public byte[] exportTransactionsCsv(String status, UUID eventId, UUID userId,
                                         LocalDateTime dateFrom, LocalDateTime dateTo,
                                         BigDecimal minAmount, BigDecimal maxAmount) {
        Specification<Order> spec = buildTransactionSpec(status, eventId, userId, dateFrom, dateTo, minAmount, maxAmount);
        List<Order> orders = orderRepository.findAll(spec);

        StringBuilder sb = new StringBuilder();
        CsvUtil.appendCsvRow(sb, "ID", "Nomor Order", "Event", "Tier", "Jumlah", "Total", "Status", "Pembayaran", "Tanggal Bayar", "Dibuat");

        for (Order o : orders) {
            String eventTitle = o.getEvent() != null ? o.getEvent().getTitle() : "-";
            String tierName = o.getTicketTier() != null ? o.getTicketTier().getTierName() : "-";
            String orderNumber = "ORD-" + o.getOrderId().toString().substring(0, 8).toUpperCase();

            CsvUtil.appendCsvRow(sb,
                    o.getOrderId().toString(),
                    orderNumber,
                    CsvUtil.escape(eventTitle),
                    CsvUtil.escape(tierName),
                    String.valueOf(o.getQuantity()),
                    o.getTotalAmount() != null ? o.getTotalAmount().toString() : "0",
                    o.getStatus(),
                    o.getPaymentMethod() != null ? o.getPaymentMethod() : "-",
                    o.getPaidAt() != null ? o.getPaidAt().toString() : "",
                    o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
        }
        return CsvUtil.toBytes(sb.toString());
    }

    private Specification<Order> buildTransactionSpec(String status, UUID eventId, UUID userId,
                                                       LocalDateTime dateFrom, LocalDateTime dateTo,
                                                       BigDecimal minAmount, BigDecimal maxAmount) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), status.toUpperCase()));
            }
            if (eventId != null) {
                predicates.add(cb.equal(root.get("event").get("eventId"), eventId));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("customer").get("userId"), userId));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), maxAmount));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateTransactionStatusTransition(String current, String next) {
        Set<String> valid = switch (current) {
            case "PENDING" -> Set.of("WAITING_PAYMENT", "CANCELLED", "EXPIRED");
            case "WAITING_PAYMENT" -> Set.of("PAID", "CANCELLED", "EXPIRED");
            case "PAID" -> Set.of("REFUNDED");
            default -> Set.of();
        };
        if (!valid.contains(next)) {
            throw new IllegalStateException("Transisi status dari " + current + " ke " + next + " tidak diizinkan");
        }
    }

    private AdminTransactionListResponse mapToListResponse(Order o) {
        return AdminTransactionListResponse.builder()
                .orderId(o.getOrderId())
                .orderNumber("ORD-" + o.getOrderId().toString().substring(0, 8).toUpperCase())
                .eventTitle(o.getEvent() != null ? o.getEvent().getTitle() : "-")
                .tierName(o.getTicketTier() != null ? o.getTicketTier().getTierName() : "-")
                .quantity(o.getQuantity())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus())
                .customerName(o.getCustomer() != null ? o.getCustomer().getName() : "-")
                .customerEmail(o.getCustomer() != null ? o.getCustomer().getEmail() : "-")
                .paidAt(o.getPaidAt())
                .createdAt(o.getCreatedAt())
                .build();
    }

    private AdminTransactionResponse mapToDetailResponse(Order o) {
        return AdminTransactionResponse.builder()
                .orderId(o.getOrderId())
                .orderNumber("ORD-" + o.getOrderId().toString().substring(0, 8).toUpperCase())
                .eventId(o.getEvent() != null ? o.getEvent().getEventId() : null)
                .eventTitle(o.getEvent() != null ? o.getEvent().getTitle() : "-")
                .eventVenue(o.getEvent() != null ? o.getEvent().getVenueName() : "-")
                .tierName(o.getTicketTier() != null ? o.getTicketTier().getTierName() : "-")
                .tierPrice(o.getTicketTier() != null ? o.getTicketTier().getPrice() : null)
                .quantity(o.getQuantity())
                .subtotal(o.getTotalAmount() != null && o.getAdminFee() != null
                        ? o.getTotalAmount().subtract(o.getAdminFee()) : null)
                .adminFee(o.getAdminFee())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus())
                .paymentMethod(o.getPaymentMethod())
                .transactionIdGateway(o.getTransactionIdGateway())
                .customerId(o.getCustomer() != null ? o.getCustomer().getUserId() : null)
                .customerName(o.getCustomer() != null ? o.getCustomer().getName() : "-")
                .customerEmail(o.getCustomer() != null ? o.getCustomer().getEmail() : "-")
                .paidAt(o.getPaidAt())
                .expiredAt(o.getExpiredAt())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
