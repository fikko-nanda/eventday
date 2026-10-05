package com.example.eventday.service;

import com.example.eventday.dto.TicketEmailData;
import com.example.eventday.entity.Event;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.TicketItem;
import com.example.eventday.entity.TicketTier;
import com.example.eventday.entity.User;
import com.example.eventday.event.OrderCreatedEvent;
import com.example.eventday.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Menerbitkan e-ticket dan mengirimkannya ke email masing-masing peserta.
 *
 * Dipanggil lewat {@link OrderCreatedEvent} yang dipublikasikan saat order menjadi PAID,
 * baik dari webhook Midtrans maupun dari admin. Pakai AFTER_COMMIT supaya email tidak
 * terkirim kalau transaksi pembayaran ternyata di-rollback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketNotificationService {

    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"));

    private final OrderRepository orderRepository;
    private final TicketService ticketService;
    private final EmailService emailService;

    // REQUIRES_NEW wajib: AFTER_COMMIT berjalan di luar transaksi sebelumnya, dan Spring
    // menolak @Transactional default pada @TransactionalEventListener.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderPaid(OrderCreatedEvent event) {
        Order eventOrder = event != null ? event.getOrder() : null;
        if (eventOrder == null || eventOrder.getOrderId() == null) {
            log.warn("OrderCreatedEvent tanpa Order ID, pengiriman e-ticket dilewati.");
            return;
        }

        try {
            // Re-fetch dengan session sendiri: entity pada event sudah detached setelah commit.
            Order order = orderRepository.findById(eventOrder.getOrderId()).orElse(null);
            if (order == null) {
                log.warn("Order {} tidak ditemukan saat mengirim e-ticket.", eventOrder.getOrderId());
                return;
            }

            List<TicketItem> tickets = ticketService.generateTicketsForOrder(order);
            if (tickets == null || tickets.isEmpty()) {
                log.warn("Tidak ada tiket yang terbit untuk order {}.", order.getOrderId());
                return;
            }

            String customerEmail = order.getCustomer() != null ? order.getCustomer().getEmail() : null;

            // Satu email per alamat penerima. Peserta tanpa email dapat email akun pembeli.
            Map<String, List<TicketEmailData.TicketLine>> ticketsByRecipient = new LinkedHashMap<>();
            for (TicketItem ticket : tickets) {
                String recipient = (ticket.getAttendeeEmail() != null && !ticket.getAttendeeEmail().isBlank())
                        ? ticket.getAttendeeEmail()
                        : customerEmail;

                if (recipient == null || recipient.isBlank()) {
                    log.warn("Tiket {} tanpa email peserta maupun akun, dilewati.", ticket.getTicketItemId());
                    continue;
                }

                ticketsByRecipient.computeIfAbsent(recipient.toLowerCase(), k -> new ArrayList<>())
                        .add(toTicketLine(ticket));
            }

            if (ticketsByRecipient.isEmpty()) {
                log.warn("Tidak ada penerima email yang valid untuk order {}.", order.getOrderId());
                return;
            }

            for (Map.Entry<String, List<TicketEmailData.TicketLine>> entry : ticketsByRecipient.entrySet()) {
                String recipient = resolveOriginalEmail(entry.getKey(), tickets, customerEmail);
                emailService.sendTicketEmail(TicketEmailData.builder()
                        .orderNumber(buildOrderNumber(order.getOrderId().toString()))
                        .eventTitle(buildEventTitle(order))
                        .eventDate(buildEventDate(order))
                        .venueName(buildVenueName(order))
                        .buyerName(buildBuyerName(order))
                        .recipientEmail(recipient)
                        .totalAmount(formatPrice(order.getTotalAmount()))
                        .tickets(entry.getValue())
                        .build());
            }

            log.info("E-ticket order {} diterbitkan ke {} penerima.", order.getOrderId(), ticketsByRecipient.size());
        } catch (Exception e) {
            // Kegagalan tiket/email tidak boleh merusak alur pembayaran.
            log.error("Gagal mengirim e-ticket untuk order {}: {}", eventOrder.getOrderId(), e.getMessage(), e);
        }
    }

    private String resolveOriginalEmail(String lowercaseKey, List<TicketItem> tickets, String customerEmail) {
        for (TicketItem ticket : tickets) {
            String email = ticket.getAttendeeEmail();
            if (email != null && email.toLowerCase().equals(lowercaseKey)) {
                return email;
            }
        }
        return customerEmail != null ? customerEmail : lowercaseKey;
    }

    private TicketEmailData.TicketLine toTicketLine(TicketItem ticket) {
        TicketTier tier = ticket.getTier();
        BigDecimal price = tier != null ? tier.getPrice() : null;

        return TicketEmailData.TicketLine.builder()
                .ticketCode(ticket.getTicketItemId() != null ? ticket.getTicketItemId().toString() : "-")
                .attendeeName(ticket.getAttendeeName())
                .tierName(tier != null ? tier.getTierName() : "-")
                .priceDisplay(formatPrice(price))
                .build();
    }

    private String buildOrderNumber(String orderId) {
        return "ORD-" + orderId.substring(0, Math.min(8, orderId.length())).toUpperCase();
    }

    private String buildEventTitle(Order order) {
        Event event = order.getEvent();
        return event != null && event.getTitle() != null ? event.getTitle() : "Eventday";
    }

    private String buildEventDate(Order order) {
        Event event = order.getEvent();
        return event != null && event.getStartDate() != null ? event.getStartDate().format(DATE_DISPLAY) : "-";
    }

    private String buildVenueName(Order order) {
        Event event = order.getEvent();
        return event != null && event.getVenueName() != null ? event.getVenueName() : "-";
    }

    private String buildBuyerName(Order order) {
        User customer = order.getCustomer();
        return customer != null && customer.getName() != null ? customer.getName() : "Pemesan";
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "Rp 0";
        }
        return "Rp " + String.format("%,d", price.longValue()).replace(",", ".");
    }
}
