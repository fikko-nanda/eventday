package com.example.eventday.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowCredentials(true);
        
        // Izinkan semua origin (localhost, IP lokal, ngrok)
        config.setAllowedOriginPatterns(List.of("*"));
        
        // Izinkan semua method HTTP
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Izinkan semua header (termasuk Authorization & ngrok-skip-browser-warning)
        config.setAllowedHeaders(List.of("*"));
        
        // PENTING: Expose Header Authorization agar frontend bisa membaca token JWT
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        
        // Cache preflight OPTIONS selama 1 jam
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}