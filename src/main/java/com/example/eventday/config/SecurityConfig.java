package com.example.eventday.config;

import com.example.eventday.dto.ApiResponse;
import com.example.eventday.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        ObjectMapper om = new ObjectMapper();
        
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) -> {
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ApiResponse<Void> body = ApiResponse.unauthorized("Unauthorized: token tidak ada atau tidak valid");
                    res.getWriter().write(om.writeValueAsString(body));
                })
                .accessDeniedHandler((req, res, accessEx) -> {
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    ApiResponse<Void> body = ApiResponse.forbidden("Forbidden: akses ditolak");
                    res.getWriter().write(om.writeValueAsString(body));
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Public root & static resources (termasuk folder upload gambar/dokumen)
                .requestMatchers("/", "/error", "/favicon.ico", "/uploads/**").permitAll()

                // Modul Auth (Mendukung /api/auth/** dan /api/v1/auth/**)
                .requestMatchers("/api/auth/**", "/api/v1/auth/**").permitAll()

                // Modul Home, Search & Legal (Publik)
                .requestMatchers("/api/home/**").permitAll()
                .requestMatchers("/api/search/**").permitAll()
                .requestMatchers("/terms-conditions", "/privacy-policy", "/api/terms-conditions", "/api/privacy-policy").permitAll()

                // Modul Events (Hanya GET yang publik, POST/PUT/DELETE butuh login)
                .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()

                // Webhook Midtrans (Publik)
                .requestMatchers("/api/payments/midtrans-notification", "/api/v1/payments/midtrans-notification").permitAll()

                // Endpoint Scan Tiket (Dibatasi untuk Admin/Organizer saja)
                .requestMatchers("/api/tickets/scan", "/api/v1/tickets/scan").hasAnyRole("ADMIN", "ORGANIZER")

                // Modul Admin
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

                // Sisanya wajib Authenticated
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}