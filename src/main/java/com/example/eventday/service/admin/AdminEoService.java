package com.example.eventday.service.admin;

import com.example.eventday.dto.admin.AdminEoApplicationResponse;
import com.example.eventday.entity.Organizer;
import com.example.eventday.entity.User;
import com.example.eventday.repository.OrganizerRepository;
import com.example.eventday.repository.UserRepository;
import com.example.eventday.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminEoService {

    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<AdminEoApplicationResponse> getApplications(String statusFilter) {
        List<Organizer> organizers;
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            organizers = organizerRepository.findByVerificationStatus(statusFilter.toUpperCase());
        } else {
            organizers = organizerRepository.findAllByOrderByCreatedAtDesc();
        }

        return organizers.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminEoApplicationResponse getApplicationDetail(UUID organizerId) {
        Organizer o = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Aplikasi EO tidak ditemukan!"));
        return mapToResponse(o);
    }

    @Transactional
    public void updateApplicationStatus(UUID organizerId, String newStatus, String reason, UUID adminId) {
        Organizer o = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Aplikasi EO tidak ditemukan!"));

        String statusUpper = newStatus.toUpperCase();
        o.setVerificationStatus(statusUpper);
        organizerRepository.save(o);

        // Jika disetujui (VERIFIED), update role akun user menjadi ORGANIZER
        if ("VERIFIED".equalsIgnoreCase(statusUpper) && o.getUser() != null) {
            User user = o.getUser();
            user.setRole(User.Role.ORGANIZER);
            userRepository.save(user);
        }

        auditLogService.log(adminId, "SUPERADMIN", "VERIFY_EO", 
                "Aplikasi EO " + o.getNameOrganizer() + " diubah menjadi: " + statusUpper + 
                (reason != null ? " (Alasan: " + reason + ")" : ""));
    }

    @Transactional(readOnly = true)
    public String getCompanyDeedDocument(UUID organizerId) {
        Organizer o = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Aplikasi EO tidak ditemukan!"));
        return o.getAktaPerusahaan() != null ? o.getAktaPerusahaan() : "";
    }

    private AdminEoApplicationResponse mapToResponse(Organizer o) {
        User u = o.getUser();
        return AdminEoApplicationResponse.builder()
                .organizerId(o.getOrganizerId())
                .userId(u != null ? u.getUserId() : null)
                .nameOrganizer(o.getNameOrganizer())
                .userEmail(u != null ? u.getEmail() : null)
                .userPhone(u != null ? u.getPhone() : null)
                .npwpNumber(o.getNpwpNumber())
                .bankName(o.getBankName())
                .bankAccountNumber(o.getBankAccountNumber())
                .aktaPerusahaan(o.getAktaPerusahaan())
                .verificationStatus(o.getVerificationStatus())
                .createdAt(o.getCreatedAt())
                .build();
    }
}