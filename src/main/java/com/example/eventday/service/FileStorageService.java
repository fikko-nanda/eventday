package com.example.eventday.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("PNG", "JPG", "JPEG", "WEBP", "GIF");

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    public String saveImage(MultipartFile file, String subfolder) {
        validateImage(file);
        try {
            Path dir = Paths.get(uploadDir, subfolder);
            Files.createDirectories(dir);
            String original = Objects.requireNonNull(file.getOriginalFilename());
            String filename = UUID.randomUUID() + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            // Ensure path uses forward slash and starts with /
            String path = "/" + dir.toString().replace("\\", "/") + "/" + filename;
            return path;
        } catch (IOException e) {
            log.warn("Gagal simpan file {}: {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Gagal mengunggah file: " + e.getMessage());
        }
    }

    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File gambar tidak boleh kosong!");
        }
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toUpperCase()
                : "";
        boolean validExt = ALLOWED_EXTENSIONS.contains(extension);
        boolean validType = contentType != null && contentType.startsWith("image/");
        if (!validExt && !validType) {
            throw new RuntimeException("Format file tidak valid. Hanya diperbolehkan: PNG, JPG, JPEG, WEBP");
        }
        long maxSizeBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new RuntimeException("Ukuran file melebihi batas maksimum 5MB!");
        }
    }
}