package com.example.eventday.service;

import com.example.eventday.entity.Event;
import com.example.eventday.entity.Organizer;
import com.example.eventday.entity.User;
import com.example.eventday.repository.AuthRepository;
import com.example.eventday.repository.EventRepository;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.OrganizerRepository;
import com.example.eventday.repository.RefundRepository;
import com.example.eventday.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerService {

    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final RefundRepository refundRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    private UUID currentUserId() {
        try {
            String uid = SecurityContextHolder.getContext().getAuthentication().getName();
            return UUID.fromString(uid);
        } catch (Exception e) {
            return null;
        }
    }

    private String saveFile(MultipartFile file, String subfolder) {
        try {
            Path dir = Paths.get(uploadDir, subfolder);
            Files.createDirectories(dir);
            String filename = UUID.randomUUID() + "_" + Objects.requireNonNull(file.getOriginalFilename()).replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/" + dir.toString().replace("\\", "/") + "/" + filename;
        } catch (IOException e) {
            log.warn("Gagal simpan file {}: {}", file.getOriginalFilename(), e.getMessage());
            return "/uploads/" + subfolder + "/" + file.getOriginalFilename();
        }
    }

    // ==========================================
    // SPEC V1.3.0 (MODUL 07) — now DB-backed
    // ==========================================

    @Transactional
    public Map<String, Object> registerOrganizer(Map<String, Object> request) {
        UUID uid = currentUserId();
        if (uid == null) {
            // fallback mock for unauthenticated dev call
            Map<String, Object> data = new HashMap<>();
            data.put("organizer_name", request.getOrDefault("name", "PT Penyelenggara Event"));
            data.put("verification_status", "PENDING");
            data.put("mock", true);
            data.put("note", "Login dulu untuk register organizer real");
            return data;
        }
        Optional<Organizer> existing = organizerRepository.findByUserUserId(uid);
        if (existing.isPresent()) {
            Organizer org = existing.get();
            Map<String, Object> data = new HashMap<>();
            data.put("organizer_id", org.getOrganizerId().toString());
            data.put("organizer_name", org.getNameOrganizer());
            data.put("verification_status", org.getVerificationStatus());
            data.put("message", "Organizer sudah terdaftar");
            return data;
        }
        User user = userRepository.findById(uid).orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        String name = (String) request.getOrDefault("name", request.getOrDefault("organizer_name", user.getName()));
        Organizer org = Organizer.builder()
                .user(user)
                .nameOrganizer(name)
                .npwpNumber((String) request.get("npwp_number"))
                .bankName((String) request.get("bank_name"))
                .bankAccountNumber((String) request.get("bank_account_number"))
                .verificationStatus("PENDING")
                .build();
        organizerRepository.save(org);
        Map<String, Object> data = new HashMap<>();
        data.put("organizer_id", org.getOrganizerId().toString());
        data.put("organizer_name", org.getNameOrganizer());
        data.put("verification_status", org.getVerificationStatus());
        data.put("user_id", uid.toString());
        return data;
    }

    public Map<String, Object> uploadDocument(MultipartFile file, String documentType) {
        String url = saveFile(file, "organizer-docs");
        Map<String, Object> data = new HashMap<>();
        data.put("document_type", documentType);
        data.put("file_name", file.getOriginalFilename());
        data.put("file_size", file.getSize());
        data.put("document_url", url);
        data.put("uploaded_at", LocalDateTime.now().toString());
        return data;
    }

    public Map<String, Object> getOrganizerStatus() {
        UUID uid = currentUserId();
        if (uid != null) {
            Optional<Organizer> opt = organizerRepository.findByUserUserId(uid);
            if (opt.isPresent()) {
                Organizer o = opt.get();
                Map<String, Object> data = new HashMap<>();
                data.put("organizer_id", o.getOrganizerId().toString());
                data.put("organizer_name", o.getNameOrganizer());
                data.put("verification_status", o.getVerificationStatus());
                data.put("created_at", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
                return data;
            }
        }
        // fallback mock — usable untuk frontend tanpa DB row
        Map<String, Object> data = new HashMap<>();
        data.put("organizer_name", "PT Penyelenggara Event");
        data.put("verification_status", "PENDING");
        data.put("mock", true);
        return data;
    }

    public Map<String, Object> getOrganizerDashboard() {
        UUID uid = currentUserId();
        if (uid != null) {
            Optional<Organizer> opt = organizerRepository.findByUserUserId(uid);
            if (opt.isPresent()) {
                Organizer org = opt.get();
                // hitung event milik organizer
                List<Event> allEvents = eventRepository.findAll();
                List<Event> myEvents = allEvents.stream()
                        .filter(e -> e.getOrganizer() != null && e.getOrganizer().getOrganizerId().equals(org.getOrganizerId()))
                        .collect(Collectors.toList());
                long activeEvents = myEvents.stream().filter(e -> "PUBLISHED".equals(e.getStatus())).count();
                // hitung revenue dari orders yang event-nya milik organizer
                double revenue = orderRepository.findAll().stream()
                        .filter(o -> o.getEvent() != null && o.getEvent().getOrganizer() != null
                                && o.getEvent().getOrganizer().getOrganizerId().equals(org.getOrganizerId()))
                        .filter(o -> "PAID".equals(o.getStatus()) || "WAITING_PAYMENT".equals(o.getStatus()))
                        .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0)
                        .sum();
                long ticketsSold = orderRepository.findAll().stream()
                        .filter(o -> o.getEvent() != null && o.getEvent().getOrganizer() != null
                                && o.getEvent().getOrganizer().getOrganizerId().equals(org.getOrganizerId()))
                        .mapToLong(o -> o.getQuantity() != null ? o.getQuantity() : 0)
                        .sum();
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("total_revenue", revenue);
                metrics.put("active_events", activeEvents);
                metrics.put("total_events", myEvents.size());
                metrics.put("tickets_sold", ticketsSold);
                metrics.put("organizer_id", org.getOrganizerId().toString());
                metrics.put("real", true);
                return metrics;
            }
        }
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_revenue", 42500000);
        metrics.put("active_events", 2);
        metrics.put("tickets_sold", 1248);
        metrics.put("mock", true);
        return metrics;
    }

    // ==========================================
    // PROFIL & LEGALITAS EO — DB aware
    // ==========================================

    public Map<String, Object> getProfile() {
        UUID uid = currentUserId();
        if (uid != null) {
            Optional<Organizer> opt = organizerRepository.findByUserUserId(uid);
            if (opt.isPresent()) {
                Organizer o = opt.get();
                User u = o.getUser();
                Map<String, Object> profile = new HashMap<>();
                profile.put("organizer_id", o.getOrganizerId().toString());
                profile.put("user_id", u.getUserId().toString());
                profile.put("name", o.getNameOrganizer());
                profile.put("pic_name", u.getName());
                profile.put("email", u.getEmail());
                profile.put("phone", u.getPhone());
                profile.put("npwp", o.getNpwpNumber());
                profile.put("bank_name", o.getBankName());
                profile.put("bank_account_number", o.getBankAccountNumber());
                profile.put("verification_status", o.getVerificationStatus());
                profile.put("avatar_url", "https://api.dicebear.com/7.x/identicon/svg?seed=" + o.getOrganizerId());
                profile.put("created_at", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
                return profile;
            }
            // user ada tapi belum jadi organizer — return user profile
            User u = userRepository.findById(uid).orElse(null);
            if (u != null) {
                Map<String, Object> profile = new HashMap<>();
                profile.put("name", u.getName());
                profile.put("pic_name", u.getName());
                profile.put("email", u.getEmail());
                profile.put("phone", u.getPhone());
                profile.put("verification_status", "UNREGISTERED");
                profile.put("note", "Belum terdaftar sebagai organizer — POST /organizer/register dulu");
                return profile;
            }
        }
        Map<String, Object> profile = new HashMap<>();
        profile.put("name", "PT Harmoni Musik Indonesia");
        profile.put("pic_name", "Budi Santoso");
        profile.put("email", "budi.santoso@harmoni.co.id");
        profile.put("phone", "+6281234567890");
        profile.put("npwp", "01.234.567.8-901.000");
        profile.put("avatar_url", "https://api.dicebear.com/7.x/identicon/svg?seed=harmoni");
        profile.put("mock", true);
        return profile;
    }

    @Transactional
    public Map<String, Object> updateProfile(Map<String, Object> payload) {
        UUID uid = currentUserId();
        if (uid != null) {
            Optional<Organizer> opt = organizerRepository.findByUserUserId(uid);
            if (opt.isPresent()) {
                Organizer o = opt.get();
                if (payload.containsKey("name")) o.setNameOrganizer((String) payload.get("name"));
                if (payload.containsKey("organizer_name")) o.setNameOrganizer((String) payload.get("organizer_name"));
                if (payload.containsKey("npwp")) o.setNpwpNumber((String) payload.get("npwp"));
                if (payload.containsKey("npwp_number")) o.setNpwpNumber((String) payload.get("npwp_number"));
                if (payload.containsKey("bank_name")) o.setBankName((String) payload.get("bank_name"));
                if (payload.containsKey("bank_account_number")) o.setBankAccountNumber((String) payload.get("bank_account_number"));
                o.setUpdatedAt(LocalDateTime.now());
                organizerRepository.save(o);
                // also update user name/phone if provided
                User u = o.getUser();
                if (payload.containsKey("pic_name")) u.setName((String) payload.get("pic_name"));
                if (payload.containsKey("phone")) u.setPhone((String) payload.get("phone"));
                userRepository.save(u);
                payload.put("organizer_id", o.getOrganizerId().toString());
                payload.put("updated_at", o.getUpdatedAt().toString());
                return payload;
            }
        }
        return payload;
    }

    public Map<String, Object> uploadAvatar(MultipartFile file) {
        String url = saveFile(file, "avatars");
        Map<String, Object> data = new HashMap<>();
        data.put("avatar_url", url);
        data.put("file_name", file.getOriginalFilename());
        data.put("file_size", file.getSize());
        return data;
    }

    public Map<String, Object> uploadPortfolio(MultipartFile file) {
        String url = saveFile(file, "organizer-docs/portfolio");
        Map<String, Object> data = new HashMap<>();
        data.put("portfolio_url", url);
        data.put("file_name", file.getOriginalFilename());
        data.put("file_size", file.getSize());
        return data;
    }

    public Map<String, Object> uploadDeed(MultipartFile file) {
        String url = saveFile(file, "organizer-docs/deeds");
        Map<String, Object> data = new HashMap<>();
        data.put("company_deed_url", url);
        data.put("file_name", file.getOriginalFilename());
        data.put("file_size", file.getSize());
        return data;
    }

    public Map<String, Object> getProfileDocuments() {
        UUID uid = currentUserId();
        if (uid != null) {
            Optional<Organizer> opt = organizerRepository.findByUserUserId(uid);
            if (opt.isPresent()) {
                Organizer o = opt.get();
                Map<String, Object> docs = new HashMap<>();
                docs.put("organizer_id", o.getOrganizerId().toString());
                docs.put("akta_perusahaan", o.getAktaPerusahaan());
                docs.put("has_akta", o.getAktaPerusahaan() != null);
                docs.put("portfolio_name", "portfolio_" + o.getOrganizerId() + ".pdf");
                docs.put("deed_name", o.getAktaPerusahaan() != null ? o.getAktaPerusahaan() : "Akta_Perusahaan.pdf");
                docs.put("ktp_name", "KTP_PIC.jpg");
                return docs;
            }
        }
        Map<String, Object> docs = new HashMap<>();
        docs.put("portfolio_name", "CV_Portofolio.pdf");
        docs.put("deed_name", "Akta_Perusahaan.pdf");
        docs.put("ktp_name", "KTP_PIC.jpg");
        docs.put("mock", true);
        return docs;
    }

    // ==========================================
    // AUTENTIKASI EO
    // ==========================================

    public void changePassword(Map<String, Object> payload) {
        UUID uid = currentUserId();
        if (uid != null) {
            String oldPass = (String) payload.get("oldPassword");
            String np = (String) payload.get("newPassword");
            if (np == null) np = (String) payload.get("new_password");
            final String newPass = np;
            if (oldPass != null && newPass != null) {
                final String oldPassFinal = oldPass;
                authRepository.findByUserUserId(uid).ifPresent(auth -> {
                    if (passwordEncoder.matches(oldPassFinal, auth.getPassword())) {
                        auth.setPassword(passwordEncoder.encode(newPass));
                        authRepository.save(auth);
                    } else {
                        throw new RuntimeException("Password lama salah!");
                    }
                });
                return;
            }
        }
        // fallback no-op for mock
        log.info("Organizer changePassword mock: {}", payload);
    }

    public void logout() {
        // Real logout delegates to AuthService via controller; here just log
        UUID uid = currentUserId();
        log.info("Organizer logout: {}", uid);
    }

    // ==========================================
    // MANAJEMEN REFUND EO — DB-backed with fallback
    // ==========================================

    public List<Map<String, Object>> getRefundRequests() {
        UUID uid = currentUserId();
        if (uid != null) {
            Optional<Organizer> orgOpt = organizerRepository.findByUserUserId(uid);
            if (orgOpt.isPresent()) {
                List<com.example.eventday.entity.RefundRequestEntity> list =
                        refundRepository.findByOrganizerId(orgOpt.get().getOrganizerId());
                if (!list.isEmpty()) {
                    return list.stream().map(r -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("refund_id", r.getRefundId().toString());
                        m.put("order_id", r.getOrderId() != null ? r.getOrderId().toString() : null);
                        m.put("customer_id", r.getCustomerId() != null ? r.getCustomerId().toString() : null);
                        m.put("amount", r.getAmount());
                        m.put("reason", r.getReason());
                        m.put("bank_name", r.getBankName());
                        m.put("account_number", r.getBankAccountNumber());
                        m.put("account_holder", r.getAccountHolder());
                        m.put("status", r.getStatus());
                        m.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
                        return m;
                    }).collect(Collectors.toList());
                }
            }
            // global fallback: show all pending refunds (for dev)
            List<com.example.eventday.entity.RefundRequestEntity> all = refundRepository.findByStatus("PENDING");
            if (!all.isEmpty()) {
                return all.stream().limit(20).map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("refund_id", r.getRefundId().toString());
                    m.put("order_id", r.getOrderId() != null ? r.getOrderId().toString() : null);
                    m.put("amount", r.getAmount());
                    m.put("reason", r.getReason());
                    m.put("status", r.getStatus());
                    return m;
                }).collect(Collectors.toList());
            }
        }
        Map<String, Object> item = new HashMap<>();
        item.put("refund_id", "REF-001");
        item.put("order_id", "ORD-12345");
        item.put("customer_name", "Farid Ainur");
        item.put("event_title", "Konser Musik Indie Fest");
        item.put("amount", 250000);
        item.put("reason", "Event dijadwalkan ulang");
        item.put("status", "PENDING");
        item.put("mock", true);
        return List.of(item);
    }

    public Map<String, Object> getRefundDetail(String refundId) {
        try {
            UUID id = UUID.fromString(refundId);
            Optional<com.example.eventday.entity.RefundRequestEntity> opt = refundRepository.findById(id);
            if (opt.isPresent()) {
                var r = opt.get();
                Map<String, Object> detail = new HashMap<>();
                detail.put("refund_id", r.getRefundId().toString());
                detail.put("order_id", r.getOrderId() != null ? r.getOrderId().toString() : null);
                detail.put("amount", r.getAmount());
                detail.put("reason", r.getReason());
                detail.put("bank_name", r.getBankName());
                detail.put("account_number", r.getBankAccountNumber());
                detail.put("account_holder", r.getAccountHolder());
                detail.put("status", r.getStatus());
                detail.put("rejection_reason", r.getRejectionReason());
                detail.put("admin_note", r.getAdminNote());
                detail.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
                return detail;
            }
        } catch (Exception ignored) {}
        Map<String, Object> detail = new HashMap<>();
        detail.put("refund_id", refundId);
        detail.put("order_id", "ORD-12345");
        detail.put("customer_name", "Farid Ainur");
        detail.put("account_number", "9876543210");
        detail.put("bank_name", "BCA");
        detail.put("amount", 250000);
        detail.put("reason", "Event dijadwalkan ulang");
        detail.put("status", "PENDING");
        detail.put("submitted_at", "2026-09-14 10:00:00");
        detail.put("mock", true);
        return detail;
    }

    @Transactional
    public Map<String, Object> updateRefundStatus(String refundId, Map<String, Object> payload) {
        try {
            UUID id = UUID.fromString(refundId);
            Optional<com.example.eventday.entity.RefundRequestEntity> opt = refundRepository.findById(id);
            if (opt.isPresent()) {
                var r = opt.get();
                String newStatus = (String) payload.getOrDefault("status", payload.getOrDefault("newStatus", "APPROVED"));
                r.setStatus(newStatus.toUpperCase());
                if (payload.containsKey("adminNote")) r.setAdminNote((String) payload.get("adminNote"));
                if (payload.containsKey("admin_note")) r.setAdminNote((String) payload.get("admin_note"));
                if (payload.containsKey("rejection_reason")) r.setRejectionReason((String) payload.get("rejection_reason"));
                if ("APPROVED".equalsIgnoreCase(newStatus) || "REJECTED".equalsIgnoreCase(newStatus)) {
                    r.setProcessedAt(LocalDateTime.now());
                }
                r.setUpdatedAt(LocalDateTime.now());
                refundRepository.save(r);
                Map<String, Object> res = new HashMap<>();
                res.put("refund_id", r.getRefundId().toString());
                res.put("status", r.getStatus());
                res.put("admin_note", r.getAdminNote());
                res.put("processed_at", r.getProcessedAt() != null ? r.getProcessedAt().toString() : null);
                return res;
            }
        } catch (Exception e) {
            log.warn("updateRefundStatus failed {}: {}", refundId, e.getMessage());
        }
        Map<String, Object> res = new HashMap<>();
        res.put("refund_id", refundId);
        res.put("status", payload.getOrDefault("status", "APPROVED"));
        res.put("notes", payload.getOrDefault("notes", "Disetujui oleh EO"));
        res.put("mock", true);
        return res;
    }

    // ==========================================
    // PAYOUT & SALDO EO — usable for frontend EO dashboard
    // ==========================================

    public List<Map<String, Object>> getBankAccounts() {
        UUID uid = currentUserId();
        if (uid != null) {
            Optional<Organizer> opt = organizerRepository.findByUserUserId(uid);
            if (opt.isPresent() && opt.get().getBankName() != null) {
                Organizer o = opt.get();
                Map<String, Object> bank = new HashMap<>();
                bank.put("id", o.getOrganizerId().toString());
                bank.put("bank_name", o.getBankName());
                bank.put("account_number", o.getBankAccountNumber());
                bank.put("account_holder", o.getNameOrganizer());
                bank.put("is_primary", true);
                return List.of(bank);
            }
        }
        Map<String, Object> bank = new HashMap<>();
        bank.put("id", 1);
        bank.put("bank_name", "BCA");
        bank.put("account_number", "8830123456");
        bank.put("account_holder", "PT Harmoni Musik Indonesia");
        bank.put("is_primary", true);
        bank.put("mock", true);
        return List.of(bank);
    }

    public Map<String, Object> getPayoutBalance(Long eventId) {
        // Try compute real if organizer context exists
        UUID uid = currentUserId();
        if (uid != null) {
            Optional<Organizer> orgOpt = organizerRepository.findByUserUserId(uid);
            if (orgOpt.isPresent()) {
                // use eventId param if provided else aggregate
                double totalSales = orderRepository.findAll().stream()
                        .filter(o -> o.getEvent() != null && o.getEvent().getEventId().toString().equals(String.valueOf(eventId))
                                || eventId == null)
                        .filter(o -> o.getEvent() != null && o.getEvent().getOrganizer().getOrganizerId().equals(orgOpt.get().getOrganizerId()))
                        .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount().doubleValue() : 0)
                        .sum();
                Map<String, Object> balance = new HashMap<>();
                balance.put("event_id", eventId);
                balance.put("organizer_id", orgOpt.get().getOrganizerId().toString());
                balance.put("total_sales", totalSales);
                balance.put("withdrawable_balance", totalSales * 0.9);
                balance.put("pending_payout", totalSales * 0.1);
                balance.put("currency", "IDR");
                return balance;
            }
        }
        Map<String, Object> balance = new HashMap<>();
        balance.put("event_id", eventId);
        balance.put("total_sales", 50000000);
        balance.put("withdrawable_balance", 45000000);
        balance.put("pending_payout", 5000000);
        balance.put("currency", "IDR");
        balance.put("mock", true);
        return balance;
    }

    public List<Map<String, Object>> getPayouts() {
        UUID uid = currentUserId();
        if (uid != null) {
            Optional<Organizer> orgOpt = organizerRepository.findByUserUserId(uid);
            if (orgOpt.isPresent()) {
                List<com.example.eventday.entity.RefundRequestEntity> list =
                        refundRepository.findByOrganizerId(orgOpt.get().getOrganizerId());
                if (!list.isEmpty()) {
                    return list.stream().map(r -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", r.getRefundId().toString());
                        m.put("payout_id", r.getRefundId().toString());
                        m.put("amount", r.getAmount());
                        m.put("status", r.getStatus());
                        m.put("bank_name", r.getBankName());
                        m.put("requested_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
                        m.put("processed_at", r.getProcessedAt() != null ? r.getProcessedAt().toString() : null);
                        return m;
                    }).collect(Collectors.toList());
                }
            }
        }
        Map<String, Object> payout = new HashMap<>();
        payout.put("id", 101);
        payout.put("amount", 25000000);
        payout.put("status", "SUCCESS");
        payout.put("requested_at", "2026-09-10");
        payout.put("mock", true);
        return List.of(payout);
    }

    @Transactional
    public Map<String, Object> createPayout(Map<String, Object> request) {
        UUID uid = currentUserId();
        UUID organizerId = null;
        if (uid != null) {
            Optional<Organizer> opt = organizerRepository.findByUserUserId(uid);
            if (opt.isPresent()) organizerId = opt.get().getOrganizerId();
        }
        if (organizerId != null && request.containsKey("amount")) {
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(String.valueOf(request.get("amount")));
                com.example.eventday.entity.RefundRequestEntity entity = com.example.eventday.entity.RefundRequestEntity.builder()
                        .organizerId(organizerId)
                        .customerId(uid)
                        .amount(amount)
                        .reason("Payout EO: " + request.getOrDefault("description", "Pencairan dana"))
                        .bankName((String) request.getOrDefault("bank_name", "BCA"))
                        .bankAccountNumber((String) request.getOrDefault("account_number", ""))
                        .accountHolder((String) request.getOrDefault("account_holder", ""))
                        .status("PENDING")
                        .build();
                refundRepository.save(entity);
                Map<String, Object> payout = new HashMap<>();
                payout.put("payout_id", entity.getRefundId().toString());
                payout.put("amount", amount);
                payout.put("status", "PENDING_APPROVAL");
                payout.put("organizer_id", organizerId.toString());
                return payout;
            } catch (Exception e) {
                log.warn("createPayout DB fail: {}", e.getMessage());
            }
        }
        Map<String, Object> payout = new HashMap<>();
        payout.put("payout_id", 102);
        payout.put("amount", request.getOrDefault("amount", 10000000));
        payout.put("status", "PENDING_APPROVAL");
        payout.put("mock", true);
        return payout;
    }

    public Map<String, Object> getPayoutDetail(Long id) {
        // id here is actually not UUID; try to parse as string fallback
        try {
            UUID uid = UUID.fromString(String.valueOf(id));
            Optional<com.example.eventday.entity.RefundRequestEntity> opt = refundRepository.findById(uid);
            if (opt.isPresent()) {
                var r = opt.get();
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", r.getRefundId().toString());
                detail.put("amount", r.getAmount());
                detail.put("status", r.getStatus());
                detail.put("bank_name", r.getBankName());
                detail.put("account_number", r.getBankAccountNumber());
                detail.put("account_holder", r.getAccountHolder());
                detail.put("reconciliation_document_url", r.getReconciliationDocumentUrl());
                detail.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
                return detail;
            }
        } catch (Exception ignored) {}
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", id);
        detail.put("amount", 25000000);
        detail.put("status", "SUCCESS");
        detail.put("bank_name", "BCA");
        detail.put("account_number", "8830123456");
        detail.put("transfer_proof_url", "https://storage.eventday.id/proofs/transfer-101.png");
        detail.put("mock", true);
        return detail;
    }
}
