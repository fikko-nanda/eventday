package com.example.eventday.service.organizer;

import com.example.eventday.entity.Organizer;
import com.example.eventday.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class OrganizerHelperService {

    private final OrganizerRepository organizerRepository;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

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

    public String saveFile(MultipartFile file, String subfolder) {
        return saveFile(file, subfolder, false);
    }

    public String saveFile(MultipartFile file, String subfolder, boolean allowPdf) {
        try {
            Path dir = Paths.get(uploadDir, subfolder);
            Files.createDirectories(dir);
            String original = Objects.requireNonNull(file.getOriginalFilename());
            String filename = UUID.randomUUID() + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/" + dir.toString().replace("\\", "/") + "/" + filename;
        } catch (IOException e) {
            log.warn("Gagal simpan file {}: {}", file.getOriginalFilename(), e.getMessage());
            return "/uploads/" + subfolder + "/" + file.getOriginalFilename();
        }
    }
}