package com.example.eventday.service.organizer;

import com.example.eventday.entity.Organizer;
import com.example.eventday.repository.OrganizerRepository;
import com.example.eventday.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class OrganizerHelperService {

    private final OrganizerRepository organizerRepository;
    private final FileStorageService fileStorageService;

    public UUID currentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return null;
            String uid = auth.getName();
            if (uid == null || uid.isBlank()) return null;
            return UUID.fromString(uid);
        } catch (Exception e) {
            return null;
        }
    }

    public Organizer resolveCurrentOrganizer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autentikasi diperlukan");
        }
        String uid = auth.getName();
        if (uid == null || uid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token tidak valid");
        }
        try {
            UUID userId = UUID.fromString(uid);
            return organizerRepository.findByUserUserId(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Akun tidak terdaftar sebagai Organizer"));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Format token tidak valid");
        }
    }

    /**
     * Resolusi organizer NULL-SAFE untuk endpoint dashboard/read-only.
     * Return null (TANPA throw) bila: auth hilang, token bukan UUID,
     * atau baris organizer belum ada di DB (misal user baru daftar).
     * Dashboard service memakai ini agar endpoint selalu 200 OK
     * (fallback metrik 0 / list kosong) alih-alih 400/403.
     */
    public Organizer findCurrentOrganizer() {
        try {
            UUID userId = currentUserId();
            if (userId == null) {
                return null;
            }
            return organizerRepository.findByUserUserId(userId).orElse(null);
        } catch (Exception e) {
            log.debug("Organizer tidak ditemukan untuk user saat ini: {}", e.getMessage());
            return null;
        }
    }

    public String saveFile(MultipartFile file, String subfolder) {
        return saveFile(file, subfolder, true);
    }

    public String saveFile(MultipartFile file, String subfolder, boolean allowPdf) {
        if (file == null || file.isEmpty()) {
            log.warn("File kosong untuk subfolder {}", subfolder);
            return null;
        }
        try {
            // Validasi + simpan (PDF diizinkan bila allowPdf) — direktori dibuat otomatis
            return fileStorageService.saveFile(file, subfolder, allowPdf);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            // Validasi file gagal (format/ukuran) -> 400 terstruktur, bukan 500
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage() != null ? e.getMessage() : "File tidak valid");
        }
    }
}