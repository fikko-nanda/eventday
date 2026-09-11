package com.example.eventday.controller;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.dto.EventCardResponse;
import com.example.eventday.dto.HeroBannerResponse;
import com.example.eventday.service.HomeSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HomeSearchController {

    private final HomeSearchService homeSearchService;

    // ===== MODUL HOME =====

    @GetMapping("/api/v1/home/hero-banner")
    public ResponseEntity<ApiResponse<List<HeroBannerResponse>>> getHeroBanner() {
        List<HeroBannerResponse> data = homeSearchService.getHeroBanners();
        if (data == null) {
            data = Collections.emptyList();
        }
        return ResponseEntity.ok(ApiResponse.ok("Berhasil mengambil hero banner", data));
    }

    @GetMapping("/api/v1/home/event-card")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHomeEventCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size, homeSearchService.parseSort("latest"));
        Page<EventCardResponse> result = homeSearchService.getHomeEventCards(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Berhasil mengambil event card", toPageMap(result)));
    }

    @GetMapping("/api/v1/home/locations")
    public ResponseEntity<ApiResponse<List<String>>> getHomeLocations() {
        List<String> data = homeSearchService.getLocations();
        if (data == null) {
            data = Collections.emptyList();
        }
        return ResponseEntity.ok(ApiResponse.ok("Berhasil mengambil daftar lokasi", data));
    }

    // ===== MODUL SEARCH =====

    @GetMapping("/api/v1/search/results")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchResults(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "latest") String sort) {
        Pageable pageable = PageRequest.of(page, size, homeSearchService.parseSort(sort));
        Page<EventCardResponse> result =
                homeSearchService.searchEvents(keyword, category, location, date, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Berhasil mencari event", toPageMap(result)));
    }

    @GetMapping("/api/v1/search/locations")
    public ResponseEntity<ApiResponse<List<String>>> getSearchLocations() {
        List<String> data = homeSearchService.getLocations();
        if (data == null) {
            data = Collections.emptyList();
        }
        return ResponseEntity.ok(ApiResponse.ok("Berhasil mengambil daftar lokasi", data));
    }

    @GetMapping("/api/v1/search/categories")
    public ResponseEntity<ApiResponse<List<String>>> getSearchCategories() {
        List<String> data = homeSearchService.getCategories();
        if (data == null) {
            data = Collections.emptyList();
        }
        return ResponseEntity.ok(ApiResponse.ok("Berhasil mengambil daftar kategori", data));
    }

    private Map<String, Object> toPageMap(Page<EventCardResponse> page) {
        Map<String, Object> map = new HashMap<>();
        if (page == null) {
            map.put("content", Collections.emptyList());
            map.put("page", 0);
            map.put("size", 0);
            map.put("totalElements", 0);
            map.put("totalPages", 0);
            return map;
        }
        map.put("content", page.getContent() != null ? page.getContent() : Collections.emptyList());
        map.put("page", page.getNumber());
        map.put("size", page.getSize());
        map.put("totalElements", page.getTotalElements());
        map.put("totalPages", page.getTotalPages());
        return map;
    }
}
