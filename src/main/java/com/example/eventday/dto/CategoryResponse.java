package com.example.eventday.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {
    private String value; // Contoh: "MUSIC_FESTIVAL"
    private String label; // Contoh: "Musik & Konser"
}