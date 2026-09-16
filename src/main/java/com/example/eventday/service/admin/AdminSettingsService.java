package com.example.eventday.service.admin;

import com.example.eventday.dto.admin.AdminSettingsRequest;
import com.example.eventday.dto.admin.AuditLogExportResponse;
import com.example.eventday.entity.AuditLog;
import com.example.eventday.entity.Settings;
import com.example.eventday.repository.AuditLogRepository;
import com.example.eventday.repository.SettingsRepository;
import com.example.eventday.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSettingsService {

    private final SettingsRepository settingsRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

    @Value("${upload.logo.dir}")
    private String logoDir;

    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<AuditLogExportResponse> exportAuditLogs() {
        List<AuditLog> logs = auditLogRepository.findAllForExport();
        return logs.stream().map(this::mapToExportResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String exportAuditLogsToCsv() {
        List<AuditLog> logs = auditLogRepository.findAllForExport();
        StringBuilder csv = new StringBuilder();
        csv.append("auditId,actorId,actorName,action,detail,createdAt\n");
        for (AuditLog log : logs) {
            csv.append(escapeCsv(log.getAuditId().toString())).append(",");
            csv.append(escapeCsv(log.getActorId().toString())).append(",");
            csv.append(escapeCsv(log.getActorName())).append(",");
            csv.append(escapeCsv(log.getAction())).append(",");
            csv.append(escapeCsv(log.getDetail())).append(",");
            csv.append(escapeCsv(log.getCreatedAt().toString())).append("\n");
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public Map<String, String> getGeneralSettings() {
        List<Settings> all = settingsRepository.findAll();
        Map<String, String> map = new LinkedHashMap<>();
        for (Settings s : all) {
            map.put(s.getSettingsKey(), s.getSettingsValue());
        }
        return map;
    }

    @Transactional
    public void saveGeneralSettings(AdminSettingsRequest req, UUID adminId) {
        saveOrUpdate("APP_NAME", req.getAppName());
        saveOrUpdate("CONTACT_EMAIL", req.getContactEmail());
        if (req.getAdminFee() != null) {
            saveOrUpdate("ADMIN_FEE", String.valueOf(req.getAdminFee()));
        }
        if (req.getOrderExpiryMinutes() != null) {
            saveOrUpdate("ORDER_EXPIRY_MINUTES", String.valueOf(req.getOrderExpiryMinutes()));
        }
        auditLogService.log(adminId, "SUPERADMIN", "UPDATE_SETTINGS", "Memperbarui konfigurasi general sistem");
    }

    @Transactional
    public String uploadLogo(MultipartFile file, UUID adminId) {
        if (file.isEmpty()) {
            throw new RuntimeException("File logo tidak boleh kosong!");
        }
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toUpperCase()
                : "";
        if (!isValidImageType(contentType, extension)) {
            throw new RuntimeException("Format file tidak valid. Hanya diperbolehkan: PNG, JPG, JPEG");
        }
        long maxSizeBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new RuntimeException("Ukuran file melebihi batas maksimum 5MB!");
        }
        try {
            File uploadDir = new File(logoDir);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            String filename = "platform-logo-" + UUID.randomUUID() + "." + extension.toLowerCase();
            File dest = new File(logoDir + File.separator + filename);
            file.transferTo(dest);
            String logoUrl = "/uploads/logos/" + filename;
            saveOrUpdate("PLATFORM_LOGO", logoUrl);
            auditLogService.log(adminId, "SUPERADMIN", "UPLOAD_LOGO",
                    "Logo platform diunggah: " + filename + " (" + file.getSize() + " bytes)");
            return logoUrl;
        } catch (IOException e) {
            throw new RuntimeException("Gagal mengunggah logo: " + e.getMessage());
        }
    }

    private void saveOrUpdate(String key, String value) {
        if (value == null) return;
        Settings s = settingsRepository.findBySettingsKey(key)
                .orElse(Settings.builder().settingsKey(key).build());
        s.setSettingsValue(value);
        settingsRepository.save(s);
    }

    private boolean isValidImageType(String contentType, String extension) {
        String upperExt = extension.toUpperCase();
        return ("PNG".equals(upperExt) || "JPG".equals(upperExt) || "JPEG".equals(upperExt))
                || (contentType != null && contentType.contains("image/"));
    }

    private AuditLogExportResponse mapToExportResponse(AuditLog log) {
        return AuditLogExportResponse.builder()
                .auditId(log.getAuditId())
                .actorId(log.getActorId())
                .actorName(log.getActorName())
                .action(log.getAction())
                .detail(log.getDetail())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
