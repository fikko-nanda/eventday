package com.example.eventday.model;

import lombok.Getter;

@Getter
public enum Category {
    MUSIC_FESTIVAL("Musik & Konser"),
    SEMINAR_WORKSHOP("Seminar & Workshop"),
    CONFERENCE("Konferensi"),
    EXHIBITION("Pameran"),
    CULINARY("Kuliner"),
    SPORTS("Olahraga"),
    COMMUNITY("Komunitas"),
    TECHNOLOGY("Teknologi"),
    OTHER("Lainnya");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    /**
     * Konversi String dari FE/query param → enum Category (toleran).
     * - null/kosong/"Semua"/"ALL" → null (artinya: tanpa filter)
     * - "MUSIC_FESTIVAL" / "Music" / "musik" → MUSIC_FESTIVAL (cocok persis dulu, lalu longgar)
     * - String tak dikenal → OTHER
     */
    public static Category fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        if ("SEMUA".equals(normalized) || "ALL".equals(normalized)) {
            return null;
        }
        try {
            return Category.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            // lanjut ke pencocokan longgar
        }
        if (normalized.contains("MUSI") || normalized.contains("KONSER")
                || normalized.contains("CONCERT") || normalized.contains("FESTIVAL")) {
            return MUSIC_FESTIVAL;
        }
        if (normalized.contains("SEMINAR") || normalized.contains("WORKSHOP")) {
            return SEMINAR_WORKSHOP;
        }
        if (normalized.contains("CONFERENCE") || normalized.contains("KONFERENSI")) {
            return CONFERENCE;
        }
        if (normalized.contains("EXHIBITION") || normalized.contains("PAMERAN") || normalized.contains("EXPO")) {
            return EXHIBITION;
        }
        if (normalized.contains("CULINARY") || normalized.contains("KULINER")
                || normalized.contains("FOOD") || normalized.contains("MAKAN")) {
            return CULINARY;
        }
        if (normalized.contains("SPORT") || normalized.contains("OLAHRAGA")) {
            return SPORTS;
        }
        if (normalized.contains("COMMUNITY") || normalized.contains("KOMUNITAS")) {
            return COMMUNITY;
        }
        if (normalized.contains("TECH") || normalized.contains("TEKNOLOGI")) {
            return TECHNOLOGY;
        }
        return OTHER;
    }

    /**
     * Kode kategori untuk response FE (misal "MUSIC_FESTIVAL"), null-safe.
     */
    public static String codeOf(Category category) {
        return category != null ? category.name() : null;
    }
}