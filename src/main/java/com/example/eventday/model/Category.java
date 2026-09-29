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
}