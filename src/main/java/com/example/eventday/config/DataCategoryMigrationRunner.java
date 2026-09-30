package com.example.eventday.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Auto-Data Cleanup Runner — normalisasi nilai legacy di kolom events.category
 * saat aplikasi start, TANPA perlu eksekusi manual via pgAdmin.
 *
 * Latar belakang: kolom category kini dipetakan ke enum Category
 * (@Enumerated STRING). Baris lama berisi string di luar enum
 * (misal 'ENTERTAINMENT', 'Musik') membuat Hibernate melempar
 * IllegalArgumentException saat load entity → endpoint 400.
 * Runner ini menormalkan semua nilai tersebut sebelum traffic masuk.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCategoryMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            int music = jdbcTemplate.update(
                    "UPDATE events SET category = 'MUSIC_FESTIVAL' WHERE category IS NULL"
                            + " OR UPPER(TRIM(category)) IN ('ENTERTAINMENT','MUSIK','MUSIC','KONSER','CONCERT','FESTIVAL','HIBURAN')"
                            + " OR UPPER(TRIM(category)) LIKE '%MUSIK%'"
                            + " OR UPPER(TRIM(category)) LIKE '%MUSIC%'"
                            + " OR UPPER(TRIM(category)) LIKE '%KONSER%'"
                            + " OR UPPER(TRIM(category)) LIKE '%CONCERT%'"
                            + " OR UPPER(TRIM(category)) LIKE '%FESTIVAL%'"
                            + " OR UPPER(TRIM(category)) LIKE '%ENTERTAINMENT%'");
            int conference = jdbcTemplate.update(
                    "UPDATE events SET category = 'CONFERENCE' WHERE UPPER(TRIM(category)) IN ('SEMINAR','SEMINAR_WORKSHOP','WORKSHOP','CONFERENCE','KONFERENSI')"
                            + " OR UPPER(TRIM(category)) LIKE '%SEMINAR%'"
                            + " OR UPPER(TRIM(category)) LIKE '%WORKSHOP%'"
                            + " OR UPPER(TRIM(category)) LIKE '%CONFERENCE%'"
                            + " OR UPPER(TRIM(category)) LIKE '%KONFERENSI%'");
            int exhibition = jdbcTemplate.update(
                    "UPDATE events SET category = 'EXHIBITION' WHERE UPPER(TRIM(category)) IN ('PAMERAN','EXHIBITION','EXPO')"
                            + " OR UPPER(TRIM(category)) LIKE '%PAMERAN%'"
                            + " OR UPPER(TRIM(category)) LIKE '%EXHIBITION%'"
                            + " OR UPPER(TRIM(category)) LIKE '%EXPO%'");
            int culinary = jdbcTemplate.update(
                    "UPDATE events SET category = 'CULINARY' WHERE UPPER(TRIM(category)) IN ('KULINER','CULINARY','FOOD')"
                            + " OR UPPER(TRIM(category)) LIKE '%KULINER%'"
                            + " OR UPPER(TRIM(category)) LIKE '%CULINARY%'");
            // Jaring pengaman terakhir: sisa nilai tak dikenal → OTHER agar
            // tidak ada lagi baris yang gagal di-hydrate ke enum Category.
            int other = jdbcTemplate.update(
                    "UPDATE events SET category = 'OTHER' WHERE UPPER(TRIM(category)) NOT IN"
                            + " ('MUSIC_FESTIVAL','SEMINAR_WORKSHOP','CONFERENCE','EXHIBITION','CULINARY','SPORTS','COMMUNITY','TECHNOLOGY','OTHER')");
            log.info("Normalisasi category events selesai (music={}, conference={}, exhibition={}, culinary={}, other={})",
                    music, conference, exhibition, culinary, other);
        } catch (Exception e) {
            // Abaikan jika tabel belum siap — endpoint tetap aman via safe-parsing
            log.warn("Lewati normalisasi category events: {}", e.getMessage());
        }
    }
}
