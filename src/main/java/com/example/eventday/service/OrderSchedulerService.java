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

    /**
     * Menjalankan pengecekan order kedaluwarsa setiap 1 menit.
     * Mengubah status dari PENDING / WAITING_PAYMENT menjadi EXPIRED
     * dan mengembalikan stok tiket yang terikat.
     */
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
                try {
                    // Update status order
                    order.setStatus("EXPIRED");
                    orderRepository.save(order);

                    // Kembalikan stok/kuota tiket dan hapus hold jika ada
                    orderService.handleExpiredOrCancelledOrder(order);

                    log.info("Order ID {} berhasil diubah menjadi EXPIRED", order.getOrderId());
                } catch (Exception e) {
                    log.error("Gagal memproses kadaluwarsa untuk Order ID {}: {}", order.getOrderId(), e.getMessage(), e);
                }
            }
        }
    }
}