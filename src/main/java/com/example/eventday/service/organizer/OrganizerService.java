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
    public Map<String, Object> registerOrganizer(Map<String, Object> request, 
            MultipartFile cvFile, MultipartFile portfolioFile, 
            MultipartFile aktaFile, MultipartFile aktaPerusahaanFile) {
        UUID uid = helperService.currentUserId();
        if (uid == null) {
            // Pendaftaran EO wajib login dulu (Customer/buyer) — tanpa login tidak bisa
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Daftar EO harus login terlebih dahulu");
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
        User user = userRepository.findById(uid).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Akun tidak ditemukan — silakan login terlebih dahulu"));
        String name = getStr(request, "name", "organizer_name", "nama", "namaEo");
        if (name == null || name.isBlank()) {
            name = user.getName();
        }

        // Handle file uploads (null-safe: file boleh tidak dikirim, invalid -> 400)
        Map<String, String> documentUrls = new HashMap<>();
        String cvUrl = null;
        String portfolioUrl = null;
        String aktaUrl = null;
        if (cvFile != null && !cvFile.isEmpty()) {
            cvUrl = helperService.saveFile(cvFile, "organizer-docs", true);
            if (cvUrl != null) documentUrls.put("cv_url", cvUrl);
        }
        if (portfolioFile != null && !portfolioFile.isEmpty()) {
            portfolioUrl = helperService.saveFile(portfolioFile, "organizer-docs", true);
            if (portfolioUrl != null) documentUrls.put("portfolio_url", portfolioUrl);
        }
        if (aktaFile != null && !aktaFile.isEmpty()) {
            String url = helperService.saveFile(aktaFile, "organizer-docs", true);
            if (url != null) documentUrls.put("akta_url", url);
        }
        if (aktaPerusahaanFile != null && !aktaPerusahaanFile.isEmpty()) {
            aktaUrl = helperService.saveFile(aktaPerusahaanFile, "organizer-docs", true);
            if (aktaUrl != null) documentUrls.put("akta_perusahaan_url", aktaUrl);
        }
        
        Organizer org = Organizer.builder()
                .user(user)
                .nameOrganizer(name)
                .npwpNumber(getStr(request, "npwp_number", "npwp", "npwpNumber"))
                .bankName(getStr(request, "bank_name", "bankName"))
                .bankAccountNumber(getStr(request, "bank_account_number", "account_number", "bankAccountNumber"))
                .cvUrl(cvUrl)
                .portfolioUrl(portfolioUrl)
                .aktaPerusahaan(aktaUrl)
                .verificationStatus("UNVERIFIED")
                .build();
        organizerRepository.save(org);
        
        Map<String, Object> data = new HashMap<>();
        data.put("organizer_id", org.getOrganizerId().toString());
        data.put("organizer_name", org.getNameOrganizer());
        data.put("verification_status", org.getVerificationStatus());
        data.put("user_id", uid.toString());
        data.putAll(documentUrls);
        return data;
    }

    // Baca nilai String dari map tanpa ClassCastException (value JSON bisa non-String)
    private static String getStr(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null) {
                String s = String.valueOf(v).trim();
                if (!s.isEmpty()) return s;
            }
        }
        return null;
    }

    public Map<String, Object> uploadDocument(MultipartFile file, String documentType) {
        if (file == null || file.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "File tidak boleh kosong");
        }
        String url = helperService.saveFile(file, "organizer-docs", true);
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