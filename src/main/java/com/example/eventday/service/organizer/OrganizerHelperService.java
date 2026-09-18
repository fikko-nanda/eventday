package com.example.eventday.service.organizer;

import com.example.eventday.entity.Organizer;
import com.example.eventday.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
public class OrganizerHelperService {

    private final OrganizerRepository organizerRepository;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public UUID currentUserId() {
        try {
            String uid = SecurityContextHolder.getContext().getAuthentication().getName();
            return UUID.fromString(uid);
        } catch (Exception e) {
            return null;
        }
    }

    public Organizer resolveCurrentOrganizer() {
        UUID uid = currentUserId();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autentikasi diperlukan");
        }
        return organizerRepository.findByUserUserId(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Akun tidak terdaftar sebagai Organizer"));
    }

    public String saveFile(MultipartFile file, String subfolder) {
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
}