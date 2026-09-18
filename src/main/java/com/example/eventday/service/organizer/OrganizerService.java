package com.example.eventday.service.organizer;

import com.example.eventday.entity.Organizer;
import com.example.eventday.entity.User;
import com.example.eventday.repository.AuthRepository;
import com.example.eventday.repository.OrganizerRepository;
import com.example.eventday.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerService {

    private final OrganizerHelperService helperService;
    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Map<String, Object> registerOrganizer(Map<String, Object> request) {
        UUID uid = helperService.currentUserId();
        if (uid == null) {
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
        String url = helperService.saveFile(file, "organizer-docs");
        Map<String, Object> data = new HashMap<>();
        data.put("document_type", documentType);
        data.put("file_name", file.getOriginalFilename());
        data.put("file_size", file.getSize());
        data.put("document_url", url);
        data.put("uploaded_at", LocalDateTime.now().toString());
        return data;
    }

    public Map<String, Object> getOrganizerStatus() {
        UUID uid = helperService.currentUserId();
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
        Map<String, Object> data = new HashMap<>();
        data.put("organizer_name", "PT Penyelenggara Event");
        data.put("verification_status", "PENDING");
        data.put("mock", true);
        return data;
    }

    public void changePassword(Map<String, Object> payload) {
        UUID uid = helperService.currentUserId();
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
        log.info("Organizer changePassword mock: {}", payload);
    }

    public void logout() {
        UUID uid = helperService.currentUserId();
        log.info("Organizer logout: {}", uid);
    }
}