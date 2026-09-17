package com.example.eventday.service;

import com.example.eventday.entity.Order;
import com.example.eventday.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSchedulerService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    // Menjalankan pengecekan setiap menit
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> expiredOrders = orderRepository.findByStatusInAndExpiredAtBefore(
                List.of("PENDING", "WAITING_PAYMENT"), now
        );

        if (!expiredOrders.isEmpty()) {
            log.info("Ditemukan {} order kedaluwarsa. Memproses pembatalan...", expiredOrders.size());
            for (Order order : expiredOrders) {
                order.setStatus("EXPIRED");
                orderRepository.save(order);
                orderService.handleExpiredOrCancelledOrder(order);
            }
        }
    }
}