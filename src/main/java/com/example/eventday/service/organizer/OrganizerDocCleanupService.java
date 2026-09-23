package com.example.eventday.service.organizer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@Service
public class OrganizerDocCleanupService {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    // Default retensi: bersihkan file dokumen sementara yang berusia lebih dari 24 jam
    private static final long RETENTION_HOURS = 24;

    /**
     * Berjalan otomatis setiap hari pada pukul 02:00 dini hari WIB
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduleCleanup() {
        log.info("[SCHEDULED CLEANUP] Memulai pembersihan dokumen dan berkas sementara organizer...");
        int deletedDocs = cleanupDirectory("organizer-docs", RETENTION_HOURS);
        log.info("[SCHEDULED CLEANUP] Selesai. Total {} berkas sampah berhasil dibersihkan.", deletedDocs);
    }

    /**
     * Membersihkan file dalam subfolder tertentu yang usianya melebihi retentionHours
     */
    public int cleanupDirectory(String subfolder, long retentionHours) {
        Path targetDir = Paths.get(uploadDir, subfolder);
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            log.debug("Direktori {} tidak ditemukan, melewati proses cleanup.", targetDir);
            return 0;
        }

        Instant cutoffTime = Instant.now().minus(retentionHours, ChronoUnit.HOURS);
        AtomicInteger deletedCount = new AtomicInteger(0);

        try (Stream<Path> files = Files.walk(targetDir)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    Instant lastModified = attrs.lastModifiedTime().toInstant();

                    if (lastModified.isBefore(cutoffTime)) {
                        Files.deleteIfExists(file);
                        deletedCount.incrementAndGet();
                        log.info("Menghapus berkas kadaluarsa/sampah: {}", file.getFileName());
                    }
                } catch (IOException e) {
                    log.warn("Gagal menghapus berkas {}: {}", file.getFileName(), e.getMessage());
                }
            });
        } catch (IOException e) {
            log.error("Gagal melakukan walk directory pada {}: {}", targetDir, e.getMessage());
        }

        return deletedCount.get();
    }
}