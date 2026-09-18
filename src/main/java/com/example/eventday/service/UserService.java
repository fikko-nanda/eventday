package com.example.eventday.service;

import com.example.eventday.dto.ChangePasswordRequest;
import com.example.eventday.dto.TransactionHistoryResponse;
import com.example.eventday.dto.UpdateProfileRequest;
import com.example.eventday.dto.UserProfileResponse;
import com.example.eventday.entity.Auth;
import com.example.eventday.entity.Order;
import com.example.eventday.entity.User;
import com.example.eventday.repository.AuthRepository;
import com.example.eventday.repository.OrderRepository;
import com.example.eventday.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserService {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan!"));

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .username(user.getUsername())
                .phone(user.getPhone())
                .nik(user.getNik())
                .role(user.getRole() != null ? user.getRole().name() : "CUSTOMER")
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan!"));

        if (request.getNik() != null && !request.getNik().isBlank() && !request.getNik().equals(user.getNik())) {
            if (userRepository.existsByNik(request.getNik())) {
                throw new RuntimeException("NIK sudah digunakan akun lain!");
            }
            user.setNik(request.getNik());
        }

        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        auditLogService.log(userId, user.getName(), "UPDATE_PROFILE", "Update biodata user: " + user.getEmail());

        return getProfile(userId);
    }

    @Transactional
public void changePassword(UUID userId, ChangePasswordRequest request) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User tidak ditemukan!"));

    Auth auth = authRepository.findByUserUserId(userId)
            .orElseThrow(() -> new RuntimeException("Kredensial auth tidak ditemukan!"));

    if (!passwordEncoder.matches(request.getOldPassword(), auth.getPassword())) {
        throw new RuntimeException("Password lama salah!");
    }

    if (request.getOldPassword().equals(request.getNewPassword())) {
        throw new RuntimeException("Password baru tidak boleh sama dengan password lama!");
    }

    auth.setPassword(passwordEncoder.encode(request.getNewPassword()));
    auth.setUpdatedAt(LocalDateTime.now());
    authRepository.save(auth);

    auditLogService.log(userId, "USER", "CHANGE_PASSWORD", "Berhasil mengganti password mandiri");

    // Kirim notifikasi email ke user
    emailService.sendPasswordChangedNotification(user.getEmail());
}

    @Transactional(readOnly = true)
        public List<TransactionHistoryResponse> getTransactionHistory(UUID userId) {
            List<Order> orders = orderRepository.findByCustomerUserId(userId);

            return orders.stream().map(order -> TransactionHistoryResponse.builder()
                    .orderId(order.getOrderId())
                    .orderNumber("ORD-" + order.getOrderId().toString().substring(0, 8).toUpperCase())
                    .eventTitle(order.getEvent() != null ? order.getEvent().getTitle() : "-")
                    .ticketTierName(order.getTicketTier() != null ? order.getTicketTier().getTierName() : "-")
                    .quantity(order.getQuantity())
                    .totalAmount(order.getTotalAmount())
                    .status(order.getStatus() != null ? order.getStatus() : "PENDING") // Hapus .name() jika status di entity berupa String
                    .createdAt(order.getCreatedAt())
                    .expiredAt(order.getExpiredAt())
                    .build()
            ).collect(Collectors.toList());
        }
        
}