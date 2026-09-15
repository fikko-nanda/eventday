package com.example.eventday.service.admin;

import com.example.eventday.dto.TransactionHistoryResponse;
import com.example.eventday.dto.admin.AdminDashboardMetricsResponse;
import com.example.eventday.entity.Order;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.TicketItemRepository;
import com.example.eventday.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketItemRepository ticketItemRepository;

    @Transactional(readOnly = true)
    public AdminDashboardMetricsResponse getDashboardMetrics() {
        long totalUsers = userRepository.count();
        long totalEvents = eventRepository.count();
        long activeEvents = eventRepository.countByStatus("PUBLISHED");
        long totalTickets = ticketItemRepository.count();

        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .filter(o -> "PAID".equalsIgnoreCase(o.getStatus()))
                .map(Order::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminDashboardMetricsResponse.builder()
                .totalPlatformRevenue(totalRevenue)
                .totalEvents(totalEvents)
                .activeEvents(activeEvents)
                .totalUsers(totalUsers)
                .totalTicketsSold(totalTickets)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentEvents() {
        return eventRepository.findTop5ByOrderByCreatedAtDesc().stream().map(e -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", e.getEventId());
            map.put("title", e.getTitle());
            map.put("category", e.getCategory());
            map.put("status", e.getStatus());
            map.put("venueName", e.getVenueName());
            map.put("startDate", e.getStartDate());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionHistoryResponse> getRecentTransactions() {
        return orderRepository.findTop10ByOrderByCreatedAtDesc().stream().map(order ->
            TransactionHistoryResponse.builder()
                .orderId(order.getOrderId())
                .orderNumber("ORD-" + order.getOrderId().toString().substring(0, 8).toUpperCase())
                .eventTitle(order.getEvent() != null ? order.getEvent().getTitle() : "-")
                .ticketTierName(order.getTicketTier() != null ? order.getTicketTier().getTierName() : "-")
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus() != null ? order.getStatus() : "PENDING")
                .createdAt(order.getCreatedAt())
                .expiredAt(order.getExpiredAt())
                .build()
        ).collect(Collectors.toList());
    }
}