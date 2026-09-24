package com.example.eventday;

import com.example.eventday.entity.EOPayoutBalance;
import com.example.eventday.entity.Event;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.Organizer;
import com.example.eventday.entity.TicketTier;
import com.example.eventday.entity.User;
import com.example.eventday.repository.EOPayoutBalanceRepository;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.OrganizerRepository;
import com.example.eventday.repository.TicketTierRepository;
import com.example.eventday.repository.UserRepository;
import com.example.eventday.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class FinancialFlowIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EOPayoutBalanceRepository balanceRepository;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketTierRepository ticketTierRepository;

    @Test
    @DisplayName("Skenario 1: Webhook Midtrans Settlement memperbarui status Order ke PAID dan meng-update Saldo EO")
    void testMidtransWebhookSettlement_ShouldUpdateOrderStatusAndBalance() {
        // 1. Arrange: Buat User
        User user = new User();
        user.setName("EO Test User");
        user.setEmail("eo_test_" + System.currentTimeMillis() + "@test.com");
        user = userRepository.save(user);

        // 2. Arrange: Buat Organizer
        Organizer organizer = new Organizer();
        organizer.setNameOrganizer("Organizer Testing");
        organizer.setUser(user);
        organizer = organizerRepository.save(organizer);

        // 3. Arrange: Buat Event
        Event event = new Event();
        event.setOrganizer(organizer);
        event.setTitle("Test Event Integration");
        event.setStartDate(LocalDateTime.now().plusDays(1));
        event.setEndDate(LocalDateTime.now().plusDays(2));
        event = eventRepository.save(event);

        // 4. Arrange: Buat Ticket Tier (dengan total_quota dan available_quota)
        TicketTier tier = new TicketTier();
        tier.setEvent(event);
        tier.setTierName("VIP Test Tier");
        tier.setPrice(new BigDecimal("50000.00"));
        tier.setTotalQuota(100);
        tier.setAvailableQuota(100);
        tier = ticketTierRepository.save(tier);

        // 5. Arrange: Buat Order
        Order order = new Order();
        order.setCustomer(user);
        order.setEvent(event);
        order.setTicketTier(tier);
        order.setStatus("PENDING");
        order.setQuantity(2);
        order.setAdminFee(new BigDecimal("5000.00"));
        order.setTotalAmount(new BigDecimal("100000.00"));
        order.setExpiredAt(LocalDateTime.now().plusDays(1));
        order = orderRepository.save(order);

        String orderIdStr = "ORD-" + order.getOrderId().toString();

        // Simulate Payload Notification Midtrans
        Map<String, Object> payload = new HashMap<>();
        payload.put("order_id", orderIdStr);
        payload.put("status_code", "200");
        payload.put("gross_amount", "100000.00");
        payload.put("transaction_status", "settlement");
        payload.put("fraud_status", "accept");
        payload.put("transaction_id", "TRX-MIDTRANS-12345");

        // 6. Act: Memproses notifikasi webhook
        paymentService.processMidtransNotification(payload);

        // 7. Assert: Pastikan Order berubah menjadi PAID
        Order updatedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals("PAID", updatedOrder.getStatus());
        assertNotNull(updatedOrder.getPaidAt());

        // 8. Assert: Pastikan EventListener memicu perubahan Saldo EO
        Optional<EOPayoutBalance> balanceOpt = balanceRepository.findByOrganizerIdAndEventId(
                organizer.getOrganizerId(), event.getEventId()
        );

        assertTrue(balanceOpt.isPresent(), "Record saldo EO harus terbentuk");
        EOPayoutBalance balance = balanceOpt.get();
        assertEquals(0, new BigDecimal("100000.00").compareTo(balance.getGrossSales()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(balance.getTotalAdminFees()));
    }
}