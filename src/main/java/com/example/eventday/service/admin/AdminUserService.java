package com.example.eventday.service.admin;

import com.example.eventday.dto.admin.AdminUserListItemResponse;
import com.example.eventday.entity.Auth;
import com.example.eventday.entity.User;
import com.example.eventday.repository.AuthRepository;
import com.example.eventday.repository.UserRepository;
import com.example.eventday.service.AuditLogService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AdminUserListItemResponse> getAllUsers(String roleFilter) {
        List<User> users;
        if (roleFilter != null && !roleFilter.isBlank() && !"ALL".equalsIgnoreCase(roleFilter)) {
            try {
                users = userRepository.findByRole(User.Role.valueOf(roleFilter.toUpperCase()));
            } catch (Exception e) {
                users = userRepository.findAll();
            }
        } else {
            users = userRepository.findAll();
        }

        return users.stream().map(this::mapToUserListItem).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminUserListItemResponse getUserDetail(UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Pengguna tidak ditemukan!"));
        return mapToUserListItem(u);
    }

    @Transactional
    public void updateUserStatus(UUID targetUserId, String newStatus, UUID adminId) {
        Auth auth = authRepository.findByUserUserId(targetUserId)
                .orElseThrow(() -> new RuntimeException("Kredensial auth pengguna tidak ditemukan!"));

        auth.setStatus(newStatus.toUpperCase());
        if ("SUSPENDED".equalsIgnoreCase(newStatus) || "INACTIVE".equalsIgnoreCase(newStatus)) {
            auth.setAksesToken(null);
        }
        auth.setUpdatedAt(LocalDateTime.now());
        authRepository.save(auth);

        auditLogService.log(adminId, "SUPERADMIN", "UPDATE_USER_STATUS",
                "Mengubah status akun " + targetUserId + " menjadi " + newStatus);
    }

    @Transactional
    public void suspendUser(UUID targetUserId, UUID adminId) {
        updateUserStatus(targetUserId, "SUSPENDED", adminId);
    }

    private AdminUserListItemResponse mapToUserListItem(User u) {
        String status = authRepository.findByUserUserId(u.getUserId())
                .map(Auth::getStatus)
                .orElse("INACTIVE");

        return AdminUserListItemResponse.builder()
                .userId(u.getUserId())
                .name(u.getName())
                .email(u.getEmail())
                .username(u.getUsername())
                .phone(u.getPhone())
                .nik(u.getNik())
                .role(u.getRole() != null ? u.getRole().name() : "CUSTOMER")
                .authStatus(status)
                .createdAt(u.getCreatedAt())
                .build();
    }
}